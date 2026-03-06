package blob.vanillasquared.util.api.builder.components;

import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import net.minecraft.resources.Identifier;

public class HitThroughBuilder {
    private final HitThroughComponent hitThroughComponent;

    public HitThroughBuilder(Identifier tag) {
        this.hitThroughComponent = new HitThroughComponent(tag);
    }

    public HitThroughBuilder(String namespace, String path) {
        this(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public HitThroughComponent build() {
        return hitThroughComponent;
    }
}
