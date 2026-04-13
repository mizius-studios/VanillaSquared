package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record EnchantingRecipeStatePayload(
        int containerId,
        List<Identifier> blockIds,
        List<Integer> blockCounts,
        List<Integer> requiredBlockCounts,
        int playerLevel,
        int levelRequirement,
        int blockRequirement,
        Component recipeName,
        Component recipeDescription,
        boolean selectionCleared
) implements CustomPacketPayload {
    public static final Type<EnchantingRecipeStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_recipe_state"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Identifier>> IDENTIFIER_LIST_CODEC = Identifier.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Integer>> VAR_INT_LIST_CODEC = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipeStatePayload> CODEC = new StreamCodec<>() {
        @Override
        public EnchantingRecipeStatePayload decode(RegistryFriendlyByteBuf buf) {
            int containerId = ByteBufCodecs.VAR_INT.decode(buf);
            List<Identifier> blockIds = IDENTIFIER_LIST_CODEC.decode(buf);
            List<Integer> blockCounts = VAR_INT_LIST_CODEC.decode(buf);
            List<Integer> requiredBlockCounts = VAR_INT_LIST_CODEC.decode(buf);
            int playerLevel = ByteBufCodecs.VAR_INT.decode(buf);
            int levelRequirement = ByteBufCodecs.VAR_INT.decode(buf);
            int blockRequirement = ByteBufCodecs.VAR_INT.decode(buf);
            Component recipeName = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
            Component recipeDescription = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
            boolean selectionCleared = ByteBufCodecs.BOOL.decode(buf);
            return new EnchantingRecipeStatePayload(containerId, blockIds, blockCounts, requiredBlockCounts, playerLevel, levelRequirement, blockRequirement, recipeName, recipeDescription, selectionCleared);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantingRecipeStatePayload value) {
            ByteBufCodecs.VAR_INT.encode(buf, value.containerId);
            IDENTIFIER_LIST_CODEC.encode(buf, value.blockIds);
            VAR_INT_LIST_CODEC.encode(buf, value.blockCounts);
            VAR_INT_LIST_CODEC.encode(buf, value.requiredBlockCounts);
            ByteBufCodecs.VAR_INT.encode(buf, value.playerLevel);
            ByteBufCodecs.VAR_INT.encode(buf, value.levelRequirement);
            ByteBufCodecs.VAR_INT.encode(buf, value.blockRequirement);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, value.recipeName);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, value.recipeDescription);
            ByteBufCodecs.BOOL.encode(buf, value.selectionCleared);
        }
    };

    public EnchantingRecipeStatePayload {
        blockIds = List.copyOf(blockIds);
        blockCounts = List.copyOf(blockCounts);
        requiredBlockCounts = List.copyOf(requiredBlockCounts);
        recipeName = recipeName.copy();
        recipeDescription = recipeDescription.copy();

        if (blockIds.size() != blockCounts.size()) {
            throw new IllegalArgumentException("blockIds and blockCounts must have the same size");
        }
        if (blockIds.size() != requiredBlockCounts.size()) {
            throw new IllegalArgumentException("blockIds and requiredBlockCounts must have the same size");
        }
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
