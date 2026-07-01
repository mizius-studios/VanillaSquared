package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.main.network.handlers.EnchantingRecipeBookSyncPayloadHandler;
import blob.vanillasquared.main.network.payload.EnchantingRecipeSelectionPayload;
import blob.vanillasquared.main.VanillaSquaredClient;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import blob.vanillasquared.main.world.recipe.enchanting.VSQEnchantmentRecipeBookCategories;
import blob.vanillasquared.mixin.client.gui.GhostSlotsAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class VSQEnchantmentRecipeBookComponent extends RecipeBookComponent<VSQEnchantmentMenu> {
    private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(VanillaSquaredClient.MOD_ID, "containers/recipe_book/enchant_filter_enabled"),
            net.minecraft.resources.Identifier.fromNamespaceAndPath(VanillaSquaredClient.MOD_ID, "containers/recipe_book/enchant_filter_disabled"),
            net.minecraft.resources.Identifier.fromNamespaceAndPath(VanillaSquaredClient.MOD_ID, "containers/recipe_book/enchant_filter_enabled_highlighted"),
            net.minecraft.resources.Identifier.fromNamespaceAndPath(VanillaSquaredClient.MOD_ID, "containers/recipe_book/enchant_filter_disabled_highlighted")
    );

    private static final List<TabInfo> TABS = List.of(
            new TabInfo(vsq$enchanted(Items.COMPASS), Optional.empty(), VSQEnchantmentRecipeBookCategories.ALL),
            new TabInfo(vsq$enchanted(Items.IRON_SWORD), Optional.empty(), VSQEnchantmentRecipeBookCategories.WEAPONS),
            new TabInfo(vsq$enchanted(Items.IRON_PICKAXE), Optional.empty(), VSQEnchantmentRecipeBookCategories.TOOLS),
            new TabInfo(vsq$enchanted(Items.IRON_CHESTPLATE), Optional.empty(), VSQEnchantmentRecipeBookCategories.ARMOR),
            new TabInfo(vsq$enchanted(Items.ENCHANTED_BOOK), Optional.empty(), VSQEnchantmentRecipeBookCategories.UTIL)
    );

    public VSQEnchantmentRecipeBookComponent(VSQEnchantmentMenu menu) {
        super(menu, TABS);
    }

    @Override
    protected @NonNull WidgetSprites getFilterButtonTextures() {
        return FILTER_BUTTON_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(net.minecraft.world.inventory.@NonNull Slot slot) {
        return this.menu.vsq$getEnchantingSlots().contains(slot);
    }

    @Override
    protected void selectMatchingRecipes(RecipeCollection recipeCollection, @NonNull StackedItemContents stackedItemContents) {
        recipeCollection.selectRecipes(stackedItemContents, display -> display instanceof ShapelessCraftingRecipeDisplay shapeless && shapeless.ingredients().size() == VSQEnchantmentMenu.TABLE_SLOT_COUNT);
    }

    @Override
    protected @NonNull Component getRecipeFilterName() {
        return Component.translatable("gui.recipebook.toggleRecipes.craftable");
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, @NonNull RecipeDisplay recipeDisplay, @NonNull ContextMap contextMap) {
        ghostSlots.clear();
        if (!(recipeDisplay instanceof ShapelessCraftingRecipeDisplay shapeless)) {
            return;
        }

        List<net.minecraft.world.item.crafting.display.SlotDisplay> ingredients = shapeless.ingredients();
        List<net.minecraft.world.inventory.Slot> enchantingSlots = this.menu.vsq$getEnchantingSlots();
        int slotCount = Math.min(ingredients.size(), enchantingSlots.size());
        for (int index = 0; index < slotCount; index++) {
            ((GhostSlotsAccessor) ghostSlots).vsq$setInput(enchantingSlots.get(index), contextMap, ingredients.get(index));
        }

        OptionalInt displayId = EnchantingRecipeBookSyncPayloadHandler.findDisplayId(this.menu.containerId, recipeDisplay);
        if (displayId.isPresent()) {
            ClientPlayNetworking.send(new EnchantingRecipeSelectionPayload(this.menu.containerId, displayId.getAsInt()));
        }
    }

    @Override
    public void recipeShown(@NonNull RecipeDisplayId recipeDisplayId) {
        super.recipeShown(recipeDisplayId);
        ClientPlayNetworking.send(new EnchantingRecipeSelectionPayload(this.menu.containerId, recipeDisplayId.index()));
    }

    public void vsq$clearSelection() {
        ClientPlayNetworking.send(new EnchantingRecipeSelectionPayload(this.menu.containerId, -1));
    }

    private static ItemStack vsq$enchanted(net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }
}
