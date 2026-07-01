package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record SpecialEnchantmentHotkeyPayload() implements CustomPacketPayload {
    public static final SpecialEnchantmentHotkeyPayload INSTANCE = new SpecialEnchantmentHotkeyPayload();
    public static final Type<SpecialEnchantmentHotkeyPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "special_enchantment_hotkey"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpecialEnchantmentHotkeyPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
