package blob.vanillasquared.mixin.world.entity.entities;

import blob.vanillasquared.main.world.util.DamageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float vsq$applyCustomProtections(float amount, ServerLevel level, DamageSource source) {
        return DamageUtil.applyCustomProtections((Player) (Object) this, source, amount);
    }
}
