package blob.vanillasquared.main;

import blob.vanillasquared.main.network.VSQNetworking;
import blob.vanillasquared.main.sound.VSQSoundEvents;
import blob.vanillasquared.main.world.effect.VSQMobEffects;
import blob.vanillasquared.main.world.inventory.VSQMenuTypes;
import blob.vanillasquared.main.world.loot.VSQLootFunctions;
import blob.vanillasquared.main.world.particle.VSQParticleTypes;
import blob.vanillasquared.main.world.item.VSQItems;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeTags;
import blob.vanillasquared.main.world.recipe.VSQRecipeTypes;
import blob.vanillasquared.main.world.VSQExperiments;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentEffectComponents;
import blob.vanillasquared.main.world.item.enchantment.effects.VSQEnchantmentEntityEffects;
import blob.vanillasquared.util.api.modules.attributes.RegisterAttributes;
import blob.vanillasquared.util.api.modules.components.RegisterComponents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaSquared implements ModInitializer {
    public static final String MOD_ID = "vsq";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        VSQMenuTypes.initialize();
        VSQMobEffects.initialize();
        VSQSoundEvents.initialize();
        VSQItems.initialize();
        VSQRecipeTypes.initialize();
        VSQExperiments.initialize();
        EnchantingRecipeRegistry.initialize();
        EnchantingRecipeTags.initialize();
        VSQParticleTypes.initialize();
        RegisterComponents.initialize();
        VSQEnchantmentEffectComponents.initialize();
        VSQEnchantmentEntityEffects.initialize();
        VSQNetworking.initialize();
        RegisterAttributes.initialize();
        VSQLootFunctions.initialize();

        LOGGER.info("Blob");
        LOGGER.info("Enchanting recipe serializer registered as {}", BuiltInRegistries.RECIPE_SERIALIZER.getKey(VSQRecipeTypes.ENCHANTING_SERIALIZER));
        LOGGER.info("Enchanting recipe type registered as {}", BuiltInRegistries.RECIPE_TYPE.getKey(VSQRecipeTypes.ENCHANTING_TYPE));
    }
}
