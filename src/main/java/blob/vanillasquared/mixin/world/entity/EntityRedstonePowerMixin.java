package blob.vanillasquared.mixin.world.entity;

import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePower;
import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePowerAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRedstonePowerMixin implements VSQEntityRedstonePowerAccess {
    @Unique
    private AABB vsq$previousRedstoneSourceBounds;
    @Unique
    private int vsq$previousRedstonePower;
    @Unique
    private int vsq$redstonePower;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vsq$initEntityRedstonePower(EntityType<?> entityType, Level level, CallbackInfo ci) {
        this.vsq$previousRedstoneSourceBounds = null;
        this.vsq$previousRedstonePower = 0;
        this.vsq$redstonePower = 0;
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void vsq$loadEntityRedstonePower(ValueInput input, CallbackInfo ci) {
        this.vsq$redstonePower = Mth.clamp(input.getIntOr(VSQEntityRedstonePower.POWER_REDSTONE_KEY, 0), 0, 15);
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void vsq$saveEntityRedstonePower(ValueOutput output, CallbackInfo ci) {
        if (this.vsq$redstonePower > 0) {
            output.putInt(VSQEntityRedstonePower.POWER_REDSTONE_KEY, this.vsq$redstonePower);
        }
    }

    @Override
    public int vsq$getRedstonePower() {
        return this.vsq$redstonePower;
    }

    @Override
    public void vsq$setRedstonePower(int power) {
        this.vsq$redstonePower = Mth.clamp(power, 0, 15);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void vsq$tickEntityRedstonePower(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        AABB currentBounds = entity.getBoundingBox();
        int currentPower = VSQEntityRedstonePower.getPower(entity);
        boolean boundsChanged = this.vsq$previousRedstoneSourceBounds != null
                && !this.vsq$previousRedstoneSourceBounds.equals(currentBounds);

        if (this.vsq$previousRedstonePower > 0 && this.vsq$previousRedstoneSourceBounds != null
                && (currentPower <= 0 || boundsChanged)) {
            VSQEntityRedstonePower.updateNeighbors(level, this.vsq$previousRedstoneSourceBounds);
        }

        if (currentPower > 0) {
            if (entity.tickCount % 2 == 0) {
                VSQEntityRedstonePower.updateNeighbors(level, currentBounds);
            }
            this.vsq$previousRedstoneSourceBounds = currentBounds;
        } else {
            this.vsq$previousRedstoneSourceBounds = null;
        }
        this.vsq$previousRedstonePower = currentPower;
    }

    @Inject(method = "onRemoval", at = @At("TAIL"))
    private void vsq$removeEntityRedstonePower(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (this.vsq$previousRedstonePower > 0 && this.vsq$previousRedstoneSourceBounds != null) {
            VSQEntityRedstonePower.updateNeighbors(level, this.vsq$previousRedstoneSourceBounds);
        } else if (VSQEntityRedstonePower.getPower(entity) > 0) {
            VSQEntityRedstonePower.updateNeighbors(level, entity.getBoundingBox());
        }

        this.vsq$previousRedstoneSourceBounds = null;
        this.vsq$previousRedstonePower = 0;
    }
}
