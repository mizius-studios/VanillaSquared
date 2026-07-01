package blob.vanillasquared.mixin.client.world.particle;

import blob.vanillasquared.main.world.particle.particles.LightningBoltParticleGroup;
import blob.vanillasquared.main.world.particle.VSQParticleRenderTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    @Final
    @Mutable
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void vsq$addLightningRenderType(CallbackInfo ci) {
        List<ParticleRenderType> updated = new ArrayList<>(RENDER_ORDER);
        if (!updated.contains(VSQParticleRenderTypes.LIGHTNING_BOLT)) {
            updated.add(VSQParticleRenderTypes.LIGHTNING_BOLT);
        }
        // Keep this list mutable because Fabric sorts render order during client init.
        RENDER_ORDER = updated;
    }

    @Inject(method = "createParticleGroup", at = @At("HEAD"), cancellable = true)
    private void vsq$createLightningParticleGroup(ParticleRenderType type, CallbackInfoReturnable<ParticleGroup<?>> cir) {
        if (type == VSQParticleRenderTypes.LIGHTNING_BOLT) {
            cir.setReturnValue(new LightningBoltParticleGroup((ParticleEngine) (Object) this));
        }
    }
}
