package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.enchantment.EnchantingOverlayRecipeButton;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(OverlayRecipeComponent.class)
public abstract class OverlayRecipeComponentMixin {
    @Shadow
    @Final
    private List<?> recipeButtons;

    @Inject(method = "init", at = @At("RETURN"))
    private void vsq$prepareEnchantingRecipeButtons(RecipeCollection collection, ContextMap contextMap,
                                                    boolean filtering, int x, int y, int maxX, int maxY,
                                                    float step, CallbackInfo ci) {
        List<RecipeDisplayEntry> craftable = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
        List<RecipeDisplayEntry> unavailable = filtering
                ? List.of()
                : collection.getSelectedRecipes(RecipeCollection.CraftableStatus.NOT_CRAFTABLE);

        for (int index = 0; index < this.recipeButtons.size(); index++) {
            RecipeDisplayEntry entry = index < craftable.size()
                    ? craftable.get(index)
                    : unavailable.get(index - craftable.size());
            ((EnchantingOverlayRecipeButton) this.recipeButtons.get(index))
                    .vsq$setEnchantingDisplay(entry.display(), contextMap);
        }
    }
}
