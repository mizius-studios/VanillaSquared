package blob.vanillasquared.main;

import blob.vanillasquared.mixin.world.inventory.RecipeManagerAccessor;
import blob.vanillasquared.main.network.VSQNetworking;
import blob.vanillasquared.main.world.item.Items.TestItem;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import blob.vanillasquared.main.world.recipe.enchanting.VSQRecipeTypes;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.api.ModInitializer;
import blob.vanillasquared.main.world.item.components.dualwield.DualWieldEvents;
import blob.vanillasquared.main.world.item.components.specialeffect.SpecialEffectEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import blob.vanillasquared.util.api.modules.attributes.RegisterAttributes;
import blob.vanillasquared.util.api.modules.components.RegisterComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class VanillaSquared implements ModInitializer {
    public static final String MOD_ID = "vsq";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    @Override
    public void onInitialize() {
        TestItem.initialize();
        VSQRecipeTypes.initialize();
        EnchantingRecipeRegistry.initialize();
        RegisterComponents.initialize();
        DualWieldEvents.initialize();
        SpecialEffectEvents.initialize();
        VSQNetworking.initialize();

        LOGGER.info("Blob");
        LOGGER.info("Enchanting recipe serializer registered as {}", BuiltInRegistries.RECIPE_SERIALIZER.getKey(VSQRecipeTypes.ENCHANTING_SERIALIZER));
        LOGGER.info("Enchanting recipe type registered as {}", BuiltInRegistries.RECIPE_TYPE.getKey(VSQRecipeTypes.ENCHANTING_TYPE));
        try (var stream = VanillaSquared.class.getResourceAsStream("/data/vsq/recipes/enchanting_debug_apple.json")) {
            if (stream == null) {
                LOGGER.warn("Could not find classpath recipe probe for /data/vsq/recipes/enchanting_debug_apple.json");
            } else {
                var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                var parseResult = VSQRecipeTypes.ENCHANTING_SERIALIZER.codec().codec().parse(JsonOps.INSTANCE, json);
                if (parseResult.isSuccess()) {
                    LOGGER.info("Enchanting recipe codec probe parsed successfully");
                } else {
                    LOGGER.warn("Enchanting recipe codec probe failed: {}", parseResult);
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Enchanting recipe codec probe crashed", exception);
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Identifier recipeId = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_debug_apple");
            Identifier recipeFileId = FileToIdConverter.json("recipes").idToFile(recipeId);
            ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);

            LOGGER.info("Server recipe resource probe for {} found {} raw resource(s)", recipeFileId, server.getResourceManager().getResourceStack(recipeFileId).size());
            LOGGER.info("Server recipe manager probe for {} present={}", recipeId, server.getRecipeManager().byKey(recipeKey).isPresent());
            server.getResourceManager().getResourceStack(recipeFileId).stream().findFirst().ifPresent(resource -> {
                try (var reader = resource.openAsReader()) {
                    var json = JsonParser.parseReader(reader).getAsJsonObject();
                    RecipeManagerAccessor.vsq$fromJson(recipeKey, json, server.registryAccess());
                    LOGGER.info("Direct RecipeManager.fromJson probe for {} succeeded", recipeId);
                } catch (Exception exception) {
                    LOGGER.error("Direct RecipeManager.fromJson probe for {} failed", recipeId, exception);
                }
            });
        });

        RegisterAttributes.initialize();
    }
}
