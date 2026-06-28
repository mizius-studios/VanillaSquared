package blob.vanillasquared.mixin.world.block;

import blob.vanillasquared.main.world.block.SulfurSpikeDripstoneUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCauldronBlock.class)
public abstract class AbstractCauldronBlockMixin extends Block {
    public AbstractCauldronBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void vanillasquared$receiveSulfurSpikeDrip(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos) != null) {
            return;
        }

        BlockPos sulfurSpikeTipPos = SulfurSpikeDripstoneUtil.findStalactiteTipAboveCauldron(level, pos);
        if (sulfurSpikeTipPos == null) {
            ci.cancel();
            return;
        }

        Fluid fluid = SulfurSpikeDripstoneUtil.getCauldronFillFluidType(level, sulfurSpikeTipPos);
        if (fluid != Fluids.EMPTY && ((AbstractCauldronBlockAccessor) this).vanillasquared$canReceiveStalactiteDrip(fluid)) {
            ((AbstractCauldronBlockAccessor) this).vanillasquared$receiveStalactiteDrip(state, (Level) level, pos, fluid);
        }
        ci.cancel();
    }
}
