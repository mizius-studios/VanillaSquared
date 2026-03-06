package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record DebugPayload() implements CustomPacketPayload {
    public static final Type<DebugPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "debug_log"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugPayload> CODEC = StreamCodec.unit(new DebugPayload());

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}