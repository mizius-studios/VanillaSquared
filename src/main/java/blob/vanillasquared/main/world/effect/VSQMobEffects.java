package blob.vanillasquared.main.world.effect;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public final class VSQMobEffects {
    public static final Holder<MobEffect> LUNGING = register("lunging", new LungingMobEffect());
    public static final Holder<MobEffect> VOIDED = register("voided", new VoidedMobEffect());

    private VSQMobEffects() {
    }

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, name),
                effect
        );
    }

    public static void initialize() {
    }
}
