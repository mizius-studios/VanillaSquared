package blob.vanillasquared.main.world.recipe.enchanting;

import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record EnchantingRecipeInput(
        ItemStack input,
        ItemStack material,
        List<ItemStack> ingredients
) implements RecipeInput {
    public static final EnchantingRecipeInput EMPTY = new EnchantingRecipeInput(
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)
    );

    public EnchantingRecipeInput {
        ingredients = List.copyOf(ingredients);
        if (ingredients.size() != 4) {
            throw new IllegalArgumentException("Enchanting recipes require exactly 4 cross ingredients");
        }
    }

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> this.input;
            case 1 -> this.material;
            case 2, 3, 4, 5 -> this.ingredients.get(index - 2);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 6;
    }

    @Override
    public boolean isEmpty() {
        if (!this.input.isEmpty() || !this.material.isEmpty()) {
            return false;
        }

        for (ItemStack ingredient : this.ingredients) {
            if (!ingredient.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
