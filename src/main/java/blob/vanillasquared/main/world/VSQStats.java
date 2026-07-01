package blob.vanillasquared.main.world;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

public final class VSQStats {
    public static final Identifier SPECIAL_ENCHANTMENTS_USED_ID = register("special_enchantments_used");
    public static final Stat<Identifier> SPECIAL_ENCHANTMENTS_USED = Stats.CUSTOM.get(
            SPECIAL_ENCHANTMENTS_USED_ID,
            StatFormatter.DEFAULT
    );

    private VSQStats() {
    }

    private static Identifier register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, name);
        return Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
    }

    public static void initialize() {
    }
}
