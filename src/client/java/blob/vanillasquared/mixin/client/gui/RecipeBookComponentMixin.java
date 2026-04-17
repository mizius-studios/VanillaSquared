package blob.vanillasquared.mixin.client.gui;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import com.google.common.collect.Lists;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
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
    @Shadow @Final protected T menu;
    @Shadow protected Minecraft minecraft;
    @Shadow private ClientRecipeBook book;
    @Shadow private RecipeBookPage recipeBookPage;
    @Shadow private RecipeBookTabButton selectedTab;
    @Shadow private EditBox searchBox;

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
    private void vsq$updateEnchantingCollections(boolean resetCurrentPage, boolean filtering, CallbackInfo ci) {
        if (!(this.menu instanceof VSQEnchantmentMenu)) {
            return;
        }

        if (this.selectedTab == null) {
            return;
        }

        List<RecipeCollection> collections = Lists.newArrayList(this.book.getCollection(this.selectedTab.getCategory()));
        collections.removeIf(collection -> !collection.hasAnySelected());

        String query = this.searchBox.getValue().toLowerCase(Locale.ROOT);
        if (!query.isEmpty() && this.minecraft.level != null) {
            ContextMap contextMap = SlotDisplayContext.fromLevel(this.minecraft.level);
            collections.removeIf(collection -> !vsq$matchesSearch(collection, query, contextMap));
        }

        if (filtering) {
            collections.removeIf(collection -> !collection.hasCraftable());
        }

        this.recipeBookPage.updateCollections(collections, resetCurrentPage, filtering);
        ci.cancel();
    }

    private static boolean vsq$matchesSearch(RecipeCollection collection, String query, ContextMap contextMap) {
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            if (entry.resultItems(contextMap).stream().anyMatch(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))) {
                return true;
            }
        }
        return false;
    }
}
