package blob.vanillasquared.util.api.builder.components;

import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import blob.vanillasquared.util.api.references.RegistryReference;
import net.minecraft.resources.Identifier;

public final class HitThroughComponentBuilder {
    private final HitThroughComponent component;

    public HitThroughComponentBuilder(Identifier tag) {
        this(RegistryReference.tag(tag));
    }

    public HitThroughComponentBuilder(RegistryReference block) {
        this.component = new HitThroughComponent(block);
    }

    public HitThroughComponentBuilder(String namespace, String path) {
        this(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public HitThroughComponent build() {
        return component;
    }
}
