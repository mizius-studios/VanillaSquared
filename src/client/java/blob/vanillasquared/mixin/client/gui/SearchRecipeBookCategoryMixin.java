package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.world.recipe.enchanting.VSQEnchantmentRecipeBookCategories;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

@Mixin(SearchRecipeBookCategory.class)
public abstract class SearchRecipeBookCategoryMixin {
    @Inject(method = "includedCategories", at = @At("RETURN"), cancellable = true)
    private void vsq$appendEnchantingCategories(CallbackInfoReturnable<List<RecipeBookCategory>> cir) {
        if ((Object) this != SearchRecipeBookCategory.CRAFTING) {
            return;
        }

        List<RecipeBookCategory> categories = new ArrayList<>(cir.getReturnValue());
        categories.add(VSQEnchantmentRecipeBookCategories.WEAPONS);
        categories.add(VSQEnchantmentRecipeBookCategories.TOOLS);
        categories.add(VSQEnchantmentRecipeBookCategories.ARMOR);
        categories.add(VSQEnchantmentRecipeBookCategories.UTIL);
        cir.setReturnValue(List.copyOf(categories));
    }
}
