package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.main.VanillaSquared;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.io.Reader;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class EnchantingRecipeRegistry {
    private static final Identifier ENCHANTING_TYPE_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting");
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_recipe_loader");
    private static final FileToIdConverter RECIPE_CONVERTER = FileToIdConverter.json("recipes");
    private static volatile Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> RECIPES = Map.of();

    private EnchantingRecipeRegistry() {
    }

    public static void initialize() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(RELOAD_LISTENER_ID, ReloadListener::new);
    }

    public static Collection<RecipeHolder<EnchantingRecipe>> recipes() {
        return RECIPES.values();
    }

    public static boolean contains(ResourceKey<Recipe<?>> recipeKey) {
        return RECIPES.containsKey(recipeKey);
    }

    public static Optional<RecipeHolder<EnchantingRecipe>> findFirstMatch(EnchantingRecipeInput input, Level level) {
        return RECIPES.values().stream()
                .filter(holder -> holder.value().matches(input, level))
                .findFirst();
    }

    public static Optional<RecipeHolder<EnchantingRecipe>> findFirstStructuralMatch(EnchantingRecipeInput input, HolderLookup.Provider registries) {
        return RECIPES.values().stream()
                .filter(holder -> vsq$hasStructuralMatch(holder.value(), input, registries))
                .findFirst();
    }

    public static Optional<RecipeHolder<EnchantingRecipe>> findFirstCraftableMatch(EnchantingRecipeInput input, int playerLevel, Map<Identifier, Integer> countedBlocks, HolderLookup.Provider registries) {
        return RECIPES.values().stream()
                .filter(holder -> vsq$hasCraftableMatch(holder.value(), input, playerLevel, countedBlocks, registries))
                .findFirst();
    }

    private static boolean vsq$hasStructuralMatch(EnchantingRecipe recipe, EnchantingRecipeInput input, HolderLookup.Provider registries) {
        return recipe.findMatch(input, registries).isPresent()
                && recipe.isBelowMaximumEnchantmentLevel(input, registries)
                && recipe.wouldModifyInput(input, registries)
                && recipe.respectsVanillaEnchantmentIncompatibilities(input, registries);
    }

    private static boolean vsq$hasCraftableMatch(EnchantingRecipe recipe, EnchantingRecipeInput input, int playerLevel, Map<Identifier, Integer> countedBlocks, HolderLookup.Provider registries) {
        return vsq$hasStructuralMatch(recipe, input, registries)
                && recipe.canPlayerCraft(input, playerLevel, registries)
                && recipe.hasRequiredBlocks(countedBlocks);
    }

    private static final class ReloadListener implements SimpleResourceReloadListener<Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>>>, IdentifiableResourceReloadListener {
        private final HolderLookup.Provider registries;

        private ReloadListener(HolderLookup.Provider registries) {
            this.registries = registries;
        }

        @Override
        public Identifier getFabricId() {
            return RELOAD_LISTENER_ID;
        }

        @Override
        public CompletableFuture<Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>>> load(ResourceManager resourceManager, Executor executor) {
            return CompletableFuture.supplyAsync(() -> {
                Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> loadedRecipes = new TreeMap<>((left, right) -> left.identifier().compareTo(right.identifier()));
                RegistryOps<com.google.gson.JsonElement> ops = this.registries.createSerializationContext(JsonOps.INSTANCE);

                for (Map.Entry<Identifier, Resource> entry : RECIPE_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
                    Identifier fileId = entry.getKey();
                    Identifier recipeId = RECIPE_CONVERTER.fileToId(fileId);

                    try (Reader reader = entry.getValue().openAsReader()) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        if (!json.has("type")) {
                            continue;
                        }

                        Identifier typeId = Identifier.tryParse(json.get("type").getAsString());
                        if (!ENCHANTING_TYPE_ID.equals(typeId)) {
                            continue;
                        }

                        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);
                        EnchantingRecipe recipe = VSQRecipeTypes.ENCHANTING_SERIALIZER.codec().codec()
                                .parse(ops, json)
                                .getOrThrow(message -> new IllegalArgumentException("Failed to parse Enchanting recipe " + recipeId + ": " + message));
                        loadedRecipes.put(recipeKey, new RecipeHolder<>(recipeKey, recipe));
                    } catch (Exception exception) {
                        VanillaSquared.LOGGER.error("Failed to load Enchanting recipe {} from {}", recipeId, fileId, exception);
                    }
                }

                return loadedRecipes;
            }, executor);
        }

        @Override
        public CompletableFuture<Void> apply(Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> data, ResourceManager resourceManager, Executor executor) {
            return CompletableFuture.runAsync(() -> {
                EnchantingIngredient.clearTagCache();
                RECIPES = Map.copyOf(data);
                VanillaSquared.LOGGER.info("Loaded {} Enchanting recipes", RECIPES.size());
            }, executor);
        }
    }
}
