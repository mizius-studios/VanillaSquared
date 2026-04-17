package blob.vanillasquared.mixin.world.inventory;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {
    @Invoker("fromJson")
    static RecipeHolder<?> vsq$fromJson(ResourceKey<Recipe<?>> recipeKey, JsonObject json, HolderLookup.Provider registries) {
        throw new AssertionError();
    }
}
