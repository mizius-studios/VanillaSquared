package blob.vanillasquared.mixin;

import blob.vanillasquared.util.api.other.vsqIdentifiers;
import blob.vanillasquared.util.api.references.Armor;
import blob.vanillasquared.util.data.GeneralArmor;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ArmorMaterial.class)
public class ArmorMaterialMixin {

    @Unique
    private static final Map<Armor, GeneralArmor> ARMOR = Map.ofEntries(
        Map.entry(Armor.LEATHER_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 2.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.LEATHER_CHESTPLATE, new GeneralArmor(vsqIdentifiers.vsqArmorChestplateOverride.identifier(), EquipmentSlotGroup.CHEST, 3.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.LEATHER_LEGGINGS, new GeneralArmor(vsqIdentifiers.vsqArmorLeggingsOverride.identifier(), EquipmentSlotGroup.LEGS, 2.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.LEATHER_BOOTS, new GeneralArmor(vsqIdentifiers.vsqArmorBootsOverride.identifier(), EquipmentSlotGroup.FEET, 1.0d, 0.0d,0.0d,0.0d)),

        Map.entry(Armor.COPPER_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 2.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.COPPER_CHESTPLATE, new GeneralArmor(vsqIdentifiers.vsqArmorChestplateOverride.identifier(), EquipmentSlotGroup.CHEST, 4.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.COPPER_LEGGINGS, new GeneralArmor(vsqIdentifiers.vsqArmorLeggingsOverride.identifier(), EquipmentSlotGroup.LEGS, 3.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.COPPER_BOOTS, new GeneralArmor(vsqIdentifiers.vsqArmorBootsOverride.identifier(), EquipmentSlotGroup.FEET, 1.0d, 0.0d,0.0d,0.0d)),

        Map.entry(Armor.CHAINMAIL_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 3.0d, 0.0d,0.0d,0.2d)),
        Map.entry(Armor.CHAINMAIL_CHESTPLATE, new GeneralArmor(vsqIdentifiers.vsqArmorChestplateOverride.identifier(), EquipmentSlotGroup.CHEST, 5.0d, 0.0d,0.0d,0.2d)),
        Map.entry(Armor.CHAINMAIL_LEGGINGS, new GeneralArmor(vsqIdentifiers.vsqArmorLeggingsOverride.identifier(), EquipmentSlotGroup.LEGS, 4.0d, 0.0d,0.0d,0.2d)),
        Map.entry(Armor.CHAINMAIL_BOOTS, new GeneralArmor(vsqIdentifiers.vsqArmorBootsOverride.identifier(), EquipmentSlotGroup.FEET, 1.0d, 0.0d,0.0d,0.2d)),

        Map.entry(Armor.IRON_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 2.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.IRON_CHESTPLATE, new GeneralArmor(vsqIdentifiers.vsqArmorChestplateOverride.identifier(), EquipmentSlotGroup.CHEST, 6.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.IRON_LEGGINGS, new GeneralArmor(vsqIdentifiers.vsqArmorLeggingsOverride.identifier(), EquipmentSlotGroup.LEGS, 5.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.IRON_BOOTS, new GeneralArmor(vsqIdentifiers.vsqArmorBootsOverride.identifier(), EquipmentSlotGroup.FEET, 1.0d, 0.0d,0.0d,0.0d)),

        Map.entry(Armor.GOLD_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 2.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.GOLD_CHESTPLATE, new GeneralArmor(vsqIdentifiers.vsqArmorChestplateOverride.identifier(), EquipmentSlotGroup.CHEST, 6.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.GOLD_LEGGINGS, new GeneralArmor(vsqIdentifiers.vsqArmorLeggingsOverride.identifier(), EquipmentSlotGroup.LEGS, 5.0d, 0.0d,0.0d,0.0d)),
        Map.entry(Armor.GOLD_BOOTS, new GeneralArmor(vsqIdentifiers.vsqArmorBootsOverride.identifier(), EquipmentSlotGroup.FEET, 1.0d, 0.0d,0.0d,0.0d)),

        Map.entry(Armor.DIAMOND_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 4.0d, 2.0d,0.0d,0.0d)),
        Map.entry(Armor.DIAMOND_CHESTPLATE, new GeneralArmor(vsqIdentifiers.vsqArmorChestplateOverride.identifier(), EquipmentSlotGroup.CHEST, 7.0d, 2.0d,0.0d,0.0d)),
        Map.entry(Armor.DIAMOND_LEGGINGS, new GeneralArmor(vsqIdentifiers.vsqArmorLeggingsOverride.identifier(), EquipmentSlotGroup.LEGS, 6.0d, 2.0d,0.0d,0.0d)),
        Map.entry(Armor.DIAMOND_BOOTS, new GeneralArmor(vsqIdentifiers.vsqArmorBootsOverride.identifier(), EquipmentSlotGroup.FEET, 3.0d, 2.0d,0.0d,0.0d)),

        Map.entry(Armor.NETHERITE_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 5.0d, 3.0d,0.1d,0.0d)),
        Map.entry(Armor.NETHERITE_CHESTPLATE, new GeneralArmor(vsqIdentifiers.vsqArmorChestplateOverride.identifier(), EquipmentSlotGroup.CHEST, 8.0d, 3.0d,0.1d,0.0d)),
        Map.entry(Armor.NETHERITE_LEGGINGS, new GeneralArmor(vsqIdentifiers.vsqArmorLeggingsOverride.identifier(), EquipmentSlotGroup.LEGS, 7.0d, 3.0d,.0d,0.0d)),
        Map.entry(Armor.NETHERITE_BOOTS, new GeneralArmor(vsqIdentifiers.vsqArmorBootsOverride.identifier(), EquipmentSlotGroup.FEET, 4.0d, 3.0d,0.1d,0.0d)),
        Map.entry(Armor.TURTLE_HELMET, new GeneralArmor(vsqIdentifiers.vsqArmorHelmetOverride.identifier(), EquipmentSlotGroup.HEAD, 4.0d, 0.0d,0.0d,0.0d))
    );

    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private void vsq$replaceNetheriteArmor(ArmorType armorType, CallbackInfoReturnable<ItemAttributeModifiers> cir) {
        Armor.find((ArmorMaterial) (Object) this, armorType).ifPresent(armor -> {
            GeneralArmor generalArmor = ARMOR.get(armor);
            if (generalArmor != null) {
                cir.setReturnValue(generalArmor.build());
            }
        });
    }
}
