package blob.vanillasquared.util.api.other;

import net.minecraft.resources.Identifier;

public enum vsqIdentifiers {
    vsqArmorChestplateOverride(Identifier.fromNamespaceAndPath("vanillasquared", "armor_chestplate_override")),
    vsqArmorLeggingsOverride(Identifier.fromNamespaceAndPath("vanillasquared", "armor_leggings_override")),
    vsqArmorBootsOverride(Identifier.fromNamespaceAndPath("vanillasquared", "armor_boots_override")),
    vsqArmorHelmetOverride(Identifier.fromNamespaceAndPath("vanillasquared", "armor_helmet_override")),
    vsqAxeReachOverride(Identifier.fromNamespaceAndPath("vanillasquared", "axe_reach")),
    vsqSwordReachOverride(Identifier.fromNamespaceAndPath("vanillasquared", "sword_reach"));

    private final Identifier identifier;

    vsqIdentifiers(Identifier identifier) {
        this.identifier = identifier;
    }
    public Identifier identifier() {
        return identifier;
    }
}
