package blob.vanillasquared.mixin.world.block;

import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractCauldronBlock.class)
public interface AbstractCauldronBlockAccessor {
    @Invoker("canReceiveStalactiteDrip")
    boolean vanillasquared$canReceiveStalactiteDrip(Fluid fluid);

    @Invoker("receiveStalactiteDrip")
    void vanillasquared$receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid);
}
