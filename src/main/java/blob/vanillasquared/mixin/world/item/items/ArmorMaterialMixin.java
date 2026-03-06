package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.api.references.armor.Armor;
import blob.vanillasquared.util.api.builder.general.GeneralArmor;
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
        Map.entry(Armor.LEATHER_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.LEATHER_CHESTPLATE, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorChestplateOverride, EquipmentSlotGroup.CHEST, 3.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.LEATHER_LEGGINGS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorLeggingsOverride, EquipmentSlotGroup.LEGS, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.LEATHER_BOOTS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorBootsOverride, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),

        Map.entry(Armor.COPPER_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.COPPER_CHESTPLATE, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorChestplateOverride, EquipmentSlotGroup.CHEST, 4.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.COPPER_LEGGINGS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorLeggingsOverride, EquipmentSlotGroup.LEGS, 3.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.COPPER_BOOTS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorBootsOverride, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),

        Map.entry(Armor.CHAINMAIL_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 3.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.2D, 0.2D)),
        Map.entry(Armor.CHAINMAIL_CHESTPLATE, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorChestplateOverride, EquipmentSlotGroup.CHEST, 5.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D, 0.2D)),
        Map.entry(Armor.CHAINMAIL_LEGGINGS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorLeggingsOverride, EquipmentSlotGroup.LEGS, 4.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.2D, 0.2D)),
        Map.entry(Armor.CHAINMAIL_BOOTS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorBootsOverride, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.2D, 0.2D)),

        Map.entry(Armor.IRON_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.IRON_CHESTPLATE, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorChestplateOverride, EquipmentSlotGroup.CHEST, 6.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.IRON_LEGGINGS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorLeggingsOverride, EquipmentSlotGroup.LEGS, 5.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.IRON_BOOTS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorBootsOverride, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),

        Map.entry(Armor.GOLD_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),
        Map.entry(Armor.GOLD_CHESTPLATE, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorChestplateOverride, EquipmentSlotGroup.CHEST, 6.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),
        Map.entry(Armor.GOLD_LEGGINGS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorLeggingsOverride, EquipmentSlotGroup.LEGS, 5.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),
        Map.entry(Armor.GOLD_BOOTS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorBootsOverride, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),

        Map.entry(Armor.DIAMOND_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 4.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.DIAMOND_CHESTPLATE, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorChestplateOverride, EquipmentSlotGroup.CHEST, 7.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.DIAMOND_LEGGINGS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorLeggingsOverride, EquipmentSlotGroup.LEGS, 6.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.DIAMOND_BOOTS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorBootsOverride, EquipmentSlotGroup.FEET, 3.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),

        Map.entry(Armor.NETHERITE_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 5.0D, 3.0D, 0.1D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.NETHERITE_CHESTPLATE, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorChestplateOverride, EquipmentSlotGroup.CHEST, 8.0D, 3.0D, 0.1D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.NETHERITE_LEGGINGS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorLeggingsOverride, EquipmentSlotGroup.LEGS, 7.0D, 3.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.NETHERITE_BOOTS, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorBootsOverride, EquipmentSlotGroup.FEET, 4.0D, 3.0D, 0.1D, 0.0D, 0.0D, 0.0D, 0.0D)),
        Map.entry(Armor.TURTLE_HELMET, new GeneralArmor(GeneralArmor.UtilIdentifiers.armorHelmetOverride, EquipmentSlotGroup.HEAD, 4.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D))
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
