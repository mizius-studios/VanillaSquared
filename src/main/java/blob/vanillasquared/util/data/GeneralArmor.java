package blob.vanillasquared.util.data;

import blob.vanillasquared.util.modules.attributes.RegisterAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Unique;

public class GeneralArmor {

    public static ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
    @Unique
    private static void vsq$armorModifier(ItemAttributeModifiers.Builder builder, Identifier id, double value, EquipmentSlotGroup slotGroup) {
        builder.add(Attributes.ARMOR, new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE), slotGroup);
    }

    @Unique
    private static void vsq$armorToughnessModifier(ItemAttributeModifiers.Builder builder, Identifier id, double value, EquipmentSlotGroup slotGroup) {
        builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE), slotGroup);
    }

    @Unique
    private static void vsq$armorKnockbackResistanceModifier(ItemAttributeModifiers.Builder builder, Identifier id, double value, EquipmentSlotGroup slotGroup) {
        builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE), slotGroup);
    }

    @Unique
    private static void vsq$maceProtectionModifier(ItemAttributeModifiers.Builder builder, Identifier id, double value, EquipmentSlotGroup slotGroup) {
        builder.add(RegisterAttributes.maceProtection, new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE), slotGroup);
    }

    public GeneralArmor(Identifier identifier, EquipmentSlotGroup slotGroup, double attributeArmor, double attributeArmorToughness, double attributeKnockbackResistance, double attributeMaceProtection) {
        vsq$armorModifier(builder, identifier, attributeArmor, slotGroup);
        vsq$armorToughnessModifier(builder, identifier, attributeArmorToughness, slotGroup);
        vsq$armorKnockbackResistanceModifier(builder, identifier, attributeKnockbackResistance, slotGroup);
        vsq$maceProtectionModifier(builder, identifier, attributeMaceProtection, slotGroup);
    }
}
