package blob.vanillasquared.util.api.builder.general;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.util.api.modules.attributes.RegisterAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class GeneralArmor {

    private final ItemAttributeModifiers modifiers;

    public GeneralArmor(
            UtilIdentifiers identifier,
            EquipmentSlotGroup slotGroup,
            double armor,
            double armorToughness,
            double knockbackResistance,
            double maceProtection,
            double magicProtection,
            double dripstoneProtection,
            double spearProtection
    ) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (armor != 0) {
            builder.add(Attributes.ARMOR, new AttributeModifier(identifier.get(UtilIdentifiers.Type.ARMOR), armor, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (armorToughness != 0) {
            builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(identifier.get(UtilIdentifiers.Type.ARMOR_TOUGHNESS), armorToughness, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (knockbackResistance != 0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(identifier.get(UtilIdentifiers.Type.KNOCKBACK_RESISTANCE), knockbackResistance, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (maceProtection != 0) {
            builder.add(RegisterAttributes.maceProtectionAttribute, new AttributeModifier(identifier.get(UtilIdentifiers.Type.MACE_PROTECTION), maceProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (magicProtection != 0) {
            builder.add(RegisterAttributes.magicProtectionAttribute, new AttributeModifier(identifier.get(UtilIdentifiers.Type.MAGIC_PROTECTION), magicProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (dripstoneProtection != 0) {
            builder.add(RegisterAttributes.dripstoneProtectionAttribute, new AttributeModifier(identifier.get(UtilIdentifiers.Type.DRIPSTONE_PROTECTION), dripstoneProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (spearProtection != 0) {
            builder.add(RegisterAttributes.spearProtectionAttribute, new AttributeModifier(identifier.get(UtilIdentifiers.Type.SPEAR_PROTECTION), spearProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }

        this.modifiers = builder.build();
    }
    public enum UtilIdentifiers {
        armorHelmetOverride(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_armor"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_armor_toughness"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_knockback_resistance"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_mace_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_magic_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_dripstone_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_spear_protection")
        ),
        armorChestplateOverride(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_armor"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_armor_toughness"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_knockback_resistance"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_mace_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_magic_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_dripstone_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_spear_protection")
        ),
        armorLeggingsOverride(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_armor"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_armor_toughness"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_knockback_resistance"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_mace_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_magic_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_dripstone_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_spear_protection")
        ),
        armorBootsOverride(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "boots_armor"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "boots_armor_toughness"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "boots_knockback_resistance"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "boots_mace_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "boots_magic_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "boots_dripstone_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "boots_spear_protection")
        );
        private final Identifier armor;
        private final Identifier armorToughness;
        private final Identifier knockbackResistance;
        private final Identifier maceProtection;
        private final Identifier magicProtection;
        private final Identifier dripstoneProtection;
        private final Identifier spearProtection;
        UtilIdentifiers(Identifier armor, Identifier armorToughness, Identifier knockbackResistance, Identifier maceProtection, Identifier magicProtection, Identifier dripstoneProtection, Identifier spearProtection) {
            this.armor = armor;
            this.armorToughness = armorToughness;
            this.knockbackResistance = knockbackResistance;
            this.maceProtection = maceProtection;
            this.magicProtection = magicProtection;
            this.dripstoneProtection = dripstoneProtection;
            this.spearProtection = spearProtection;
        }
        public Identifier get(Type type) {
            return switch (type) {
                case Type.ARMOR -> armor;
                case Type.ARMOR_TOUGHNESS -> armorToughness;
                case Type.KNOCKBACK_RESISTANCE -> knockbackResistance;
                case Type.MACE_PROTECTION -> maceProtection;
                case Type.MAGIC_PROTECTION -> magicProtection;
                case Type.DRIPSTONE_PROTECTION -> dripstoneProtection;
                case Type.SPEAR_PROTECTION -> spearProtection;
            };
        }

        public boolean is(AttributeModifier modifier) {
            return modifier.is(armor) || modifier.is(armorToughness) || modifier.is(knockbackResistance) || modifier.is(maceProtection) || modifier.is(magicProtection) || modifier.is(dripstoneProtection) || modifier.is(spearProtection);
        }

        public boolean isItem(AttributeModifier modifier, Item item) {
            return switch (item) {
                case Item.HELMET -> UtilIdentifiers.armorHelmetOverride.is(modifier);
                case Item.CHESTPLATE -> UtilIdentifiers.armorChestplateOverride.is(modifier);
                case Item.LEGGINGS -> UtilIdentifiers.armorLeggingsOverride.is(modifier);
                case Item.BOOTS -> UtilIdentifiers.armorBootsOverride.is(modifier);
            };
        }

        public enum Type {
            ARMOR,
            ARMOR_TOUGHNESS,
            KNOCKBACK_RESISTANCE,
            MACE_PROTECTION,
            MAGIC_PROTECTION,
            DRIPSTONE_PROTECTION,
            SPEAR_PROTECTION
        }
        public enum Item {
            HELMET,
            CHESTPLATE,
            LEGGINGS,
            BOOTS
        }

    }

    public ItemAttributeModifiers build() {
        return modifiers;
    }
}
