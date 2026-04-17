package blob.vanillasquared.main.network;

import blob.vanillasquared.main.network.payload.EnchantingBookClickPayload;
import blob.vanillasquared.main.network.payload.EnchantingRecipeBookSyncPayload;
import blob.vanillasquared.main.network.payload.EnchantingRecipeSelectionPayload;
import blob.vanillasquared.main.network.payload.EnchantingRecipeStatePayload;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class VSQNetworking {
    private VSQNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(EnchantingBookClickPayload.TYPE, EnchantingBookClickPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EnchantingRecipeSelectionPayload.TYPE, EnchantingRecipeSelectionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnchantingRecipeStatePayload.TYPE, EnchantingRecipeStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnchantingRecipeBookSyncPayload.TYPE, EnchantingRecipeBookSyncPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EnchantingBookClickPayload.TYPE, (payload, context) ->
                context.server().execute(() -> vsq$handleEnchantingBookClick(payload, context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(EnchantingRecipeSelectionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> vsq$handleEnchantingRecipeSelection(payload, context.player()))
        );
    }

    private static void vsq$handleEnchantingBookClick(EnchantingBookClickPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof VSQEnchantmentMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            return;
        }
        menu.vsq$tryCraftEnchantingRecipe(player);
    }

    private static void vsq$handleEnchantingRecipeSelection(EnchantingRecipeSelectionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof VSQEnchantmentMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            return;
        }
        menu.vsq$setSelectedDisplayId(payload.displayId());
    }
}
