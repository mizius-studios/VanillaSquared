package blob.vanillasquared.mixin.client.world.entity;

import blob.vanillasquared.main.sound.VoidedSoundController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tickEffects", at = @At("TAIL"))
    private void vsq$tickVoidedSounds(CallbackInfo ci) {
        VoidedSoundController.tickEntity((LivingEntity) (Object) this);
    }
}
