package blob.vanillasquared.main.world.item.enchantment.effects;

import blob.vanillasquared.main.VanillaSquared;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;

public final class VSQEnchantmentEntityEffects {
    public static final MapCodec<? extends EnchantmentEntityEffect> CHANNELING = Registry.register(
            BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "channeling"),
            VSQChannelingEffect.MAP_CODEC
    );
    public static final MapCodec<? extends EnchantmentEntityEffect> SEND_CHAT_MSG = Registry.register(
            BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "send_chat_msg"),
            VSQSendChatMessageEffect.MAP_CODEC
    );

    private VSQEnchantmentEntityEffects() {
    }

    public static void initialize() {
    }
}
