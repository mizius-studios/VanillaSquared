package blob.vanillasquared.mixin.world.entity.entities;

import blob.vanillasquared.main.world.effect.VoidedEffectState;
import blob.vanillasquared.main.world.util.DamageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, name = "dmg")
    private float vsq$applyCustomProtections(float dmg, ServerLevel level, DamageSource source) {
        return DamageUtil.applyCustomProtections((Player) (Object) this, source, dmg);
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void vsq$flushPendingVoidedRemoval(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        VoidedEffectState.flushPendingRemoval((Player) (Object) this);
    }
}
