package blob.vanillasquared.util.api.modules.components;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.item.components.dualwield.DualWieldComponent;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import blob.vanillasquared.main.world.item.components.specialeffect.SpecialEffectComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class RegisterComponents {
    public static final DataComponentType<VSQEnchantmentComponent> enchantmentComponent = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "enchantment"),
            DataComponentType.<VSQEnchantmentComponent>builder()
                    .persistent(VSQEnchantmentComponent.CODEC.codec())
                    .networkSynchronized(VSQEnchantmentComponent.STREAM_CODEC)
                    .cacheEncoding()
                    .build()
    );

    public static final DataComponentType<DualWieldComponent> dualWieldComponent = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "dual_wield"),
            DataComponentType.<DualWieldComponent>builder()
                    .persistent(DualWieldComponent.CODEC)
                    .networkSynchronized(DualWieldComponent.STREAM_CODEC)
                    .cacheEncoding()
                    .build()
    );

    public static final DataComponentType<HitThroughComponent> hitThroughComponent = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "hit_through"),
            DataComponentType.<HitThroughComponent>builder()
                    .persistent(HitThroughComponent.CODEC)
                    .networkSynchronized(HitThroughComponent.STREAM_CODEC)
                    .cacheEncoding()
                    .build()
    );

    public static final DataComponentType<SpecialEffectComponent> specialEffectComponent = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "special_effect"),
            DataComponentType.<SpecialEffectComponent>builder()
                    .persistent(SpecialEffectComponent.CODEC)
                    .networkSynchronized(SpecialEffectComponent.STREAM_CODEC)
                    .cacheEncoding()
                    .build()
    );

    private RegisterComponents() {
    }

    public static void initialize() {
    }
}
