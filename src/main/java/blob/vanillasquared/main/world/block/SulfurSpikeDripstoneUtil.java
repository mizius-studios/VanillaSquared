package blob.vanillasquared.main.world.block;

import blob.vanillasquared.mixin.world.block.AbstractCauldronBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public final class SulfurSpikeDripstoneUtil {
    public static final float WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.17578125F;
    public static final float LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.05859375F;
    public static final float COPPER_OXIDATION_PROBABILITY_PER_RANDOM_TICK = 0.84F;
    private static final int MAX_SEARCH_LENGTH = 11;
    private static final double STALACTITE_DRIP_START_PIXEL = 0.6875D;
    private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.column(6.0D, 0.0D, 16.0D);

    private SulfurSpikeDripstoneUtil() {
    }

    public static void maybeTransferFluid(BlockState state, ServerLevel level, BlockPos pos, float chance) {
        if (chance > WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK
                && chance > LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK
                && chance > COPPER_OXIDATION_PROBABILITY_PER_RANDOM_TICK) {
            return;
        }
        if (!isStalactiteStartPos(state, level, pos)) {
            return;
        }

        Optional<FluidInfo> fluidInfo = getFluidAboveStalactite(level, pos, state);
        if (fluidInfo.isEmpty()) {
            return;
        }

        Fluid fluid = fluidInfo.get().fluid();
        float transferProbability;
        if (fluid == Fluids.WATER) {
            transferProbability = WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK;
        } else if (fluid == Fluids.LAVA) {
            transferProbability = LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK;
        } else {
            return;
        }
        boolean canTransferFluid = chance < transferProbability;
        boolean canOxidizeCopper = fluid == Fluids.WATER && chance < COPPER_OXIDATION_PROBABILITY_PER_RANDOM_TICK;
        if (!canTransferFluid && !canOxidizeCopper) {
            return;
        }

        BlockPos tipPos = findTip(level, pos, state);
        if (tipPos == null) {
            return;
        }

        if (canOxidizeCopper && oxidizeCopperBelowStalactiteTip(level, tipPos)) {
            level.levelEvent(1504, tipPos, 0);
            return;
        }

        if (!canTransferFluid) {
            return;
        }

        BlockPos cauldronPos = findFillableCauldronBelowStalactiteTip(level, tipPos, fluid);
        if (cauldronPos == null) {
            return;
        }

        level.levelEvent(1504, tipPos, 0);
        int delay = 50 + tipPos.getY() - cauldronPos.getY();
        BlockState cauldronState = level.getBlockState(cauldronPos);
        level.scheduleTick(cauldronPos, cauldronState.getBlock(), delay);
    }

    public static void spawnDripParticle(Level level, BlockPos pos, BlockState state) {
        getFluidAboveStalactite(level, pos, state).ifPresent(fluidInfo -> spawnDripParticle(level, pos, state, fluidInfo.fluid(), fluidInfo.pos()));
    }

    public static BlockPos findStalactiteTipAboveCauldron(Level level, BlockPos cauldronPos) {
        BlockPos.MutableBlockPos mutable = cauldronPos.mutable();
        for (int i = 1; i <= MAX_SEARCH_LENGTH; i++) {
            mutable.move(Direction.UP);
            BlockState state = level.getBlockState(mutable);
            if (isSulfurSpikeTipDown(state)) {
                return mutable.immutable();
            }
            if (!canDripThrough(level, mutable, state)) {
                return null;
            }
        }
        return null;
    }

    public static Fluid getCauldronFillFluidType(ServerLevel level, BlockPos tipPos) {
        return getFluidAboveStalactite(level, tipPos, level.getBlockState(tipPos))
                .map(FluidInfo::fluid)
                .filter(SulfurSpikeDripstoneUtil::canFillCauldron)
                .orElse(Fluids.EMPTY);
    }

    private static Optional<FluidInfo> getFluidAboveStalactite(Level level, BlockPos pos, BlockState state) {
        if (!isStalactite(state)) {
            return Optional.empty();
        }
        return findRootBlock(level, pos, state).flatMap(rootPos -> {
            BlockPos sourcePos = rootPos.above();
            BlockState sourceState = level.getBlockState(sourcePos);
            FluidState fluidState = level.getFluidState(sourcePos);
            if (fluidState.isSource()) {
                return Optional.of(new FluidInfo(sourcePos, fluidState.getType(), sourceState));
            }

            BlockPos fluidPos = sourcePos.above();
            FluidState fluidAboveSourceBlock = level.getFluidState(fluidPos);
            if (fluidAboveSourceBlock.isSource()) {
                return Optional.of(new FluidInfo(sourcePos, fluidAboveSourceBlock.getType(), sourceState));
            }

            return Optional.empty();
        });
    }

    private static Optional<BlockPos> findRootBlock(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(SpeleothemBlock.TIP_DIRECTION);
        BlockPos.MutableBlockPos mutable = pos.mutable();
        BlockPos last = pos;
        for (int i = 0; i < MAX_SEARCH_LENGTH; i++) {
            mutable.move(direction.getOpposite());
            BlockState nextState = level.getBlockState(mutable);
            if (!isSameSulfurSpikeDirection(nextState, direction)) {
                return Optional.of(last);
            }
            last = mutable.immutable();
        }
        return Optional.empty();
    }

    private static BlockPos findTip(LevelAccessor level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(SpeleothemBlock.TIP_DIRECTION);
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < MAX_SEARCH_LENGTH; i++) {
            BlockState currentState = level.getBlockState(mutable);
            if (!isSameSulfurSpikeDirection(currentState, direction)) {
                return null;
            }
            SpeleothemThickness thickness = currentState.getValue(SpeleothemBlock.THICKNESS);
            if (thickness == SpeleothemThickness.TIP || thickness == SpeleothemThickness.TIP_MERGE) {
                return mutable.immutable();
            }
            mutable.move(direction);
        }
        return null;
    }

    private static BlockPos findFillableCauldronBelowStalactiteTip(Level level, BlockPos tipPos, Fluid fluid) {
        BlockPos.MutableBlockPos mutable = tipPos.mutable();
        for (int i = 1; i <= MAX_SEARCH_LENGTH; i++) {
            mutable.move(Direction.DOWN);
            BlockState state = level.getBlockState(mutable);
            if (state.getBlock() instanceof AbstractCauldronBlock cauldronBlock
                    && ((AbstractCauldronBlockAccessor) cauldronBlock).vanillasquared$canReceiveStalactiteDrip(fluid)) {
                return mutable.immutable();
            }
            if (!canDripThrough(level, mutable, state)) {
                return null;
            }
        }
        return null;
    }

    private static boolean oxidizeCopperBelowStalactiteTip(ServerLevel level, BlockPos tipPos) {
        BlockPos.MutableBlockPos mutable = tipPos.mutable();
        for (int i = 1; i <= MAX_SEARCH_LENGTH; i++) {
            mutable.move(Direction.DOWN);
            BlockState state = level.getBlockState(mutable);
            Optional<BlockState> oxidized = WeatheringCopper.getNext(state.getBlock())
                    .map(block -> block.withPropertiesOf(state));
            if (oxidized.isPresent()) {
                BlockState oxidizedState = copyProperties(state, oxidized.get());
                BlockPos oxidizedPos = mutable.immutable();
                level.setBlockAndUpdate(oxidizedPos, oxidizedState);
                Block.pushEntitiesUp(state, oxidizedState, level, oxidizedPos);
                level.gameEvent(GameEvent.BLOCK_CHANGE, oxidizedPos, GameEvent.Context.of(oxidizedState));
                return true;
            }
            if (!canDripThrough(level, mutable, state)) {
                return false;
            }
        }
        return false;
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copyProperty(from, result, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    private static void spawnDripParticle(Level level, BlockPos pos, BlockState state, Fluid fluid, BlockPos sourcePos) {
        if (!canFillCauldron(fluid)) {
            return;
        }

        Vec3 offset = state.getOffset(pos);
        double x = pos.getX() + 0.5D + offset.x;
        double y = pos.getY() + STALACTITE_DRIP_START_PIXEL - 0.0625D;
        double z = pos.getZ() + 0.5D + offset.z;
        ParticleOptions particle = fluid == Fluids.LAVA ? ParticleTypes.DRIPPING_DRIPSTONE_LAVA : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
        level.addParticle(particle, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private static boolean isStalactiteStartPos(BlockState state, Level level, BlockPos pos) {
        return isStalactite(state) && !isSameSulfurSpikeDirection(level.getBlockState(pos.above()), Direction.DOWN);
    }

    private static boolean isStalactite(BlockState state) {
        return isSameSulfurSpikeDirection(state, Direction.DOWN);
    }

    private static boolean isSulfurSpikeTipDown(BlockState state) {
        return isStalactite(state) && (state.getValue(SpeleothemBlock.THICKNESS) == SpeleothemThickness.TIP || state.getValue(SpeleothemBlock.THICKNESS) == SpeleothemThickness.TIP_MERGE);
    }

    private static boolean isSameSulfurSpikeDirection(BlockState state, Direction direction) {
        return state.is(Blocks.SULFUR_SPIKE) && state.getValue(SpeleothemBlock.TIP_DIRECTION) == direction;
    }

    private static boolean canFillCauldron(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.LAVA;
    }

    private static boolean canDripThrough(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.isSolidRender()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, shape, BooleanOp.AND);
    }

    public record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
    }
}
