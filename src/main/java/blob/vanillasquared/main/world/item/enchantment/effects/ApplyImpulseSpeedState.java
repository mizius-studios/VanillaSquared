package blob.vanillasquared.main.world.item.enchantment.effects;

import net.minecraft.world.item.enchantment.effects.ApplyEntityImpulse;

import java.util.Map;
import java.util.WeakHashMap;

public final class ApplyImpulseSpeedState {
    private static final Map<ApplyEntityImpulse, Double> SPEEDS = new WeakHashMap<>();

    private ApplyImpulseSpeedState() {
    }

    public static ApplyEntityImpulse remember(ApplyEntityImpulse effect, double speed) {
        if (speed == 1.0D) {
            return effect;
        }

        synchronized (SPEEDS) {
            SPEEDS.put(effect, speed);
        }
        return effect;
    }

    public static double speed(ApplyEntityImpulse effect) {
        synchronized (SPEEDS) {
            return SPEEDS.getOrDefault(effect, 1.0D);
        }
    }

    public static boolean hasCustomSpeed(ApplyEntityImpulse effect) {
        synchronized (SPEEDS) {
            return SPEEDS.containsKey(effect);
        }
    }
}
