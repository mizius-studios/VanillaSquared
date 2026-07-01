package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.api.builder.general.WeaponAttributeBuilder;
import blob.vanillasquared.util.api.combat.VSQCombatPresets;
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
        WeaponAttributeBuilder trident = VSQCombatPresets.tridentAttributes();
        cir.setReturnValue(trident.build());
    }
}
