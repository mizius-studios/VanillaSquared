package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.List;

@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {
    @Inject(method = "loadRecipes", at = @At("HEAD"), cancellable = true)
    private void vsq$ignoreCustomEnchantingRecipeBookEntries(
            List<ResourceKey<Recipe<?>>> recipes,
            Consumer<ResourceKey<Recipe<?>>> recipeAddingMethod,
            Predicate<ResourceKey<Recipe<?>>> validator,
            CallbackInfo ci
    ) {
        for (ResourceKey<Recipe<?>> recipe : recipes) {
            if (validator.test(recipe)) {
                recipeAddingMethod.accept(recipe);
                continue;
            }
            if (EnchantingRecipeRegistry.contains(recipe)) {
                recipeAddingMethod.accept(recipe);
            }
        }
        ci.cancel();
    }
}
