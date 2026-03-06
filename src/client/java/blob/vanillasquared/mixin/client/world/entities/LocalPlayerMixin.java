package blob.vanillasquared.mixin.client.world.entities;

import blob.vanillasquared.main.world.item.components.hitthrough.HitThroughComponent;
import blob.vanillasquared.util.api.modules.components.DataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "raycastHitResult", at = @At("RETURN"), cancellable = true)
    private void vsq$allowHitThroughGrass(float tickDelta, Entity cameraEntity, CallbackInfoReturnable<HitResult> cir) {
        if (cameraEntity == null) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        HitThroughComponent hitThrough = player.getMainHandItem().get(DataComponents.HIT_THROUGH);
        if (hitThrough == null) {
            return;
        }

        HitResult currentHit = cir.getReturnValue();
        if (!(currentHit instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockState firstBlockState = player.level().getBlockState(blockHit.getBlockPos());
        if (!hitThrough.canHitThrough(firstBlockState)) {
            return;
        }

        Vec3 eyePosition = cameraEntity.getEyePosition(tickDelta);
        Vec3 viewVector = cameraEntity.getViewVector(tickDelta);
        double maxRange = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
        Vec3 rayEnd = eyePosition.add(viewVector.scale(maxRange));
        Vec3 collisionEnd = this.vsq$findFirstSolidCollision(player, cameraEntity, hitThrough, eyePosition, rayEnd, viewVector);

        double clipDistanceSquared = eyePosition.distanceToSqr(collisionEnd);
        if (clipDistanceSquared <= 0.0D) {
            return;
        }

        AABB searchBox = cameraEntity.getBoundingBox().expandTowards(viewVector.scale(maxRange)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                cameraEntity,
                eyePosition,
                collisionEnd,
                searchBox,
                EntitySelector.CAN_BE_PICKED,
                clipDistanceSquared
        );

        if (entityHit == null) {
            return;
        }

        if (!entityHit.getLocation().closerThan(eyePosition, player.entityInteractionRange())) {
            return;
        }

        cir.setReturnValue(entityHit);
    }

    @Unique
    private Vec3 vsq$findFirstSolidCollision(
            LocalPlayer player,
            Entity cameraEntity,
            HitThroughComponent hitThrough,
            Vec3 start,
            Vec3 end,
            Vec3 direction
    ) {
        Vec3 currentStart = start;

        for (int i = 0; i < 16; i++) {
            ClipContext context = new ClipContext(currentStart, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, cameraEntity);
            BlockHitResult blockHit = player.level().clip(context);

            if (blockHit.getType() != HitResult.Type.BLOCK) {
                return end;
            }

            BlockState blockState = player.level().getBlockState(blockHit.getBlockPos());
            if (!hitThrough.canHitThrough(blockState)) {
                return blockHit.getLocation();
            }

            currentStart = blockHit.getLocation().add(direction.scale(0.01D));
        }

        return end;
    }
}
