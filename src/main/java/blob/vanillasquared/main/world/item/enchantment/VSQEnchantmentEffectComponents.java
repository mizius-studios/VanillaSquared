package blob.vanillasquared.main.world.item.enchantment;

import blob.vanillasquared.main.VanillaSquared;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.enchantment.TargetedConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;

public final class VSQEnchantmentEffectComponents {
    public static final DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> POST_BLOCK = Registry.register(
            BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "post_block"),
            DataComponentType.<List<TargetedConditionalEffect<EnchantmentEntityEffect>>>builder()
                    .persistent(validatedListCodec(TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
                    .build()
    );
    public static final DataComponentType<List<TargetedConditionalEffect<EnchantmentLocationBasedEffect>>> CHANNELING_PATH = Registry.register(
            BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "channeling_path"),
            DataComponentType.<List<TargetedConditionalEffect<EnchantmentLocationBasedEffect>>>builder()
                    .persistent(validatedListCodec(TargetedConditionalEffect.codec(EnchantmentLocationBasedEffect.CODEC), LootContextParamSets.ENCHANTED_DAMAGE))
                    .build()
    );

    private VSQEnchantmentEffectComponents() {
    }

    public static void initialize() {
    }

    private static <T extends Validatable> Codec<List<T>> validatedListCodec(Codec<T> elementCodec, ContextKeySet paramSet) {
        return elementCodec.listOf().validate(Validatable.listValidatorForContext(paramSet));
    }
}
