package blob.vanillasquared.mixin;

import blob.vanillasquared.util.modules.attributes.RegisterAttributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorMaterial.class)
public class ArmorMaterialMixin {

    @Unique
    private static final Identifier vsqArmorChestplateOverride = Identifier.fromNamespaceAndPath("vanillasquared", "armor_chestplate_override");
    @Unique
    private static final Identifier vsqArmorLeggingsOverride = Identifier.fromNamespaceAndPath("vanillasquared", "armor_leggings_override");
    @Unique
    private static final Identifier vsqArmorBootsOverride = Identifier.fromNamespaceAndPath("vanillasquared", "armor_boots_override");
    @Unique
    private static final Identifier vsqArmorHelmetOverride = Identifier.fromNamespaceAndPath("vanillasquared", "armor_helmet_override");

    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private void vsq$replaceNetheriteArmor(ArmorType armorType, CallbackInfoReturnable<ItemAttributeModifiers> cir) {
        if (ArmorMaterials.NETHERITE.equals(this)) {
            switch (armorType) {
                case CHESTPLATE -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorChestplateOverride, 8.0d, EquipmentSlotGroup.CHEST);
                    vsq$armorToughnessModifier(builder, vsqArmorChestplateOverride, 3.0d, EquipmentSlotGroup.CHEST);
                    vsq$armorKnockbackResistanceModifier(builder, vsqArmorChestplateOverride, 0.1d, EquipmentSlotGroup.CHEST);
                    cir.setReturnValue(builder.build());
                }
                case LEGGINGS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorLeggingsOverride, 7.0d, EquipmentSlotGroup.LEGS);
                    vsq$armorToughnessModifier(builder, vsqArmorLeggingsOverride, 3.0d, EquipmentSlotGroup.LEGS);
                    vsq$armorKnockbackResistanceModifier(builder, vsqArmorLeggingsOverride, 0.1d, EquipmentSlotGroup.LEGS);
                    cir.setReturnValue(builder.build());
                }
                case BOOTS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorBootsOverride, 4.0d, EquipmentSlotGroup.FEET);
                    vsq$armorToughnessModifier(builder, vsqArmorBootsOverride, 3.0d, EquipmentSlotGroup.FEET);
                    vsq$armorKnockbackResistanceModifier(builder, vsqArmorBootsOverride, 0.1d, EquipmentSlotGroup.FEET);
                    cir.setReturnValue(builder.build());
                }
                case HELMET -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorHelmetOverride, 5.0d, EquipmentSlotGroup.HEAD);
                    vsq$armorToughnessModifier(builder, vsqArmorHelmetOverride, 3.0d, EquipmentSlotGroup.HEAD);
                    vsq$armorKnockbackResistanceModifier(builder, vsqArmorHelmetOverride, 0.1d, EquipmentSlotGroup.HEAD);
                    cir.setReturnValue(builder.build());
                }
                default -> {}
            }
        } else if (ArmorMaterials.DIAMOND.equals(this)) {
            switch (armorType) {
                case CHESTPLATE -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorChestplateOverride, 7.0d, EquipmentSlotGroup.CHEST);
                    vsq$armorToughnessModifier(builder, vsqArmorChestplateOverride, 2.0d, EquipmentSlotGroup.CHEST);
                    cir.setReturnValue(builder.build());
                }
                case LEGGINGS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorLeggingsOverride, 6.0d, EquipmentSlotGroup.LEGS);
                    vsq$armorToughnessModifier(builder, vsqArmorLeggingsOverride, 2.0d, EquipmentSlotGroup.LEGS);
                    cir.setReturnValue(builder.build());
                }
                case BOOTS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorBootsOverride, 3.0d, EquipmentSlotGroup.FEET);
                    vsq$armorToughnessModifier(builder, vsqArmorBootsOverride, 2.0d, EquipmentSlotGroup.FEET);
                    cir.setReturnValue(builder.build());
                }
                case HELMET -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorHelmetOverride, 4.0d, EquipmentSlotGroup.HEAD);
                    vsq$armorToughnessModifier(builder, vsqArmorHelmetOverride, 2.0d, EquipmentSlotGroup.HEAD);
                    cir.setReturnValue(builder.build());
                }
                default -> {}
            }
        } else if (ArmorMaterials.IRON.equals(this)) {
            switch (armorType) {
                case CHESTPLATE -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorChestplateOverride, 6.0d, EquipmentSlotGroup.CHEST);
                    cir.setReturnValue(builder.build());
                }
                case LEGGINGS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorLeggingsOverride, 5.0d, EquipmentSlotGroup.LEGS);
                    cir.setReturnValue(builder.build());
                }
                case BOOTS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorBootsOverride, 1.0d, EquipmentSlotGroup.FEET);
                    cir.setReturnValue(builder.build());
                }
                case HELMET -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorHelmetOverride, 2.0d, EquipmentSlotGroup.HEAD);
                    cir.setReturnValue(builder.build());
                }
                default -> {}
            }
        } else if (ArmorMaterials.GOLD.equals(this)) {
            switch (armorType) {
                case CHESTPLATE -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorChestplateOverride, 6.0d, EquipmentSlotGroup.CHEST);
                    cir.setReturnValue(builder.build());
                }
                case LEGGINGS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorLeggingsOverride, 5.0d, EquipmentSlotGroup.LEGS);
                    cir.setReturnValue(builder.build());
                }
                case BOOTS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorBootsOverride, 1.0d, EquipmentSlotGroup.FEET);
                    cir.setReturnValue(builder.build());
                }
                case HELMET -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorHelmetOverride, 2.0d, EquipmentSlotGroup.HEAD);
                    cir.setReturnValue(builder.build());
                }
                default -> {}
            }
        } else if (ArmorMaterials.CHAINMAIL.equals(this)) {
            switch (armorType) {
                case CHESTPLATE -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorChestplateOverride, 5.0d, EquipmentSlotGroup.CHEST);
                    vsq$maceProtectionModifier(builder, vsqArmorChestplateOverride, 0.2d, EquipmentSlotGroup.CHEST);
                    cir.setReturnValue(builder.build());
                }
                case LEGGINGS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorLeggingsOverride, 5.0d, EquipmentSlotGroup.LEGS);
                    vsq$maceProtectionModifier(builder, vsqArmorLeggingsOverride, 0.2d, EquipmentSlotGroup.LEGS);
                    cir.setReturnValue(builder.build());
                }
                case BOOTS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorBootsOverride, 1.0d, EquipmentSlotGroup.FEET);
                    vsq$maceProtectionModifier(builder, vsqArmorBootsOverride, 0.2d, EquipmentSlotGroup.FEET);
                    cir.setReturnValue(builder.build());
                }
                case HELMET -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorHelmetOverride, 2.0d, EquipmentSlotGroup.HEAD);
                    vsq$maceProtectionModifier(builder, vsqArmorHelmetOverride, 0.2d, EquipmentSlotGroup.HEAD);
                    cir.setReturnValue(builder.build());
                }
                default -> {}
            }
        } else if (ArmorMaterials.COPPER.equals(this)) {
            switch (armorType) {
                case CHESTPLATE -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorChestplateOverride, 4.0d, EquipmentSlotGroup.CHEST);
                    cir.setReturnValue(builder.build());
                }
                case LEGGINGS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorLeggingsOverride, 3.0d, EquipmentSlotGroup.LEGS);
                    cir.setReturnValue(builder.build());
                }
                case BOOTS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorBootsOverride, 1.0d, EquipmentSlotGroup.FEET);
                    cir.setReturnValue(builder.build());
                }
                case HELMET -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorHelmetOverride, 2.0d, EquipmentSlotGroup.HEAD);
                    cir.setReturnValue(builder.build());
                }
                default -> {}
            }
        } else if (ArmorMaterials.LEATHER.equals(this)) {
            switch (armorType) {
                case CHESTPLATE -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorChestplateOverride, 3.0d, EquipmentSlotGroup.CHEST);
                    cir.setReturnValue(builder.build());
                }
                case LEGGINGS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorLeggingsOverride, 2.0d, EquipmentSlotGroup.LEGS);
                    cir.setReturnValue(builder.build());
                }
                case BOOTS -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorBootsOverride, 1.0d, EquipmentSlotGroup.FEET);
                    cir.setReturnValue(builder.build());
                }
                case HELMET -> {
                    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                    vsq$armorModifier(builder, vsqArmorHelmetOverride, 2.0d, EquipmentSlotGroup.HEAD);
                    cir.setReturnValue(builder.build());
                }
                default -> {}
            }
        } else if (ArmorMaterials.TURTLE_SCUTE.equals(this)) {
                ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                vsq$armorModifier(builder, vsqArmorHelmetOverride, 4.0d, EquipmentSlotGroup.HEAD);
                cir.setReturnValue(builder.build());
            }
        }

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

}
