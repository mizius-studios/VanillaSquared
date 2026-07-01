package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.recipe.VSQRecipeTypes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.io.Reader;
import java.util.*;

public final class EnchantingRecipeRegistry {
    private static final Identifier ENCHANTING_TYPE_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting");
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_recipe_loader");
    private static final FileToIdConverter RECIPE_CONVERTER = FileToIdConverter.json("recipe");
    private static volatile Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> RECIPES = Map.of();
    private static volatile Map<ResourceKey<Recipe<?>>, Integer> RECIPE_DISPLAY_IDS = Map.of();
    private static volatile Map<String, Integer> RECIPE_GROUP_IDS = Map.of();

    private EnchantingRecipeRegistry() {
    }

    public static void initialize() {
        DataResourceLoader.get().registerReloadListener(RELOAD_LISTENER_ID, registries -> new ReloadListener());
    }

    public static Collection<RecipeHolder<EnchantingRecipe>> recipes() {
        return RECIPES.values();
    }

    public static Collection<RecipeHolder<?>> recipeHolders() {
        return new java.util.ArrayList<>(RECIPES.values());
    }

    public static java.util.stream.Stream<Identifier> recipeIds() {
        return RECIPES.keySet().stream().map(ResourceKey::identifier);
    }

    public static Optional<RecipeHolder<EnchantingRecipe>> byKey(ResourceKey<Recipe<?>> recipeKey) {
        return Optional.ofNullable(RECIPES.get(recipeKey));
    }

    public static boolean contains(ResourceKey<Recipe<?>> recipeKey) {
        return RECIPES.containsKey(recipeKey);
    }

    public static OptionalInt displayId(ResourceKey<Recipe<?>> recipeKey) {
        Integer displayId = RECIPE_DISPLAY_IDS.get(recipeKey);
        return displayId != null ? OptionalInt.of(displayId) : OptionalInt.empty();
    }

    public static OptionalInt groupId(String group) {
        Integer groupId = RECIPE_GROUP_IDS.get(group);
        return groupId != null ? OptionalInt.of(groupId) : OptionalInt.empty();
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
                && recipe.hasRequiredBlocks(input, countedBlocks, registries);
    }

    private static final class ReloadListener extends SimpleReloadListener<Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>>> {
        @Override
        protected Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> prepare(PreparableReloadListener.SharedState store) {
            Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> loadedRecipes = new TreeMap<>(Comparator.comparing(ResourceKey::identifier));
            HolderLookup.Provider registries = store.get(ResourceLoader.REGISTRY_LOOKUP_KEY);
            RegistryOps<com.google.gson.JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

            for (Map.Entry<Identifier, Resource> entry : RECIPE_CONVERTER.listMatchingResources(store.resourceManager()).entrySet()) {
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
        }

        @Override
        protected void apply(Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> data, PreparableReloadListener.SharedState store) {
            EnchantingIngredient.clearTagCache();
            RECIPES = Map.copyOf(data);
            RECIPE_DISPLAY_IDS = vsq$createDisplayIds(data);
            RECIPE_GROUP_IDS = vsq$createGroupIds(data);
            EnchantingRecipeTags.invalidateValidRecipeCache();
            VanillaSquared.LOGGER.info("Loaded {} Enchanting recipes", RECIPES.size());
        }

        private static Map<ResourceKey<Recipe<?>>, Integer> vsq$createDisplayIds(Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> recipes) {
            Map<ResourceKey<Recipe<?>>, Integer> displayIds = new LinkedHashMap<>(recipes.size());
            int nextDisplayId = 0;
            for (ResourceKey<Recipe<?>> recipeKey : recipes.keySet()) {
                displayIds.put(recipeKey, nextDisplayId++);
            }
            return Map.copyOf(displayIds);
        }

        private static Map<String, Integer> vsq$createGroupIds(Map<ResourceKey<Recipe<?>>, RecipeHolder<EnchantingRecipe>> recipes) {
            Map<String, Integer> groupIds = new LinkedHashMap<>();
            int nextGroupId = 0;
            for (RecipeHolder<EnchantingRecipe> holder : recipes.values()) {
                String group = holder.value().group();
                if (group.isBlank() || groupIds.containsKey(group)) {
                    continue;
                }
                groupIds.put(group, nextGroupId++);
            }
            return Map.copyOf(groupIds);
        }
    }
}
