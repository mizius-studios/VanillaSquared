package blob.vanillasquared.mixin.world.item.echanting;

import blob.vanillasquared.main.world.item.enchantment.effects.ApplyImpulseSpeedState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.ApplyEntityImpulse;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ApplyEntityImpulse.class)
public abstract class ApplyEntityImpulseMixin {
    @Shadow
    @Final
    @Mutable
    public static MapCodec<ApplyEntityImpulse> CODEC;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void vsq$applySpeedScaledImpulse(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position, CallbackInfo ci) {
        ApplyEntityImpulse self = (ApplyEntityImpulse) (Object) this;
        if (!ApplyImpulseSpeedState.hasCustomSpeed(self)) {
            return;
        }

        Vec3 look = entity.getLookAngle();
        double speed = ApplyImpulseSpeedState.speed(self);
        Vec3 direction = look.addLocalCoordinates(self.direction()).multiply(self.coordinateScale()).scale(self.magnitude().calculate(enchantmentLevel) * speed);
        entity.addDeltaMovement(direction);
        entity.hurtMarked = true;
        entity.needsSync = true;
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.applyPostImpulseGraceTime(10);
        }
        ci.cancel();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void vsq$extendCodec(CallbackInfo ci) {
        CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Vec3.CODEC.fieldOf("direction").forGetter(ApplyEntityImpulse::direction),
                Vec3.CODEC.fieldOf("coordinate_scale").forGetter(ApplyEntityImpulse::coordinateScale),
                LevelBasedValue.CODEC.fieldOf("magnitude").forGetter(ApplyEntityImpulse::magnitude),
                Codec.DOUBLE.optionalFieldOf("speed", 1.0D).forGetter(effect -> ApplyImpulseSpeedState.speed(effect))
        ).apply(instance, (direction, coordinateScale, magnitude, speed) ->
                ApplyImpulseSpeedState.remember(new ApplyEntityImpulse(direction, coordinateScale, magnitude), speed)
        ));
    }
}
