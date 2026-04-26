package blob.vanillasquared.main.world.effect;

import blob.vanillasquared.main.world.particle.VSQParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

public final class VoidedEffectState {
    private static final String VOIDED_STATE_TAG = "VSQVoidedState";
    private static final String MULTIPLIER_TAG = "Multiplier";
    private static final String INCREMENT_INTERVAL_TAG = "IncrementInterval";
    private static final String TICKS_UNTIL_INCREMENT_TAG = "TicksUntilNextIncrement";
    private static final String MAX_MULTIPLIER_TAG = "MaxMultiplier";
    private static final int ACTIVE_CLOUD_PARTICLE_INTERVAL = 14;
    private static final int ACTIVE_PIXEL_PARTICLE_INTERVAL = 3;
    private static final int INCREMENT_CLOUD_PARTICLE_COUNT = 5;
    private static final int INFINITE_DURATION_INCREMENT_INTERVAL = 100;
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private static final Map<LivingEntity, Boolean> PENDING_REMOVALS = new WeakHashMap<>();
    private static final Map<LivingEntity, Boolean> LOADING_FROM_NBT = new WeakHashMap<>();

    private VoidedEffectState() {
    }

    public static void tick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MobEffectInstance effect = entity.getEffect(VSQMobEffects.VOIDED);
        if (effect == null) {
            clear(entity);
            return;
        }

        State state = STATES.get(entity);
        if (state == null) {
            if (PENDING_REMOVALS.containsKey(entity)) {
                return;
            }
            refresh(entity, effect);
            return;
        }

        if (effect.isVisible()) {
            spawnActiveParticles(serverLevel, entity);
        }

        if (state.multiplier >= state.maxMultiplier) {
            return;
        }

        state.ticksUntilNextIncrement--;
        if (state.ticksUntilNextIncrement > 0) {
            return;
        }

