package blob.vanillasquared.mixin.client.gui;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import blob.vanillasquared.main.world.recipe.enchanting.VSQEnchantmentRecipeBookCategories;
import com.google.common.collect.Lists;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Locale;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin<T extends RecipeBookMenu> {
    @Unique private static final int VSQ$RECIPE_BOOK_WIDTH = 147;
    @Unique private static final int VSQ$RECIPE_BOOK_HEIGHT = 166;
    @Unique private static final int VSQ$TAB_HEIGHT = 27;

    @Shadow @Final protected T menu;
    @Shadow protected Minecraft minecraft;
    @Shadow private ClientRecipeBook book;
    @Shadow private int xOffset;
    @Shadow private int width;
    @Shadow private int height;
    @Final
    @Shadow private RecipeBookPage recipeBookPage;
    @Shadow @Final
    private GhostSlots ghostSlots;
    @Shadow private RecipeBookTabButton selectedTab;
    @Shadow private EditBox searchBox;
    @Shadow @Final private List<RecipeBookTabButton> tabButtons;

    @Shadow
    protected abstract void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipeDisplay, ContextMap contextMap);

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void vsq$ignoreVerticalArrowsOnSearchBox(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.searchBox == null || !this.searchBox.isFocused() || !this.searchBox.isVisible()) {
            return;
        }

        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "updateCollections", at = @At("HEAD"), cancellable = true)
    private void vsq$updateEnchantingCollections(boolean resetPage, boolean isFiltering, CallbackInfo ci) {
        if (!(this.menu instanceof VSQEnchantmentMenu)) {
            return;
        }

        if (this.selectedTab == null) {
            return;
        }

        List<RecipeCollection> collections = this.selectedTab.getCategory() == VSQEnchantmentRecipeBookCategories.ALL
                ? vsq$allEnchantingCollections()
                : Lists.newArrayList(this.book.getCollection(this.selectedTab.getCategory()));
        collections.removeIf(collection -> !collection.hasAnySelected());

        String query = this.searchBox.getValue().toLowerCase(Locale.ROOT);
        if (!query.isEmpty() && this.minecraft.level != null) {
            ContextMap contextMap = SlotDisplayContext.fromLevel(this.minecraft.level);
            collections.removeIf(collection -> !vsq$matchesSearch(collection, query, contextMap));
        }

        if (isFiltering) {
            collections.removeIf(collection -> !collection.hasCraftable());
        }

        this.recipeBookPage.updateCollections(collections, resetPage, isFiltering);
        ci.cancel();
    }

    @Inject(method = "fillGhostRecipe(Lnet/minecraft/world/item/crafting/display/RecipeDisplay;)V", at = @At("HEAD"), cancellable = true)
    private void vsq$fillEnchantingGhostRecipe(RecipeDisplay recipe, CallbackInfo ci) {
        if (!(this.menu instanceof VSQEnchantmentMenu) || this.minecraft.level == null) {
            return;
        }

        this.fillGhostRecipe(this.ghostSlots, recipe, SlotDisplayContext.fromLevel(this.minecraft.level));
        ci.cancel();
    }

    @Inject(method = "updateTabs", at = @At("HEAD"), cancellable = true)
    private void vsq$updateEnchantingTabs(boolean isFiltering, CallbackInfo ci) {
        if (!(this.menu instanceof VSQEnchantmentMenu)) {
            return;
        }

        int left = (this.width - VSQ$RECIPE_BOOK_WIDTH) / 2 - this.xOffset - 30;
        int top = (this.height - VSQ$RECIPE_BOOK_HEIGHT) / 2 + 3;
        int visibleRow = 0;

        for (RecipeBookTabButton button : this.tabButtons) {
            boolean visible = (button.getCategory() == VSQEnchantmentRecipeBookCategories.ALL
                    ? vsq$hasAnyEnchantingCollections()
                    : button.updateVisibility(this.book));
            ((AbstractWidgetAccessor) button).vsq$setVisible(visible);
            if (!visible) {
                continue;
            }

            button.setPosition(left, top + VSQ$TAB_HEIGHT * visibleRow++);
            button.startAnimation(this.book, isFiltering);
        }
        ci.cancel();
    }

    @Unique
    private List<RecipeCollection> vsq$allEnchantingCollections() {
        List<RecipeCollection> collections = Lists.newArrayList();
        collections.addAll(this.book.getCollection(VSQEnchantmentRecipeBookCategories.WEAPONS));
        collections.addAll(this.book.getCollection(VSQEnchantmentRecipeBookCategories.TOOLS));
        collections.addAll(this.book.getCollection(VSQEnchantmentRecipeBookCategories.ARMOR));
        collections.addAll(this.book.getCollection(VSQEnchantmentRecipeBookCategories.UTIL));
        return collections;
    }

    @Unique
    private boolean vsq$hasAnyEnchantingCollections() {
        return vsq$allEnchantingCollections().stream().anyMatch(RecipeCollection::hasAnySelected);
    }

    @Unique
    private static boolean vsq$matchesSearch(RecipeCollection collection, String query, ContextMap contextMap) {
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            if (entry.resultItems(contextMap).stream().anyMatch(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))) {
                return true;
            }
        }
        return false;
    }
}
