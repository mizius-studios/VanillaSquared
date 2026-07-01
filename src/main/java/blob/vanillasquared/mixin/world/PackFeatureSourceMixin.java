package blob.vanillasquared.mixin.world;

import blob.vanillasquared.main.world.VSQExperiments;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pack.class)
public abstract class PackFeatureSourceMixin {

    @Inject(method = "getPackSource", at = @At("RETURN"), cancellable = true)
    private void vsq$markBuiltinExperimentPacks(CallbackInfoReturnable<PackSource> cir) {
        Pack self = (Pack) (Object) this;
        if (VSQExperiments.isBuiltinPackId(self.getId())) {
            cir.setReturnValue(PackSource.FEATURE);
        }
    }
}
