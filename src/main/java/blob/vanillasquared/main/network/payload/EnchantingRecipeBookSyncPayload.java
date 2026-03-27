package blob.vanillasquared.main.network.payload;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingIngredient;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipe;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeCategory;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.Empty;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public record EnchantingRecipeBookSyncPayload(int containerId, boolean replace, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<EnchantingRecipeBookSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting_recipe_book_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingRecipeBookSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantingRecipeBookSyncPayload::containerId,
            ByteBufCodecs.BOOL, EnchantingRecipeBookSyncPayload::replace,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), EnchantingRecipeBookSyncPayload::entries,
            EnchantingRecipeBookSyncPayload::new
    );

    public EnchantingRecipeBookSyncPayload {
        entries = List.copyOf(entries);
    }

    public static EnchantingRecipeBookSyncPayload create(int containerId, boolean replace, Iterable<RecipeView> recipes, HolderLookup.Provider registries) {
        List<Entry> entries = new ArrayList<>();
        Map<String, Integer> groupIds = new LinkedHashMap<>();
        int displayId = 0;
        for (RecipeView view : recipes) {
            EnchantingRecipe recipe = view.holder().value();
            OptionalInt group = recipe.group().isBlank()
                    ? OptionalInt.empty()
                    : OptionalInt.of(groupIds.computeIfAbsent(recipe.group(), ignored -> groupIds.size()));
            entries.add(new Entry(
                    displayId++,
                    recipe.category(),
                    group,
                    vsq$createDisplay(recipe, registries, view.previewInput()),
                    view.craftable() ? vsq$createCraftingRequirements(recipe) : Optional.empty()
            ));
        }
        return new EnchantingRecipeBookSyncPayload(containerId, replace, entries);
    }

    public static RecipeDisplay createDisplay(EnchantingRecipe recipe, HolderLookup.Provider registries) {
        return vsq$createDisplay(recipe, registries, Optional.empty());
    }

    private static RecipeDisplay vsq$createDisplay(EnchantingRecipe recipe, HolderLookup.Provider registries, Optional<EnchantingRecipeInput> previewInput) {
        List<SlotDisplay> ingredients = new ArrayList<>(6);
        if (previewInput.isPresent()) {
            EnchantingRecipeInput input = previewInput.get();
            ingredients.add(vsq$stackDisplay(input.input()));
            ingredients.add(vsq$stackDisplay(input.material()));
            for (ItemStack stack : input.ingredients()) {
                ingredients.add(vsq$stackDisplay(stack));
            }
        } else {
            ingredients.add(recipe.input().display());
            ingredients.add(recipe.material().display());
            for (EnchantingIngredient ingredient : recipe.ingredients()) {
                ingredients.add(ingredient.display());
            }
        }
        ItemStack previewStack = previewInput.map(EnchantingRecipeInput::input).map(ItemStack::copy).orElseGet(recipe.input()::previewStack);
        ItemStack previewResult = previewStack.isEmpty() ? new ItemStack(Items.ENCHANTED_BOOK) : recipe.componentModifier().apply(previewStack, registries);
        return new ShapelessCraftingRecipeDisplay(
                ingredients,
                previewResult.isEmpty() ? Empty.INSTANCE : new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(previewResult)),
                new SlotDisplay.ItemSlotDisplay(Items.ENCHANTING_TABLE)
        );
    }

    private static Optional<List<Ingredient>> vsq$createCraftingRequirements(EnchantingRecipe recipe) {
        List<Ingredient> craftingRequirements = new ArrayList<>();
        if (!vsq$appendRequirements(craftingRequirements, recipe.input())) {
            return Optional.empty();
        }
        if (!vsq$appendRequirements(craftingRequirements, recipe.material())) {
            return Optional.empty();
        }
        for (EnchantingIngredient ingredient : recipe.ingredients()) {
            if (!vsq$appendRequirements(craftingRequirements, ingredient)) {
                return Optional.empty();
            }
        }
        return Optional.of(List.copyOf(craftingRequirements));
    }

    private static boolean vsq$appendRequirements(List<Ingredient> craftingRequirements, EnchantingIngredient ingredient) {
        Optional<Ingredient> safeIngredient = ingredient.safeIngredient();
        if (safeIngredient.isEmpty()) {
            return false;
        }
        for (int index = 0; index < ingredient.count(); index++) {
            craftingRequirements.add(safeIngredient.get());
        }
        return true;
    }

    private static SlotDisplay vsq$stackDisplay(ItemStack stack) {
        return stack.isEmpty() ? Empty.INSTANCE : new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int displayId, EnchantingRecipeCategory category, OptionalInt group, RecipeDisplay display, Optional<List<Ingredient>> craftingRequirements) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::displayId,
                ByteBufCodecs.stringUtf8(16), entry -> entry.category.serializedName(),
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), entry -> entry.group.isPresent() ? Optional.of(entry.group.getAsInt()) : Optional.empty(),
                RecipeDisplay.STREAM_CODEC, Entry::display,
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list())), Entry::craftingRequirements,
                (displayId, categoryName, group, display, craftingRequirements) -> new Entry(
                        displayId,
                        EnchantingRecipeCategory.fromSerializedName(categoryName),
                        group.map(OptionalInt::of).orElseGet(OptionalInt::empty),
                        display,
                        craftingRequirements
                )
        );

        public net.minecraft.world.item.crafting.display.RecipeDisplayEntry toDisplayEntry() {
            return new net.minecraft.world.item.crafting.display.RecipeDisplayEntry(
                    new RecipeDisplayId(this.displayId),
                    this.display,
                    this.group,
                    this.category.recipeBookCategory(),
                    this.craftingRequirements
            );
        }
    }

    public record RecipeView(RecipeHolder<EnchantingRecipe> holder, Optional<EnchantingRecipeInput> previewInput, boolean craftable) {
    }
}
