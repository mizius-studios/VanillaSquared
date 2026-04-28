package blob.vanillasquared.main.sound;

import blob.vanillasquared.main.world.effect.VSQMobEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public final class VoidedSoundController {
    private static final int INFINITE_DURATION_INCREMENT_INTERVAL = 100;
    private static final int PASSIVE_FADE_TICKS = 20;
    private static final float PASSIVE_START_VOLUME = 0.03F;
    private static final float PASSIVE_VOLUME = 0.55F;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private VoidedSoundController() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void apply(int entityId, boolean active, boolean playIncrease) {
        State state = STATES.computeIfAbsent(entityId, State::new);
        state.active = active;
        state.pendingMultiplierIncrease |= playIncrease;
    }

    public static void tickEntity(LivingEntity entity) {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }
        SoundManager soundManager = minecraft.getSoundManager();
        Iterator<Map.Entry<Integer, State>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, State> entry = iterator.next();
            LivingEntity entity = minecraft.level.getEntity(entry.getKey()) instanceof LivingEntity living ? living : null;
            State state = entry.getValue();

            if (!state.active || entity == null || entity.isRemoved()) {
                if (state.fadeOut(soundManager)) {
                    iterator.remove();
                }
                continue;
            }

            if (state.pendingMultiplierIncrease) {
                playMultiplierIncrease(entity, soundManager);
                state.pendingMultiplierIncrease = false;
            }

            state.ensurePassive(entity, soundManager);
            state.fadeIn();
        }
    }

    public static void clear() {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        Iterator<State> iterator = STATES.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().stopPassive(soundManager);
            iterator.remove();
        }
    }

    private static void playMultiplierIncrease(LivingEntity entity, SoundManager soundManager) {
        if (entity.isSilent()) {
            return;
        }
        RandomSource random = entity.getRandom();
        soundManager.play(new SimpleSoundInstance(
                VSQSoundEvents.VOIDED_MULTIPLIER_INCREASE.value(),
                entity.getSoundSource(),
                1.0F,
                1.0F,
                random,
                entity.getX(),
                entity.getY(),
                entity.getZ()
        ));
    }

    private static final class State {
        private final int entityId;
        private boolean active;
        private boolean pendingMultiplierIncrease;
        private int passiveFadeTicks;
        private VoidedPassiveSoundInstance passiveSound;

        private State(int entityId) {
            this.entityId = entityId;
        }

        private void ensurePassive(LivingEntity entity, SoundManager soundManager) {
            if (entity.isSilent()) {
                stopPassive(soundManager);
                return;
            }

            if (this.passiveSound != null && soundManager.isActive(this.passiveSound)) {
                return;
            }

            this.passiveSound = new VoidedPassiveSoundInstance(entity);
            this.passiveFadeTicks = 0;
            soundManager.play(this.passiveSound);
        }

        private void fadeIn() {
            if (this.passiveSound == null) {
                return;
            }

            if (this.passiveFadeTicks < PASSIVE_FADE_TICKS) {
                this.passiveFadeTicks++;
            }
            this.passiveSound.setVolume(Math.max(PASSIVE_START_VOLUME, PASSIVE_VOLUME * this.passiveFadeTicks / PASSIVE_FADE_TICKS));
        }

        private boolean fadeOut(SoundManager soundManager) {
            if (this.passiveSound == null) {
                return true;
            }

            this.passiveFadeTicks--;
            if (this.passiveFadeTicks <= 0) {
                stopPassive(soundManager);
                return true;
            }

            this.passiveSound.setVolume(Math.max(PASSIVE_START_VOLUME, PASSIVE_VOLUME * this.passiveFadeTicks / PASSIVE_FADE_TICKS));
            return false;
        }

        private void stopPassive(SoundManager soundManager) {
            if (this.passiveSound != null) {
                soundManager.stop(this.passiveSound);
                this.passiveSound = null;
            }
            this.passiveFadeTicks = 0;
        }
    }

    private static final class VoidedPassiveSoundInstance extends EntityBoundSoundInstance {
        private VoidedPassiveSoundInstance(LivingEntity entity) {
            super(VSQSoundEvents.VOIDED_PASSIVE.value(), entity.getSoundSource(), PASSIVE_START_VOLUME, 1.0F, entity, entity.getRandom().nextLong());
            this.looping = true;
            this.delay = 0;
            this.volume = PASSIVE_START_VOLUME;
        }

        private void setVolume(float volume) {
            this.volume = volume;
        }
    }
}
