package blob.vanillasquared.main;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentScreen;
import blob.vanillasquared.main.network.handlers.EnchantingRecipeBookSyncPayloadHandler;
import blob.vanillasquared.main.network.handlers.EnchantingRecipeStatePayloadHandler;
import blob.vanillasquared.main.world.inventory.VSQMenuTypes;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingIngredient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.screens.MenuScreens;

public class VanillaSquaredClient implements ClientModInitializer {
    public static final String MOD_ID = "vsq";

    @Override
    public void onInitializeClient() {
        EnchantingRecipeStatePayloadHandler.register();
        EnchantingRecipeBookSyncPayloadHandler.register();
        MenuScreens.register(VSQMenuTypes.ENCHANTING, VSQEnchantmentScreen::new);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                EnchantingRecipeStatePayloadHandler.clearAll();
                EnchantingIngredient.clearTagCache();
        });
    }
}
