package blob.vanillasquared.util.api.enchantment;

import blob.vanillasquared.main.world.item.enchantment.effects.VSQChannelingEffect;
import blob.vanillasquared.main.world.item.enchantment.effects.VSQBeginLungingEffect;
import blob.vanillasquared.main.world.item.enchantment.effects.VSQSendChatMessageEffect;
import blob.vanillasquared.util.api.modules.components.ComponentRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
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

public final class VSQEnchantmentEffects {
    public static final DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> POST_BLOCK = registerValidatedEffectComponent(
            Identifier.fromNamespaceAndPath("vsq", "post_block"),
            TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC),
            LootContextParamSets.ENCHANTED_DAMAGE
    );
    public static final DataComponentType<List<TargetedConditionalEffect<EnchantmentLocationBasedEffect>>> CHANNELING_PATH = registerValidatedEffectComponent(
            Identifier.fromNamespaceAndPath("vsq", "channeling_path"),
            TargetedConditionalEffect.codec(EnchantmentLocationBasedEffect.CODEC),
            LootContextParamSets.ENCHANTED_DAMAGE
    );
    public static final DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> IN_LUNGING = registerValidatedEffectComponent(
            Identifier.fromNamespaceAndPath("vsq", "in_lunging"),
            TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC),
            LootContextParamSets.ENCHANTED_DAMAGE
    );

    public static final MapCodec<? extends EnchantmentEntityEffect> CHANNELING = registerEntityEffect(
            Identifier.fromNamespaceAndPath("vsq", "channeling"),
            VSQChannelingEffect.MAP_CODEC
    );
    public static final MapCodec<? extends EnchantmentEntityEffect> BEGIN_LUNGING = registerEntityEffect(
            Identifier.fromNamespaceAndPath("vsq", "begin_lunging"),
            VSQBeginLungingEffect.MAP_CODEC
    );
    public static final MapCodec<? extends EnchantmentEntityEffect> SEND_CHAT_MESSAGE = registerEntityEffect(
            Identifier.fromNamespaceAndPath("vsq", "send_chat_msg"),
            VSQSendChatMessageEffect.MAP_CODEC
    );

    private VSQEnchantmentEffects() {
    }

    public static void initialize() {
    }

    public static <T> DataComponentType<T> registerEffectComponent(Identifier id, Codec<T> codec) {
        return ComponentRegistry.registerPersistent(id, codec, BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE);
    }

    public static <T extends Validatable> DataComponentType<List<T>> registerValidatedEffectComponent(
            Identifier id,
            Codec<T> elementCodec,
            ContextKeySet paramSet
    ) {
        return registerEffectComponent(id, validatedListCodec(elementCodec, paramSet));
    }

    public static MapCodec<? extends EnchantmentEntityEffect> registerEntityEffect(
            Identifier id,
            MapCodec<? extends EnchantmentEntityEffect> codec
    ) {
        return Registry.register(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE, id, codec);
    }

    public static Identifier componentId(DataComponentType<?> componentType) {
        return BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.getKey(componentType);
    }

    private static <T extends Validatable> Codec<List<T>> validatedListCodec(Codec<T> elementCodec, ContextKeySet paramSet) {
        return elementCodec.listOf().validate(Validatable.listValidatorForContext(paramSet));
    }
}
