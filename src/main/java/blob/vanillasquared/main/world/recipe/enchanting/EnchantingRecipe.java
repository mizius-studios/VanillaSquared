package blob.vanillasquared.main.world.recipe.enchanting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EnchantingRecipe(
        EnchantingRecipeCategory category,
        String group,
        Component name,
        Component description,
        EnchantingIngredient material,
        List<EnchantingIngredient> ingredients,
        List<EnchantingBlockRequirement> blocks,
        int levelMultiplier,
        EnchantingRecipeEnchantment enchantment
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
            EnchantingRecipeCategory.CODEC.fieldOf("category").forGetter(EnchantingRecipe::category),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(EnchantingRecipe::group),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(EnchantingRecipe::name),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(EnchantingRecipe::description),
            EnchantingIngredient.CODEC.fieldOf("material").forGetter(EnchantingRecipe::material),
            INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(EnchantingRecipe::ingredients),
            BLOCKS_CODEC.optionalFieldOf("blocks", List.of()).forGetter(EnchantingRecipe::blocks),
            com.mojang.serialization.Codec.INT.optionalFieldOf("level_multiplier", 1).forGetter(EnchantingRecipe::levelMultiplier),
            EnchantingRecipeEnchantment.CODEC.fieldOf("enchantment").forGetter(EnchantingRecipe::enchantment)
    ).apply(instance, EnchantingRecipe::vsq$create));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnchantingRecipe decode(RegistryFriendlyByteBuf buf) {
            return EnchantingRecipe.vsq$create(
                    EnchantingRecipeCategory.fromSerializedName(ByteBufCodecs.stringUtf8(16).decode(buf)),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    EnchantingIngredient.STREAM_CODEC.decode(buf),
                    EnchantingIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    EnchantingBlockRequirement.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    EnchantingRecipeEnchantment.STREAM_CODEC.decode(buf)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantingRecipe value) {
            ByteBufCodecs.stringUtf8(16).encode(buf, value.category().serializedName());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.group());
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, value.name());
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, value.description());
            EnchantingIngredient.STREAM_CODEC.encode(buf, value.material());
            EnchantingIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, value.ingredients());
            EnchantingBlockRequirement.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, value.blocks());
            ByteBufCodecs.VAR_INT.encode(buf, value.levelMultiplier());
            EnchantingRecipeEnchantment.STREAM_CODEC.encode(buf, value.enchantment());
        }
    };

    public EnchantingRecipe {
        group = group == null ? "" : group;
        ingredients = List.copyOf(ingredients);
        blocks = List.copyOf(blocks);
        name = name.copy();
        description = description.copy();
        levelMultiplier = Math.max(levelMultiplier, 1);
        if (ingredients.size() != 4) {
            throw new IllegalArgumentException("Enchanting recipes require exactly 4 cross ingredients");
        }
    }

    @Override
    public boolean matches(EnchantingRecipeInput input, Level level) {
        return this.findMatch(input, level.registryAccess()).isPresent();
    }

    @Override
    public ItemStack assemble(EnchantingRecipeInput input) {
        return input.input().copy();
    }

    public ItemStack assemble(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.enchantment.apply(input.input(), registries);
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public String group() {
        return this.group;
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
        return this.category.recipeBookCategory();
    }

    public Optional<Match> findMatch(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        if (!this.inputIngredient(registries).test(input.input()) || !this.material.test(input.material())) {
            return Optional.empty();
        }
        return this.vsq$findCrossMatch(input);
    }

    private Optional<Match> vsq$findCrossMatch(EnchantingRecipeInput input) {
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

    public boolean canPlayerCraft(EnchantingRecipeInput input, int playerLevel, net.minecraft.core.HolderLookup.Provider registries) {
        return playerLevel >= this.xpCost(input, registries);
    }

    public boolean wouldModifyInput(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.enchantment.modifies(input.input(), registries);
    }

    public boolean isBelowMaximumEnchantmentLevel(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.enchantment.isBelowMaximumLevel(input.input(), registries);
    }

    public boolean respectsVanillaEnchantmentIncompatibilities(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.enchantment.respectsVanillaEnchantmentIncompatibilities(input.input(), registries);
    }

    public int xpCost(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.enchantment.xpCost(input.input(), registries) * this.levelMultiplier;
    }

    public Component displayName(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.enchantment.displayName(input.input(), registries);
    }

    public Component previewName(net.minecraft.core.HolderLookup.Provider registries) {
        return this.enchantment.displayName(ItemStack.EMPTY, registries);
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

    public EnchantingIngredient inputIngredient(net.minecraft.core.HolderLookup.Provider registries) {
        return new EnchantingIngredient(this.enchantment.supportedItemsIngredient(registries), 1, null);
    }

    private static EnchantingRecipe vsq$create(EnchantingRecipeCategory category, String group, Component name, Component description, EnchantingIngredient material, List<EnchantingIngredient> ingredients, List<EnchantingBlockRequirement> blocks, int levelMultiplier, EnchantingRecipeEnchantment enchantment) {
        return new EnchantingRecipe(category, group, name, description, material, ingredients, blocks, levelMultiplier, enchantment);
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

    public record BlockRequirementDisplay(Identifier blockId, int placedCount, int requiredCount) {
    }
}
