package blob.vanillasquared.main.sound;

import blob.vanillasquared.main.world.effect.VSQMobEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
public final class VoidedSoundController {
    private static final int INFINITE_DURATION_INCREMENT_INTERVAL = 100;
    private static final int PASSIVE_FADE_TICKS = 20;
    private static final float PASSIVE_START_VOLUME = 0.03F;
    private static final float PASSIVE_VOLUME = 0.55F;
    private static final float INITIAL_MULTIPLIER = 1.1F;
    private static final float MULTIPLIER_STEP = 0.1F;
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    private VoidedSoundController() {
    }

    public static void tickEntity(LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        MobEffectInstance effect = entity.getEffect(VSQMobEffects.VOIDED);
        if (effect == null) {
            remove(entity);
            return;
        }

        State state = STATES.get(entity);
        if (state == null) {
            state = State.create(effect);
            STATES.put(entity, state);
        } else if (state.shouldRefresh(effect)) {
            state.refresh(effect, entity);
        } else if (state.multiplier < state.maxMultiplier) {
            state.ticksUntilNextIncrement--;
            if (state.ticksUntilNextIncrement <= 0) {
                state.multiplier = Math.min(state.multiplier + MULTIPLIER_STEP, state.maxMultiplier);
                state.ticksUntilNextIncrement = state.incrementInterval;
                playMultiplierIncrease(entity);
            }
        }

        state.ensurePassive(entity, minecraft.getSoundManager());
        state.lastAmplifier = effect.getAmplifier();
        state.lastDuration = effect.getDuration();
    }

    public static void clear() {
        Iterator<State> iterator = STATES.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().stopPassive();
            iterator.remove();
        }
    }

    private static void remove(LivingEntity entity) {
        State state = STATES.remove(entity);
        if (state != null) {
            state.stopPassive();
        }
    }

    private static void playMultiplierIncrease(LivingEntity entity) {
        if (entity.isSilent()) {
            return;
        }

        RandomSource random = entity.getRandom();
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                VSQSoundEvents.VOIDED_MULTIPLIER_INCREASE.value(),
                SoundSource.NEUTRAL,
                1.0F,
                1.0F,
                random,
                entity.getX(),
                entity.getY(),
                entity.getZ()
        ));
    }

    private static final class State {
        private float multiplier;
        private int incrementInterval;
        private int ticksUntilNextIncrement;
        private float maxMultiplier;
        private int lastAmplifier;
        private int lastDuration;
        private VoidedPassiveSoundInstance passiveSound;

        private State(float multiplier, int incrementInterval, int ticksUntilNextIncrement, float maxMultiplier, int lastAmplifier, int lastDuration) {
            this.multiplier = multiplier;
            this.incrementInterval = incrementInterval;
            this.ticksUntilNextIncrement = ticksUntilNextIncrement;
            this.maxMultiplier = maxMultiplier;
            this.lastAmplifier = lastAmplifier;
            this.lastDuration = lastDuration;
        }

        private static State create(MobEffectInstance effect) {
            int stepCount = effect.getAmplifier() + 1;
            int incrementInterval = resolveIncrementInterval(effect.getDuration(), stepCount);
            float maxMultiplier = 1.0F + (stepCount * MULTIPLIER_STEP);
            return new State(
                    Math.min(INITIAL_MULTIPLIER, maxMultiplier),
                    incrementInterval,
                    incrementInterval,
                    maxMultiplier,
                    effect.getAmplifier(),
                    effect.getDuration()
            );
        }

        private boolean shouldRefresh(MobEffectInstance effect) {
            return effect.getAmplifier() != this.lastAmplifier
                    || effect.getDuration() > this.lastDuration
                    || (effect.getDuration() < 0) != (this.lastDuration < 0);
        }

        private void refresh(MobEffectInstance effect, LivingEntity entity) {
            int stepCount = effect.getAmplifier() + 1;
            int nextIncrementInterval = resolveIncrementInterval(effect.getDuration(), stepCount);
            float nextMaxMultiplier = 1.0F + (stepCount * MULTIPLIER_STEP);
            float previousMultiplier = this.multiplier;

            this.incrementInterval = nextIncrementInterval;
            this.maxMultiplier = nextMaxMultiplier;
            this.multiplier = Math.min(Math.max(INITIAL_MULTIPLIER, previousMultiplier), nextMaxMultiplier);
            this.ticksUntilNextIncrement = Math.min(Math.max(1, this.ticksUntilNextIncrement), nextIncrementInterval);

            if (this.multiplier > previousMultiplier) {
                playMultiplierIncrease(entity);
            }
        }

        private void ensurePassive(LivingEntity entity, SoundManager soundManager) {
            if (entity.isSilent()) {
                stopPassive();
                return;
            }

            if (this.passiveSound != null && soundManager.isActive(this.passiveSound) && !this.passiveSound.isStopping()) {
                return;
            }

            this.passiveSound = new VoidedPassiveSoundInstance(entity);
            soundManager.play(this.passiveSound);
        }

        private void stopPassive() {
            if (this.passiveSound != null) {
                this.passiveSound.requestStop();
                this.passiveSound = null;
            }
        }

        private static int resolveIncrementInterval(int duration, int stepCount) {
            return duration < 0
                    ? INFINITE_DURATION_INCREMENT_INTERVAL
                    : Math.max(duration / stepCount, 1);
        }
    }

    private static final class VoidedPassiveSoundInstance extends AbstractTickableSoundInstance {
        private final LivingEntity entity;
        private int fadeTicks;
        private boolean stopping;

        private VoidedPassiveSoundInstance(LivingEntity entity) {
            super(VSQSoundEvents.VOIDED_PASSIVE.value(), SoundSource.NEUTRAL, entity.getRandom());
            this.entity = entity;
            this.looping = true;
            this.delay = 0;
            this.volume = PASSIVE_START_VOLUME;
            this.pitch = 1.0F;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
            this.attenuation = Attenuation.LINEAR;
        }

        @Override
        public boolean canPlaySound() {
            return !this.entity.isSilent();
        }

        @Override
        public void tick() {
            if (this.entity.isRemoved() || !this.entity.isAlive() || this.entity.isSilent()) {
                requestStop();
            }

            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();

            if (this.stopping) {
                this.fadeTicks--;
                this.volume = PASSIVE_VOLUME * Math.max(this.fadeTicks, 0) / PASSIVE_FADE_TICKS;
                if (this.fadeTicks <= 0) {
                    this.stop();
                }
                return;
            }

            if (this.fadeTicks < PASSIVE_FADE_TICKS) {
                this.fadeTicks++;
            }
            this.volume = Math.max(PASSIVE_START_VOLUME, PASSIVE_VOLUME * this.fadeTicks / PASSIVE_FADE_TICKS);
        }

        private void requestStop() {
            if (!this.stopping) {
                this.stopping = true;
                this.fadeTicks = Math.min(this.fadeTicks, PASSIVE_FADE_TICKS);
            }
        }

        private boolean isStopping() {
            return this.stopping;
        }
    }
}
