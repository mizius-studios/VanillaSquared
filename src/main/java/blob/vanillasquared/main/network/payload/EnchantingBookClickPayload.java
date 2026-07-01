package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record EnchantingBookClickPayload(int containerId) implements CustomPacketPayload {
    public static final Type<EnchantingBookClickPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_book_click"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingBookClickPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EnchantingBookClickPayload::containerId,
            EnchantingBookClickPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
