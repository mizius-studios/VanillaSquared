package blob.vanillasquared.main.world.recipe.enchanting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EnchantingRecipe(
        int level,
        int consumedLevels,
        EnchantingIngredient input,
        EnchantingIngredient material,
        List<EnchantingIngredient> ingredients,
        List<EnchantingBlockRequirement> blocks,
        EnchantingComponentModifier componentModifier
) implements Recipe<EnchantingRecipeInput> {
    private static final com.mojang.serialization.Codec<List<EnchantingIngredient>> INGREDIENTS_CODEC = net.minecraft.util.ExtraCodecs.JSON.flatXmap(
            EnchantingRecipe::vsq$decodeIngredients,
            EnchantingRecipe::vsq$encodeIngredients
    );
    private static final com.mojang.serialization.Codec<List<EnchantingBlockRequirement>> BLOCKS_CODEC = net.minecraft.util.ExtraCodecs.JSON.flatXmap(
            EnchantingRecipe::vsq$decodeBlocks,
            EnchantingRecipe::vsq$encodeBlocks
    );

    public static final MapCodec<EnchantingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("level", 0).forGetter(EnchantingRecipe::level),
            com.mojang.serialization.Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("consumed_levels", 0).forGetter(EnchantingRecipe::consumedLevels),
            EnchantingIngredient.CODEC.fieldOf("input").forGetter(EnchantingRecipe::input),
            EnchantingIngredient.CODEC.fieldOf("material").forGetter(EnchantingRecipe::material),
            INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(EnchantingRecipe::ingredients),
            BLOCKS_CODEC.optionalFieldOf("blocks", List.of()).forGetter(EnchantingRecipe::blocks),
            EnchantingComponentModifier.CODEC.fieldOf("component_modifier").forGetter(EnchantingRecipe::componentModifier)
    ).apply(instance, EnchantingRecipe::vsq$create));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantingRecipe::level,
            ByteBufCodecs.VAR_INT, EnchantingRecipe::consumedLevels,
            EnchantingIngredient.STREAM_CODEC, EnchantingRecipe::input,
            EnchantingIngredient.STREAM_CODEC, EnchantingRecipe::material,
            EnchantingIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), EnchantingRecipe::ingredients,
            EnchantingBlockRequirement.STREAM_CODEC.apply(ByteBufCodecs.list()), EnchantingRecipe::blocks,
            EnchantingComponentModifier.STREAM_CODEC, EnchantingRecipe::componentModifier,
            EnchantingRecipe::vsq$create
    );

    public EnchantingRecipe {
        ingredients = List.copyOf(ingredients);
        blocks = List.copyOf(blocks);
        if (ingredients.size() != 4) {
            throw new IllegalArgumentException("Enchanting recipes require exactly 4 cross ingredients");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Enchanting recipe level must be non-negative");
        }
        if (consumedLevels < 0) {
            throw new IllegalArgumentException("Enchanting recipe consumed levels must be non-negative");
        }
        consumedLevels = Math.min(consumedLevels, level);
    }

    @Override
    public boolean matches(EnchantingRecipeInput input, Level level) {
        return this.findMatch(input).isPresent();
    }

    @Override
    public ItemStack assemble(EnchantingRecipeInput input) {
        return input.input().copy();
    }

    public ItemStack assemble(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.componentModifier.apply(input.input(), registries);
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        List<Ingredient> placementIngredients = new ArrayList<>(6);
        placementIngredients.add(this.input.ingredient());
        placementIngredients.add(this.material.ingredient());
        for (EnchantingIngredient ingredient : this.ingredients) {
            placementIngredients.add(ingredient.ingredient());
        }
        return PlacementInfo.create(placementIngredients);
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<EnchantingRecipe> getSerializer() {
        return VSQRecipeTypes.ENCHANTING_SERIALIZER;
    }

    @Override
    public RecipeType<EnchantingRecipe> getType() {
        return VSQRecipeTypes.ENCHANTING_TYPE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return new RecipeBookCategory();
    }

    public Optional<Match> findMatch(EnchantingRecipeInput input) {
        if (!this.input.test(input.input()) || !this.material.test(input.material())) {
            return Optional.empty();
        }

        List<ItemStack> crossStacks = input.ingredients();
        boolean[] usedSlots = new boolean[crossStacks.size()];
        List<Integer> matchedCrossSlots = new ArrayList<>(this.ingredients.size());

        for (ItemStack stack : crossStacks) {
            if (stack.isEmpty()) {
                return Optional.empty();
            }
        }

        for (EnchantingIngredient ingredient : this.ingredients) {
            boolean matched = false;

            for (int i = 0; i < crossStacks.size(); i++) {
                if (usedSlots[i] || !ingredient.test(crossStacks.get(i))) {
                    continue;
                }

                usedSlots[i] = true;
                matchedCrossSlots.add(i);
                matched = true;
                break;
            }

            if (!matched) {
                return Optional.empty();
            }
        }

        return Optional.of(new Match(matchedCrossSlots));
    }

    public boolean canPlayerCraft(int playerLevel) {
        return playerLevel >= this.level;
    }

    public boolean hasRequiredBlocks(java.util.Map<net.minecraft.resources.Identifier, Integer> countedBlocks) {
        for (EnchantingBlockRequirement requirement : this.blocks) {
            if (!requirement.matches(countedBlocks)) {
                return false;
            }
        }
        return true;
    }

    public List<BlockRequirementDisplay> blockRequirementDisplay(java.util.Map<net.minecraft.resources.Identifier, Integer> countedBlocks) {
        List<BlockRequirementDisplay> display = new ArrayList<>(this.blocks.size());
        for (EnchantingBlockRequirement requirement : this.blocks) {
            display.add(new BlockRequirementDisplay(requirement.displayBlockId(), requirement.placedCount(countedBlocks), requirement.count()));
        }
        return display;
    }

    private static EnchantingRecipe vsq$create(int level, int consumedLevels, EnchantingIngredient input, EnchantingIngredient material, List<EnchantingIngredient> ingredients, List<EnchantingBlockRequirement> blocks, EnchantingComponentModifier componentModifier) {
        return new EnchantingRecipe(level, consumedLevels, input, material, ingredients, blocks, componentModifier);
    }

    private static DataResult<List<EnchantingIngredient>> vsq$decodeIngredients(JsonElement json) {
        if (!json.isJsonArray()) {
            return DataResult.error(() -> "Enchanting recipe ingredients must be a JSON array");
        }

        List<EnchantingIngredient> decoded = new ArrayList<>();
        int index = 0;
        for (JsonElement element : json.getAsJsonArray()) {
            int currentIndex = index++;
            var parsedResult = EnchantingIngredient.CODEC.parse(JsonOps.INSTANCE, element);
            var parsed = parsedResult.result();
            if (parsed.isEmpty()) {
                String details = parsedResult.error().map(DataResult.Error::message).orElse("unknown reason");
                return DataResult.error(() -> "Failed to parse enchanting ingredient at index " + currentIndex + ": " + details);
            }
            decoded.add(parsed.get());
        }
        return DataResult.success(decoded);
    }

    private static DataResult<JsonElement> vsq$encodeIngredients(List<EnchantingIngredient> ingredients) {
        JsonArray array = new JsonArray();
        for (EnchantingIngredient ingredient : ingredients) {
            var encoded = EnchantingIngredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).result();
            if (encoded.isEmpty()) {
                return DataResult.error(() -> "Failed to encode enchanting ingredient list");
            }
            array.add(encoded.get());
        }
        return DataResult.success(array);
    }

    private static DataResult<List<EnchantingBlockRequirement>> vsq$decodeBlocks(JsonElement json) {
        if (!json.isJsonArray()) {
            return DataResult.error(() -> "Enchanting recipe blocks must be a JSON array");
        }

        List<EnchantingBlockRequirement> decoded = new ArrayList<>();
        int index = 0;
        for (JsonElement element : json.getAsJsonArray()) {
            int currentIndex = index++;
            var parsedResult = EnchantingBlockRequirement.CODEC.parse(JsonOps.INSTANCE, element);
            var parsed = parsedResult.result();
            if (parsed.isEmpty()) {
                String details = parsedResult.error().map(DataResult.Error::message).orElse("unknown reason");
                return DataResult.error(() -> "Failed to parse enchanting block requirement at index " + currentIndex + ": " + details);
            }
            decoded.add(parsed.get());
        }
        return DataResult.success(decoded);
    }

    private static DataResult<JsonElement> vsq$encodeBlocks(List<EnchantingBlockRequirement> blocks) {
        JsonArray array = new JsonArray();
        for (EnchantingBlockRequirement blockRequirement : blocks) {
            var encoded = EnchantingBlockRequirement.CODEC.encodeStart(JsonOps.INSTANCE, blockRequirement).result();
            if (encoded.isEmpty()) {
                return DataResult.error(() -> "Failed to encode enchanting block requirement list");
            }
            array.add(encoded.get());
        }
        return DataResult.success(array);
    }

    public record Match(List<Integer> matchedCrossSlots) {
        public Match {
            matchedCrossSlots = List.copyOf(matchedCrossSlots);
        }
    }

    public record BlockRequirementDisplay(net.minecraft.resources.Identifier blockId, int placedCount, int requiredCount) {
    }
}
