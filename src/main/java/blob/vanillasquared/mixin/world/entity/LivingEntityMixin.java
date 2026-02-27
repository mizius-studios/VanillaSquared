package blob.vanillasquared.mixin.world.entity;

import blob.vanillasquared.util.combat.DamageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float vsq$applyAttributeProtections(float amount, ServerLevel level, DamageSource source) {
        return DamageUtil.applyCustomProtections((LivingEntity) (Object) this, source, amount);
    }
}
