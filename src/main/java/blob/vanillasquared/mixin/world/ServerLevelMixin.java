package blob.vanillasquared.mixin.world;

import blob.vanillasquared.main.world.effect.ChannelingState;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void vsq$tickChanneling(BooleanSupplier haveTime, CallbackInfo ci) {
        ChannelingState.tick((ServerLevel) (Object) this);
    }
}
