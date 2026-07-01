package blob.vanillasquared.main.world.loot;

import blob.vanillasquared.main.VanillaSquared;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

public final class VSQLootFunctions {
    public static final MapCodec<? extends LootItemFunction> RANDOMIZE_ENCHANTMENT_SLOTS = Registry.register(
            BuiltInRegistries.LOOT_FUNCTION_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "randomize_enchantment_slots"),
            RandomizeEnchantmentSlotsFunction.MAP_CODEC
    );

    private VSQLootFunctions() {
    }

    public static void initialize() {
    }
}