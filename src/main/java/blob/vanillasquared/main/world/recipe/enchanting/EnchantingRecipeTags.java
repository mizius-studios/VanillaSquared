package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.loot.LootTableIdResolver;
import blob.vanillasquared.main.world.item.VSQItems;
import blob.vanillasquared.util.api.modules.components.VSQItemComponents;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantingRecipeTags {
    public static final Identifier DEFAULT_LOOT_TAG = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "default");
    public static final Identifier DEFAULT_LIBRARIAN_TAG = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "villager/librarian/default");
    private static final Map<String, Identifier> LOOT_TABLE_TO_TAG = Map.ofEntries(
            Map.entry("chests/jungle_temple", tag("jungle_temple_chest")),
            Map.entry("chests/stronghold_crossing", tag("stronghold_storeroom_chest")),
            Map.entry("chests/stronghold_library", tag("stronghold_library_chest")),
            Map.entry("chests/stronghold_corridor", tag("stronghold_altar_chest")),
            Map.entry("chests/simple_dungeon", tag("monster_room_chest")),
            Map.entry("chests/abandoned_mineshaft", tag("mineshaft_chest")),
            Map.entry("chests/ancient_city", tag("ancient_city_chest")),
            Map.entry("chests/desert_pyramid", tag("desert_pyramid_chest")),
            Map.entry("chests/pillager_outpost", tag("pillager_outpost_chest")),
            Map.entry("chests/underwater_ruin_big", tag("ocean_ruins_big_ruins_chest")),
            Map.entry("chests/woodland_mansion", tag("woodland_mansion_chest")),
            Map.entry("chests/bastion_other", tag("bastion_remnant_generic_chest")),
            Map.entry("chests/bastion_remnant_generic", tag("bastion_remnant_generic_chest")),
            Map.entry("chests/trial_chambers/reward", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_common", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_rare", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_unique", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous_common", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous_rare", tag("trial_chamber_vault")),
            Map.entry("chests/trial_chambers/reward_ominous_unique", tag("trial_chamber_vault")),
            Map.entry("gameplay/fishing/treasure", tag("fishing")),
            Map.entry("gameplay/fishing", tag("fishing")),
            Map.entry("gameplay/piglin_bartering", tag("piglin_bartering")),
            Map.entry("chests/end_city_treasure", tag("end_city_treasure_chest"))
    );
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_recipe_tag_loader");
    private static final FileToIdConverter TAG_CONVERTER = FileToIdConverter.json("tags/recipe");
    private static volatile Map<Identifier, List<ResourceKey<Recipe<?>>>> TAGS = Map.of();
    private static final Map<Identifier, List<ResourceKey<Recipe<?>>>> VALID_RECIPE_CACHE = new ConcurrentHashMap<>();
    private static final Set<Identifier> WARNED_EMPTY_TAGS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private EnchantingRecipeTags() {
    }

    public static void initialize() {
        DataResourceLoader.get().registerReloadListener(RELOAD_LISTENER_ID, registries -> new ReloadListener());
    }

    public static boolean isValidRecipe(ResourceKey<Recipe<?>> recipeKey) {
        return EnchantingRecipeRegistry.contains(recipeKey);
    }

    public static Optional<ResourceKey<Recipe<?>>> randomRecipe(Identifier tagId, RandomSource random) {
        List<ResourceKey<Recipe<?>>> entries = VALID_RECIPE_CACHE.computeIfAbsent(tagId, id -> TAGS.getOrDefault(id, List.of()).stream()
                .filter(EnchantingRecipeRegistry::contains)
                .toList());
        if (entries.isEmpty()) {
            if (WARNED_EMPTY_TAGS.add(tagId)) {
                VanillaSquared.LOGGER.warn("Enchant recipe tag {} is missing, empty, or contains no valid enchanting recipes", tagId);
            }
            return Optional.empty();
        }
        return Optional.of(entries.get(random.nextInt(entries.size())));
    }

    static void invalidateValidRecipeCache() {
        VALID_RECIPE_CACHE.clear();
        WARNED_EMPTY_TAGS.clear();
    }

    public static ItemStack createStack(ResourceKey<Recipe<?>> recipeKey) {
        ItemStack stack = new ItemStack(VSQItems.ENCHANT_RECIPE);
        VSQItemComponents.setEnchantRecipe(stack, recipeKey);
        return stack;
    }

    public static ItemStack createRandomStack(Identifier tagId, RandomSource random) {
        return randomRecipe(tagId, random)
                .map(EnchantingRecipeTags::createStack)
                .orElse(ItemStack.EMPTY);
    }

    public static Identifier tag(String path) {
        return Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, path);
    }

    public static Identifier lootTagForTable(Identifier lootTableId) {
        return "minecraft".equals(lootTableId.getNamespace()) ? LOOT_TABLE_TO_TAG.getOrDefault(lootTableId.getPath(), DEFAULT_LOOT_TAG) : DEFAULT_LOOT_TAG;
    }

    public static Identifier librarianTagForVariant(Identifier villagerVariantId) {
        if (!"minecraft".equals(villagerVariantId.getNamespace())) {
            return DEFAULT_LIBRARIAN_TAG;
        }
        return switch (villagerVariantId.getPath()) {
            case "desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga" ->
                    tag("villager/librarian/" + villagerVariantId.getPath());
            default -> DEFAULT_LIBRARIAN_TAG;
        };
    }

    private static final class ReloadListener extends SimpleReloadListener<Map<Identifier, List<ResourceKey<Recipe<?>>>>> {
        @Override
        protected Map<Identifier, List<ResourceKey<Recipe<?>>>> prepare(PreparableReloadListener.SharedState store) {
            Map<Identifier, List<ResourceKey<Recipe<?>>>> loaded = new LinkedHashMap<>();
            for (Identifier fileId : TAG_CONVERTER.listMatchingResources(store.resourceManager()).keySet()) {
                Identifier tagId = TAG_CONVERTER.fileToId(fileId);
                List<Resource> stack;
                try {
                    stack = store.resourceManager().getResourceStack(fileId);
                } catch (Exception exception) {
                    VanillaSquared.LOGGER.error("Failed to resolve enchant recipe tag stack for {}", fileId, exception);
                    continue;
                }

                List<ResourceKey<Recipe<?>>> mergedValues = new ArrayList<>();
                for (Resource resource : stack) {
                    try (Reader reader = resource.openAsReader()) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        if (json.has("replace") && json.get("replace").isJsonPrimitive() && json.get("replace").getAsBoolean()) {
                            mergedValues.clear();
                        }
                        if (!json.has("values") || !json.get("values").isJsonArray()) {
                            continue;
                        }
                        mergedValues.addAll(vsq$parseValues(tagId, json.getAsJsonArray("values")));
                    } catch (Exception exception) {
                        VanillaSquared.LOGGER.error("Failed to load enchant recipe tag {} from {}", tagId, fileId, exception);
                    }
                }

                loaded.put(tagId, mergedValues.stream().distinct().collect(Collectors.toUnmodifiableList()));
            }
            return loaded;
        }

        private static List<ResourceKey<Recipe<?>>> vsq$parseValues(Identifier tagId, com.google.gson.JsonArray values) {
            List<ResourceKey<Recipe<?>>> parsed = new ArrayList<>(values.size());
            for (JsonElement value : values) {
                String rawId = null;
                Identifier recipeId = null;
                if (value.isJsonPrimitive()) {
                    rawId = value.getAsString();
                } else if (value.isJsonObject()) {
                    JsonObject entry = value.getAsJsonObject();
                    if (entry.has("id") && entry.get("id").isJsonPrimitive()) {
                        rawId = entry.get("id").getAsString();
                    }
                }

                if (rawId != null && rawId.startsWith("#")) {
                    VanillaSquared.LOGGER.warn("Ignoring unsupported enchant recipe tag reference {} in tag {}", rawId, tagId);
                    continue;
                }

                recipeId = rawId == null ? null : Identifier.tryParse(rawId);
                if (recipeId == null) {
                    continue;
                }
                parsed.add(ResourceKey.create(Registries.RECIPE, recipeId));
            }
            return parsed;
        }

        @Override
        protected void apply(Map<Identifier, List<ResourceKey<Recipe<?>>>> data, PreparableReloadListener.SharedState store) {
            LootTableIdResolver.clearCache();
            TAGS = Map.copyOf(data);
            invalidateValidRecipeCache();
            VanillaSquared.LOGGER.info("Loaded {} enchant recipe tags", TAGS.size());
        }
    }
}
