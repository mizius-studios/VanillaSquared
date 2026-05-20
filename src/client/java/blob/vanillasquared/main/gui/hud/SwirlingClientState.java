package blob.vanillasquared.main.gui.hud;

import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class SwirlingClientState {
    private static final Map<Integer, Animation> ANIMATIONS = new HashMap<>();
    private static final float RETURN_TICKS = 6.0F;

    private SwirlingClientState() {
    }

    public static void setActive(int entityId, boolean active, int remainingTicks, int totalTicks, int warmupTicks, boolean paused, float ageInTicks) {
        if (!active || remainingTicks <= 0 || totalTicks <= 0) {
            Animation previous = ANIMATIONS.get(entityId);
            if (previous != null) {
                float visualWeight = previous.visualWeight(ageInTicks);
                ANIMATIONS.put(entityId, new Animation(previous.startAge, previous.totalTicks, previous.warmupTicks, true, previous.effectiveAge(ageInTicks), visualWeight, 0.0F, ageInTicks, true));
                return;
            }
            ANIMATIONS.remove(entityId);
            return;
        }
        Animation previous = ANIMATIONS.get(entityId);
        int elapsedTicks = Math.max(0, totalTicks - remainingTicks);
        float startAge = ageInTicks - elapsedTicks;
        int resolvedWarmupTicks = Math.max(0, warmupTicks);
        if (previous == null) {
            ANIMATIONS.put(entityId, new Animation(startAge, totalTicks, resolvedWarmupTicks, paused, ageInTicks, paused ? 0.0F : 1.0F, paused ? 0.0F : 1.0F, ageInTicks, false));
            return;
        }
        if (previous.paused == paused) {
            ANIMATIONS.put(entityId, new Animation(startAge, totalTicks, resolvedWarmupTicks, paused, ageInTicks, previous.visualStartWeight, previous.visualTargetWeight, previous.visualStartAge, false));
            return;
        }
        float visualWeight = previous.visualWeight(ageInTicks);
        ANIMATIONS.put(entityId, new Animation(startAge, totalTicks, resolvedWarmupTicks, paused, ageInTicks, visualWeight, paused ? 0.0F : 1.0F, ageInTicks, false));
    }

    public static float progress(int entityId, float ageInTicks) {
        Animation animation = ANIMATIONS.get(entityId);
        if (animation == null) {
            return -1.0F;
        }
        float effectiveAge = animation.effectiveAge(ageInTicks);
        float progress = (effectiveAge - animation.startAge) / Math.max(1.0F, animation.totalTicks);
        if (progress >= 1.0F) {
            if (!animation.finishing) {
                ANIMATIONS.remove(entityId);
                return -1.0F;
            }
            return 1.0F;
        }
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public static float warmupProgress(int entityId, float ageInTicks) {
        Animation animation = ANIMATIONS.get(entityId);
        if (animation == null || animation.warmupTicks <= 0) {
            return 1.0F;
        }
        float effectiveAge = animation.effectiveAge(ageInTicks);
        return Mth.clamp((effectiveAge - animation.startAge) / animation.warmupTicks, 0.0F, 1.0F);
    }

    public static float spinProgress(int entityId, float ageInTicks) {
        Animation animation = ANIMATIONS.get(entityId);
        if (animation == null) {
            return -1.0F;
        }
        float activeTicks = Math.max(1.0F, animation.totalTicks - animation.warmupTicks);
        float effectiveAge = animation.effectiveAge(ageInTicks);
        return Mth.clamp((effectiveAge - animation.startAge - animation.warmupTicks) / activeTicks, 0.0F, 1.0F);
    }

    public static float visualWeight(int entityId, float ageInTicks) {
        Animation animation = ANIMATIONS.get(entityId);
        return animation == null ? 0.0F : animation.visualWeight(ageInTicks);
    }

    public static void tick(float ageInTicks) {
        Iterator<Map.Entry<Integer, Animation>> iterator = ANIMATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next().getValue();
            if (animation.finishing && animation.visualWeight(ageInTicks) <= 1.0E-3F) {
                iterator.remove();
                continue;
            }
            if (!animation.paused && ageInTicks - animation.startAge >= animation.totalTicks + 2.0F) {
                iterator.remove();
            }
        }
    }

    public static void clear() {
        ANIMATIONS.clear();
    }

    private record Animation(
            float startAge,
            int totalTicks,
            int warmupTicks,
            boolean paused,
            float pausedAge,
            float visualStartWeight,
            float visualTargetWeight,
            float visualStartAge,
            boolean finishing
    ) {
        private float effectiveAge(float ageInTicks) {
            return this.paused ? this.pausedAge : ageInTicks;
        }

        private float visualWeight(float ageInTicks) {
            float progress = Mth.clamp((ageInTicks - this.visualStartAge) / RETURN_TICKS, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
            return Mth.lerp(eased, this.visualStartWeight, this.visualTargetWeight);
        }
    }
}
