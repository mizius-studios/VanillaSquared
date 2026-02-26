package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.api.other.vsqIdentifiers;
import blob.vanillasquared.util.builder.general.GeneralWeapon;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentItemMixin {
    @Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
    private static void createAttributes(CallbackInfoReturnable<ItemAttributeModifiers> cir) {
        GeneralWeapon trident = new GeneralWeapon(vsqIdentifiers.tridentOverride.identifier(), EquipmentSlotGroup.MAINHAND, 7.0F, -2.875F, 0.5F);
        cir.setReturnValue(trident.build());
    }
}
