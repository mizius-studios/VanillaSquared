package blob.vanillasquared.mixin.world.block;

import blob.vanillasquared.main.world.block.SulfurSpikeDripstoneUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.SulfurSpikeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SulfurSpikeBlock.class)
public abstract class SulfurSpikeBlockMixin extends SpeleothemBlock {
    public SulfurSpikeBlockMixin(BlockState blockState, BlockBehaviour.Properties properties) {
        super(blockState, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isFreeHangingStalactite(state)) {
            return;
        }

        float chance = random.nextFloat();
        if (chance > 0.12F) {
            return;
        }

        SulfurSpikeDripstoneUtil.spawnDripParticle(level, pos, state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SulfurSpikeDripstoneUtil.maybeTransferFluid(state, level, pos, random.nextFloat());
        super.randomTick(state, level, pos, random);
    }
}
