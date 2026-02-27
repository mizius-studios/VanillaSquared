package blob.vanillasquared.mixin.world.entity;

import blob.vanillasquared.util.modules.attributes.RegisterAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAttributesMixin {

    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void vsq$addCustomAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(RegisterAttributes.maceProtectionAttribute);
        cir.getReturnValue().add(RegisterAttributes.magicProtectionAttribute);
        cir.getReturnValue().add(RegisterAttributes.dripstoneProtectionAttribute);
        cir.getReturnValue().add(RegisterAttributes.spearProtectionAttribute);
    }
}
