package blob.vanillasquared.mixin;

import blob.vanillasquared.util.modules.attributes.RegisterAttributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float vsq$applyAttributeProtections(float amount, ServerLevel level, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Mace Protection
        Entity attacker = source.getDirectEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            if (livingAttacker.getMainHandItem().is(Items.MACE)) {
                double protection = entity.getAttributeValue(RegisterAttributes.maceProtection);
                protection = Math.max(0.0, Math.min(1.0, protection));
                return Math.max(amount * (1.0F - (float) protection), 0.0F);
            }
        }

        return Math.max(amount, 0.0F);
    }
}
