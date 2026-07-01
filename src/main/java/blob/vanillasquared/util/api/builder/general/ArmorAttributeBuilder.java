package blob.vanillasquared.util.api.builder.general;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.util.api.modules.attributes.VSQAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class ArmorAttributeBuilder {
    private final ItemAttributeModifiers modifiers;

    public ArmorAttributeBuilder(
            ModifierIds ids,
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
            builder.add(Attributes.ARMOR, new AttributeModifier(ids.get(ModifierType.ARMOR), armor, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (armorToughness != 0) {
            builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(ids.get(ModifierType.ARMOR_TOUGHNESS), armorToughness, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (knockbackResistance != 0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(ids.get(ModifierType.KNOCKBACK_RESISTANCE), knockbackResistance, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (maceProtection != 0) {
            builder.add(VSQAttributes.MACE_PROTECTION, new AttributeModifier(ids.get(ModifierType.MACE_PROTECTION), maceProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (magicProtection != 0) {
            builder.add(VSQAttributes.MAGIC_PROTECTION, new AttributeModifier(ids.get(ModifierType.MAGIC_PROTECTION), magicProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (dripstoneProtection != 0) {
            builder.add(VSQAttributes.DRIPSTONE_PROTECTION, new AttributeModifier(ids.get(ModifierType.DRIPSTONE_PROTECTION), dripstoneProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (spearProtection != 0) {
            builder.add(VSQAttributes.SPEAR_PROTECTION, new AttributeModifier(ids.get(ModifierType.SPEAR_PROTECTION), spearProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        this.modifiers = builder.build();
    }

    public ItemAttributeModifiers build() {
        return modifiers;
    }

    public enum ModifierIds {
        HELMET(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_armor"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_armor_toughness"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_knockback_resistance"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_mace_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_magic_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_dripstone_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "helmet_spear_protection")
        ),
        CHESTPLATE(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_armor"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_armor_toughness"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_knockback_resistance"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_mace_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_magic_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_dripstone_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "chestplate_spear_protection")
        ),
        LEGGINGS(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_armor"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_armor_toughness"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_knockback_resistance"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_mace_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_magic_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_dripstone_protection"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "leggings_spear_protection")
        ),
        BOOTS(
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

        ModifierIds(
                Identifier armor,
                Identifier armorToughness,
                Identifier knockbackResistance,
                Identifier maceProtection,
                Identifier magicProtection,
                Identifier dripstoneProtection,
                Identifier spearProtection
        ) {
            this.armor = armor;
            this.armorToughness = armorToughness;
            this.knockbackResistance = knockbackResistance;
            this.maceProtection = maceProtection;
            this.magicProtection = magicProtection;
            this.dripstoneProtection = dripstoneProtection;
            this.spearProtection = spearProtection;
        }

        public Identifier get(ModifierType type) {
            return switch (type) {
                case ARMOR -> armor;
                case ARMOR_TOUGHNESS -> armorToughness;
                case KNOCKBACK_RESISTANCE -> knockbackResistance;
                case MACE_PROTECTION -> maceProtection;
                case MAGIC_PROTECTION -> magicProtection;
                case DRIPSTONE_PROTECTION -> dripstoneProtection;
                case SPEAR_PROTECTION -> spearProtection;
            };
        }

        public boolean is(AttributeModifier modifier) {
            return modifier.is(armor)
                    || modifier.is(armorToughness)
                    || modifier.is(knockbackResistance)
                    || modifier.is(maceProtection)
                    || modifier.is(magicProtection)
                    || modifier.is(dripstoneProtection)
                    || modifier.is(spearProtection);
        }

        public boolean isItem(AttributeModifier modifier, ItemType item) {
            return switch (item) {
                case HELMET -> HELMET.is(modifier);
                case CHESTPLATE -> CHESTPLATE.is(modifier);
                case LEGGINGS -> LEGGINGS.is(modifier);
                case BOOTS -> BOOTS.is(modifier);
            };
        }
    }

    public enum ModifierType {
        ARMOR,
        ARMOR_TOUGHNESS,
        KNOCKBACK_RESISTANCE,
        MACE_PROTECTION,
        MAGIC_PROTECTION,
        DRIPSTONE_PROTECTION,
        SPEAR_PROTECTION
    }

    public enum ItemType {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }
}
