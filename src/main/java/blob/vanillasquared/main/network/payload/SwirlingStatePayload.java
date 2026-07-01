package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record SwirlingStatePayload(int entityId, boolean active, int remainingTicks, int totalTicks, int warmupTicks, boolean paused) implements CustomPacketPayload {
    public static final Type<SwirlingStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "swirling_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwirlingStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.mapStream(buf -> buf),
            SwirlingStatePayload::entityId,
            ByteBufCodecs.BOOL.mapStream(buf -> buf),
            SwirlingStatePayload::active,
            ByteBufCodecs.VAR_INT.mapStream(buf -> buf),
            SwirlingStatePayload::remainingTicks,
            ByteBufCodecs.VAR_INT.mapStream(buf -> buf),
            SwirlingStatePayload::totalTicks,
            ByteBufCodecs.VAR_INT.mapStream(buf -> buf),
            SwirlingStatePayload::warmupTicks,
            ByteBufCodecs.BOOL.mapStream(buf -> buf),
            SwirlingStatePayload::paused,
            SwirlingStatePayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
