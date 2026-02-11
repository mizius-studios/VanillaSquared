package blob.vanillasquared.mixin;

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
    private static final Identifier VSQ_NETHERITE_CHESTPLATE_ARMOR_BONUS =
            Identifier.fromNamespaceAndPath("vanillasquared", "netherite_chestplate_armor_bonus");

    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private void vsq$addNetheriteChestplateArmorBonus(ArmorType armorType, CallbackInfoReturnable<ItemAttributeModifiers> cir) {
        if ((Object) this != ArmorMaterials.NETHERITE || armorType != ArmorType.CHESTPLATE) {
            return;
        }

        ItemAttributeModifiers updated = cir.getReturnValue().withModifierAdded(
                Attributes.ARMOR,
                new AttributeModifier(VSQ_NETHERITE_CHESTPLATE_ARMOR_BONUS, 1.0d, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.CHEST
        );
        cir.setReturnValue(updated);
    }
}
