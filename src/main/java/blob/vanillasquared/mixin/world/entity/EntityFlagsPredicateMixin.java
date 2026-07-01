package blob.vanillasquared.mixin.world.entity;

import blob.vanillasquared.main.world.effect.LungingState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(EntityFlagsPredicate.class)
public abstract class EntityFlagsPredicateMixin {
    @Shadow
    @Final
    @Mutable
    public static Codec<EntityFlagsPredicate> CODEC;

    @Shadow
    public abstract Optional<Boolean> isOnGround();

    @Shadow
    public abstract Optional<Boolean> isOnFire();

    @Shadow
    public abstract Optional<Boolean> isCrouching();

    @Shadow
    public abstract Optional<Boolean> isSprinting();

    @Shadow
    public abstract Optional<Boolean> isSwimming();

    @Shadow
    public abstract Optional<Boolean> isFlying();

    @Shadow
    public abstract Optional<Boolean> isBaby();

    @Shadow
    public abstract Optional<Boolean> isInWater();

    @Shadow
    public abstract Optional<Boolean> isFallFlying();

    @Unique
    private static final ThreadLocal<Optional<Boolean>> VSQ_DECODE_IS_LUNGING = ThreadLocal.withInitial(Optional::empty);

    @Unique
    private Optional<Boolean> vsq$isLunging = Optional.empty();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vsq$rememberIsLunging(
            Optional<Boolean> isOnGround,
            Optional<Boolean> isOnFire,
            Optional<Boolean> isCrouching,
            Optional<Boolean> isSprinting,
            Optional<Boolean> isSwimming,
            Optional<Boolean> isFlying,
            Optional<Boolean> isBaby,
            Optional<Boolean> isInWater,
            Optional<Boolean> isFallFlying,
            CallbackInfo ci
    ) {
        this.vsq$isLunging = VSQ_DECODE_IS_LUNGING.get();
        VSQ_DECODE_IS_LUNGING.remove();
    }

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void vsq$checkLunging(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || this.vsq$isLunging.isEmpty()) {
            return;
        }

        boolean isLunging = entity instanceof LivingEntity living && LungingState.isLunging(living);
        if (isLunging != this.vsq$isLunging.orElse(false)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void vsq$extendCodec(CallbackInfo ci) {
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("is_on_ground").forGetter(EntityFlagsPredicate::isOnGround),
                Codec.BOOL.optionalFieldOf("is_on_fire").forGetter(EntityFlagsPredicate::isOnFire),
                Codec.BOOL.optionalFieldOf("is_sneaking").forGetter(EntityFlagsPredicate::isCrouching),
                Codec.BOOL.optionalFieldOf("is_sprinting").forGetter(EntityFlagsPredicate::isSprinting),
                Codec.BOOL.optionalFieldOf("is_swimming").forGetter(EntityFlagsPredicate::isSwimming),
                Codec.BOOL.optionalFieldOf("is_flying").forGetter(EntityFlagsPredicate::isFlying),
                Codec.BOOL.optionalFieldOf("is_baby").forGetter(EntityFlagsPredicate::isBaby),
                Codec.BOOL.optionalFieldOf("is_in_water").forGetter(EntityFlagsPredicate::isInWater),
                Codec.BOOL.optionalFieldOf("is_fall_flying").forGetter(EntityFlagsPredicate::isFallFlying),
                Codec.BOOL.optionalFieldOf("is_lunging").forGetter(EntityFlagsPredicateMixin::vsq$isLungingForCodec)
        ).apply(instance, EntityFlagsPredicateMixin::vsq$decode));
    }

    @Unique
    private static EntityFlagsPredicate vsq$decode(
            Optional<Boolean> isOnGround,
            Optional<Boolean> isOnFire,
            Optional<Boolean> isCrouching,
            Optional<Boolean> isSprinting,
            Optional<Boolean> isSwimming,
            Optional<Boolean> isFlying,
            Optional<Boolean> isBaby,
            Optional<Boolean> isInWater,
            Optional<Boolean> isFallFlying,
            Optional<Boolean> isLunging
    ) {
        VSQ_DECODE_IS_LUNGING.set(isLunging);
        return new EntityFlagsPredicate(
                isOnGround,
                isOnFire,
                isCrouching,
                isSprinting,
                isSwimming,
                isFlying,
                isBaby,
                isInWater,
                isFallFlying
        );
    }

    @Unique
    private static Optional<Boolean> vsq$isLungingForCodec(EntityFlagsPredicate predicate) {
        return ((EntityFlagsPredicateMixin) (Object) predicate).vsq$isLunging;
    }
}
