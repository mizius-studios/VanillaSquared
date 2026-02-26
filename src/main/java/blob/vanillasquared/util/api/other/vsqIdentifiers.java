package blob.vanillasquared.util.api.other;

import net.minecraft.resources.Identifier;

public enum vsqIdentifiers {
    armorChestplateOverride(Identifier.fromNamespaceAndPath("vanilla-squared", "armor_chestplate_override")),
    armorLeggingsOverride(Identifier.fromNamespaceAndPath("vanilla-squared", "armor_leggings_override")),
    armorBootsOverride(Identifier.fromNamespaceAndPath("vanilla-squared", "armor_boots_override")),
    armorHelmetOverride(Identifier.fromNamespaceAndPath("vanilla-squared", "armor_helmet_override")),
    swordOverride(Identifier.fromNamespaceAndPath("vanilla-squared", "sword_override")),
    axeOverride(Identifier.fromNamespaceAndPath("vanilla-squared", "axe_override")),
    tridentOverride(Identifier.fromNamespaceAndPath("vanilla-squared", "trident_override"));

    private final Identifier identifier;

    vsqIdentifiers(Identifier identifier) {
        this.identifier = identifier;
    }

    public Identifier identifier() {
        return identifier;
    }
}
