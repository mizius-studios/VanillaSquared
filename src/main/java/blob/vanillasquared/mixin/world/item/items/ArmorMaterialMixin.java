package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.api.builder.general.ArmorAttributeBuilder;
import blob.vanillasquared.util.api.combat.VSQCombatPresets;
import blob.vanillasquared.util.api.references.armor.Armor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorMaterial.class)
public class ArmorMaterialMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private void vsq$replaceNetheriteArmor(ArmorType type, CallbackInfoReturnable<ItemAttributeModifiers> cir) {
        Armor.find((ArmorMaterial) (Object) this, type).ifPresent(armor -> {
            ArmorAttributeBuilder generalArmor = VSQCombatPresets.armorAttributes(armor);
            if (generalArmor != null) {
                cir.setReturnValue(generalArmor.build());
            }
        });
    }
}
