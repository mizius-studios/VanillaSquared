package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record EnchantingRecipeSelectionPayload(int containerId, int displayId) implements CustomPacketPayload {
    public static final Type<EnchantingRecipeSelectionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_recipe_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipeSelectionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantingRecipeSelectionPayload::containerId,
            ByteBufCodecs.VAR_INT, EnchantingRecipeSelectionPayload::displayId,
            EnchantingRecipeSelectionPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
