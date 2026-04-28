package blob.vanillasquared.main;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentScreen;
import blob.vanillasquared.main.gui.hud.SpecialEnchantmentCooldownClientState;
import blob.vanillasquared.main.gui.controls.VSQKeyMappings;
import blob.vanillasquared.main.network.handlers.EnchantingRecipeBookSyncPayloadHandler;
import blob.vanillasquared.main.network.handlers.EnchantingRecipeStatePayloadHandler;
import blob.vanillasquared.main.network.handlers.SpecialEnchantmentCooldownPayloadHandler;
import blob.vanillasquared.main.sound.VoidedSoundController;
import blob.vanillasquared.main.world.inventory.VSQMenuTypes;
import blob.vanillasquared.main.world.particle.particles.LightningBoltParticle;
import blob.vanillasquared.main.world.particle.particles.VoidedCloudParticle;
import blob.vanillasquared.main.world.particle.particles.VoidedPixelParticle;
import blob.vanillasquared.main.world.particle.VSQParticleTypes;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingIngredient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.screens.MenuScreens;

public class VanillaSquaredClient implements ClientModInitializer {
    public static final String MOD_ID = "vsq";

    @Override
    public void onInitializeClient() {
        EnchantingRecipeStatePayloadHandler.register();
        EnchantingRecipeBookSyncPayloadHandler.register();
        SpecialEnchantmentCooldownPayloadHandler.register();
        SpecialEnchantmentCooldownClientState.initialize();
        VSQKeyMappings.initialize();
        ParticleProviderRegistry.getInstance().register(VSQParticleTypes.VOID_CLOUD, VoidedCloudParticle.Provider::new);
        ParticleProviderRegistry.getInstance().register(VSQParticleTypes.VOID_PIXEL, VoidedPixelParticle.Provider::new);
        ParticleProviderRegistry.getInstance().register(VSQParticleTypes.LIGHTNING_BOLT, new LightningBoltParticle.Provider());
        MenuScreens.register(VSQMenuTypes.ENCHANTING, VSQEnchantmentScreen::new);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                EnchantingRecipeStatePayloadHandler.clearAll();
                SpecialEnchantmentCooldownClientState.clear();
                VoidedSoundController.clear();
                EnchantingIngredient.clearTagCache();
        });
    }
}
