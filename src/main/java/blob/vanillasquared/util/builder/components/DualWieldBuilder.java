package blob.vanillasquared.util.builder.components;

import blob.vanillasquared.util.combat.components.dualwield.DualWieldComponent;
import net.minecraft.resources.Identifier;

import java.util.List;

public class DualWieldBuilder {
    private final DualWieldComponent dualWieldComponent;

    public DualWieldBuilder(
            List<String> identifiers,
            int cooldown,
            int criticalHits,
            Identifier blockedEnchantmentsTag,
            int sweepingDamage,
            int criticalDamage
    ) {
        this.dualWieldComponent = new DualWieldComponent(
                identifiers,
                cooldown,
                criticalHits,
                blockedEnchantmentsTag,
                sweepingDamage,
                criticalDamage
        );
    }

    public DualWieldBuilder(
            List<String> identifiers,
            int cooldown,
            int criticalHits,
            int sweepingDamage,
            int criticalDamage,
            String blockedEnchantmentsTagNamespace,
            String blockedEnchantmentsTagPath
    ) {
        this(
                identifiers,
                cooldown,
                criticalHits,
                Identifier.fromNamespaceAndPath(blockedEnchantmentsTagNamespace, blockedEnchantmentsTagPath),
                sweepingDamage,
                criticalDamage
        );
    }

    public DualWieldComponent build() {
        return dualWieldComponent;
    }
}