        state.multiplier = Math.min(state.multiplier + 0.1F, state.maxMultiplier);
        state.ticksUntilNextIncrement = state.incrementInterval;
        if (effect.isVisible()) {
            spawnBurst(serverLevel, entity);
        }
    }

    public static void refresh(LivingEntity entity, MobEffectInstance effect) {
        State previousState = STATES.get(entity);
        int initialDuration = effect.getDuration();
        int stepCount = effect.getAmplifier() + 1;
        int incrementInterval = initialDuration < 0
                ? INFINITE_DURATION_INCREMENT_INTERVAL
                : Math.max(initialDuration / stepCount, 1);
        float maxMultiplier = 1.0F + (stepCount * 0.1F);
        float previousMultiplier = previousState == null ? 1.0F : previousState.multiplier;
        float multiplier = Math.min(Math.max(1.1F, previousMultiplier), maxMultiplier);
        int ticksUntilNextIncrement = previousState == null
                ? incrementInterval
                : Math.min(Math.max(1, previousState.ticksUntilNextIncrement), incrementInterval);

        STATES.put(entity, new State(multiplier, incrementInterval, ticksUntilNextIncrement, maxMultiplier));

        if (entity.level() instanceof ServerLevel serverLevel
                && multiplier > previousMultiplier
                && effect.isVisible()
                && LOADING_FROM_NBT.get(entity) == null) {
            spawnBurst(serverLevel, entity);
        }
    }

    public static float consume(LivingEntity entity) {
        State state = STATES.remove(entity);
        if (state == null) {
            return 1.0F;
        }
        return state.multiplier;
    }

    public static void clear(LivingEntity entity) {
        STATES.remove(entity);
        PENDING_REMOVALS.remove(entity);
        LOADING_FROM_NBT.remove(entity);
    }

    public static void clearRemoved(LivingEntity entity, Collection<MobEffectInstance> effects) {
        for (MobEffectInstance effect : effects) {
            if (effect.is(VSQMobEffects.VOIDED)) {
                clear(entity);
                return;
            }
        }
    }

    public static void writeToNbt(LivingEntity entity, ValueOutput output) {
        State state = STATES.get(entity);
        if (state == null) {
            output.discard(VOIDED_STATE_TAG);
            return;
        }

        ValueOutput stateTag = output.child(VOIDED_STATE_TAG);
        stateTag.putFloat(MULTIPLIER_TAG, state.multiplier);
        stateTag.putInt(INCREMENT_INTERVAL_TAG, state.incrementInterval);
        stateTag.putInt(TICKS_UNTIL_INCREMENT_TAG, state.ticksUntilNextIncrement);
        stateTag.putFloat(MAX_MULTIPLIER_TAG, state.maxMultiplier);
    }

    public static void readFromNbt(LivingEntity entity, ValueInput input) {
        ValueInput stateTag = input.child(VOIDED_STATE_TAG).orElse(null);
        if (stateTag == null) {
            return;
        }

        float multiplier = Math.max(stateTag.getFloatOr(MULTIPLIER_TAG, 1.0F), 1.0F);
        int incrementInterval = Math.max(stateTag.getIntOr(INCREMENT_INTERVAL_TAG, 1), 1);
        int ticksUntilNextIncrement = Math.max(stateTag.getIntOr(TICKS_UNTIL_INCREMENT_TAG, 1), 1);
        float maxMultiplier = Math.max(stateTag.getFloatOr(MAX_MULTIPLIER_TAG, Math.max(multiplier, 1.1F)), multiplier);

        STATES.put(entity, new State(multiplier, incrementInterval, ticksUntilNextIncrement, maxMultiplier));
    }

    public static void beginLoadFromNbt(LivingEntity entity) {
        LOADING_FROM_NBT.put(entity, true);
    }

    public static void endLoadFromNbt(LivingEntity entity) {
        LOADING_FROM_NBT.remove(entity);
    }

    public static void scheduleRemoveEffect(LivingEntity entity) {
        PENDING_REMOVALS.put(entity, true);
    }

    public static void flushPendingRemoval(LivingEntity entity) {
        if (PENDING_REMOVALS.remove(entity) != null) {
            entity.removeEffect(VSQMobEffects.VOIDED);
        }
    }

    private static void spawnBurst(ServerLevel level, LivingEntity entity) {
        level.sendParticles(
                VSQParticleTypes.VOID_CLOUD,
                entity.getX(),
                entity.getY(0.18),
                entity.getZ(),
                INCREMENT_CLOUD_PARTICLE_COUNT,
                entity.getBbWidth() * 0.24,
                entity.getBbHeight() * 0.14,
                entity.getBbWidth() * 0.24,
                0.01
        );
    }

    private static void spawnActiveParticles(ServerLevel level, LivingEntity entity) {
        if (entity.tickCount % ACTIVE_CLOUD_PARTICLE_INTERVAL == 0) {
            level.sendParticles(
                    VSQParticleTypes.VOID_CLOUD,
                    entity.getX(),
                    entity.getY(0.08),
                    entity.getZ(),
                    1,
                    entity.getBbWidth() * 0.14,
                    entity.getBbHeight() * 0.12,
                    entity.getBbWidth() * 0.14,
                    0.006
            );
        }

        if (entity.tickCount % ACTIVE_PIXEL_PARTICLE_INTERVAL == 0) {
            level.sendParticles(
                    VSQParticleTypes.VOID_PIXEL,
                    entity.getX(),
                    entity.getY(0.5),
                    entity.getZ(),
                    1,
                    entity.getBbWidth() * 0.55,
                    entity.getBbHeight() * 0.4,
                    entity.getBbWidth() * 0.55,
                    0.015
            );
        }
    }

    private static final class State {
        private float multiplier;
        private final int incrementInterval;
        private int ticksUntilNextIncrement;
        private final float maxMultiplier;

        private State(float multiplier, int incrementInterval, int ticksUntilNextIncrement, float maxMultiplier) {
            this.multiplier = multiplier;
            this.incrementInterval = incrementInterval;
            this.ticksUntilNextIncrement = ticksUntilNextIncrement;
            this.maxMultiplier = maxMultiplier;
        }
    }
}
