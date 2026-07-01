package blob.vanillasquared.util.api.modules.components;

import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public final class VSQItemComponents {
    private VSQItemComponents() {
    }

    public static boolean hasEnchantmentComponent(ItemStack stack) {
        return stack.has(VSQDataComponents.ENCHANTMENT);
    }

    public static VSQEnchantmentComponent getEnchantmentComponent(ItemStack stack) {
        return stack.get(VSQDataComponents.ENCHANTMENT);
    }

    public static void setEnchantmentComponent(ItemStack stack, VSQEnchantmentComponent component) {
        stack.set(VSQDataComponents.ENCHANTMENT, component);
    }

    public static void removeEnchantmentComponent(ItemStack stack) {
        stack.remove(VSQDataComponents.ENCHANTMENT);
    }

    public static HitThroughComponent getHitThroughComponent(ItemStack stack) {
        return stack.get(VSQDataComponents.HIT_THROUGH);
    }

    public static ResourceKey<Recipe<?>> getEnchantRecipe(ItemStack stack) {
        return stack.get(VSQDataComponents.ENCHANT_RECIPE);
    }

    public static void setEnchantRecipe(ItemStack stack, ResourceKey<Recipe<?>> recipeKey) {
        stack.set(VSQDataComponents.ENCHANT_RECIPE, recipeKey);
    }
}
