package blob.vanillasquared.util.modules.components;

import blob.vanillasquared.VanillaSquared;
import blob.vanillasquared.util.combat.components.dualwield.DualWieldComponent;
import blob.vanillasquared.util.combat.components.hitthrough.HitThroughComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class RegisterComponents {

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

    private RegisterComponents() {
    }

    public static void initialize() {
    }
}
