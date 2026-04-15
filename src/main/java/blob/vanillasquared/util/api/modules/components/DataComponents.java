package blob.vanillasquared.util.api.modules.components;

import blob.vanillasquared.main.world.item.components.dualwield.DualWieldComponent;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import blob.vanillasquared.main.world.item.components.specialeffect.SpecialEffectComponent;
import net.minecraft.core.component.DataComponentType;

public final class DataComponents {
    public static final DataComponentType<VSQEnchantmentComponent> VSQ_ENCHANTMENT = RegisterComponents.enchantmentComponent;
    public static final DataComponentType<DualWieldComponent> DUAL_WIELD = RegisterComponents.dualWieldComponent;
    public static final DataComponentType<HitThroughComponent> HIT_THROUGH = RegisterComponents.hitThroughComponent;
    public static final DataComponentType<SpecialEffectComponent> SPECIAL_EFFECT = RegisterComponents.specialEffectComponent;

    private DataComponents() {
    }
}
