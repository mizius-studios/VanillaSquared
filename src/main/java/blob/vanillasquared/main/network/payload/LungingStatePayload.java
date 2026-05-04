package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record LungingStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<LungingStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "lunging_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LungingStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.mapStream(buf -> buf),
            LungingStatePayload::active,
            LungingStatePayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
