package blob.vanillasquared.main.world.recipe;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class VSQRecipeTypes {
    public static final RecipeType<EnchantingRecipe> ENCHANTING_TYPE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return VanillaSquared.MOD_ID + ":enchanting";
                }
            }
    );

    public static final RecipeSerializer<EnchantingRecipe> ENCHANTING_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchanting"),
            new RecipeSerializer<>(EnchantingRecipe.CODEC, EnchantingRecipe.STREAM_CODEC)
    );

    private VSQRecipeTypes() {
    }

    public static void initialize() {
    }
}
