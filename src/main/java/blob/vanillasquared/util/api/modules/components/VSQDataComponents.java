package blob.vanillasquared.util.api.modules.components;

import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

public final class VSQDataComponents {
    public static final DataComponentType<VSQEnchantmentComponent> ENCHANTMENT = RegisterComponents.enchantmentComponent;
    public static final DataComponentType<HitThroughComponent> HIT_THROUGH = RegisterComponents.hitThroughComponent;
    public static final DataComponentType<ResourceKey<Recipe<?>>> ENCHANT_RECIPE = RegisterComponents.enchantRecipeComponent;

    private VSQDataComponents() {
    }

    public static void initialize() {
        RegisterComponents.initialize();
    }
}
