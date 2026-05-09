package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record VoidedSoundPayload(
        int entityId,
        boolean active,
        boolean playIncrease,
        boolean playConsume
) implements CustomPacketPayload {
    public static final Type<VoidedSoundPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "voided_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidedSoundPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.mapStream(buf -> buf),
            VoidedSoundPayload::entityId,
            ByteBufCodecs.BOOL.mapStream(buf -> buf),
            VoidedSoundPayload::active,
            ByteBufCodecs.BOOL.mapStream(buf -> buf),
            VoidedSoundPayload::playIncrease,
            ByteBufCodecs.BOOL.mapStream(buf -> buf),
            VoidedSoundPayload::playConsume,
            VoidedSoundPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
