package blob.vanillasquared.mixin.world.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("attackStrengthTicker")
    void vsq$setAttackStrengthTicker(int ticks);

    @Invoker("detectEquipmentUpdates")
    void vsq$detectEquipmentUpdates();
}
