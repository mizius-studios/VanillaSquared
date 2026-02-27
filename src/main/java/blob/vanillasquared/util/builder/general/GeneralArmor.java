package blob.vanillasquared.util.builder.general;

import blob.vanillasquared.util.modules.attributes.RegisterAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class GeneralArmor {

    private final ItemAttributeModifiers modifiers;

    public GeneralArmor(
            Identifier identifier,
            EquipmentSlotGroup slotGroup,
            double armor,
            double armorToughness,
            double knockbackResistance,
            double maceProtection
    ) {
        this(identifier, slotGroup, armor, armorToughness, knockbackResistance, maceProtection, 0.0D, 0.0D, 0.0D);
    }

    public GeneralArmor(
            Identifier identifier,
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
            builder.add(Attributes.ARMOR, new AttributeModifier(identifier, armor, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (armorToughness != 0) {
            builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(identifier, armorToughness, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (knockbackResistance != 0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(identifier, knockbackResistance, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (maceProtection != 0) {
            builder.add(RegisterAttributes.maceProtectionAttribute, new AttributeModifier(identifier, maceProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (magicProtection != 0) {
            builder.add(RegisterAttributes.magicProtectionAttribute, new AttributeModifier(identifier, magicProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (dripstoneProtection != 0) {
            builder.add(RegisterAttributes.dripstoneProtectionAttribute, new AttributeModifier(identifier, dripstoneProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (spearProtection != 0) {
            builder.add(RegisterAttributes.spearProtectionAttribute, new AttributeModifier(identifier, spearProtection, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }

        this.modifiers = builder.build();
    }

    public ItemAttributeModifiers build() {
        return modifiers;
    }
}
