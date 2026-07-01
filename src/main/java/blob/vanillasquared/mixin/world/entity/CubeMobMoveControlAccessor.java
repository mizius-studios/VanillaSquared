package blob.vanillasquared.mixin.world.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobMoveControl")
public interface CubeMobMoveControlAccessor {
    @Invoker("setDirection")
    void vsq$setDirection(float yRot, boolean aggressive);

    @Invoker("setWantedMovement")
    void vsq$setWantedMovement(double speedModifier);
}
