package blob.vanillasquared.mixin.world.entity;

import blob.vanillasquared.main.world.effect.VSQMobEffects;
import blob.vanillasquared.main.world.effect.VoidedEffectState;
import blob.vanillasquared.main.world.util.DamageUtil;
import blob.vanillasquared.main.world.item.enchantment.effects.EnchantmentPostBlockEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private ItemStack vsq$blockingItem;
    @Unique
    private EquipmentSlot vsq$blockingSlot;

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, name = "dmg")
    private float vsq$applyAttributeProtections(float dmg, ServerLevel level, DamageSource source) {
        return DamageUtil.applyCustomProtections((LivingEntity) (Object) this, source, dmg);
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void vsq$flushPendingVoidedRemoval(ServerLevel level, DamageSource source, float dmg, CallbackInfo ci) {
        VoidedEffectState.flushPendingRemoval((LivingEntity) (Object) this);
    }

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void vsq$trackVoidedAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (effect.is(VSQMobEffects.VOIDED)) {
            VoidedEffectState.refresh((LivingEntity) (Object) this, effect);
        }
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void vsq$trackVoidedUpdated(MobEffectInstance effect, boolean doRefreshAttributes, Entity source, CallbackInfo ci) {
        if (effect.is(VSQMobEffects.VOIDED)) {
            VoidedEffectState.refresh((LivingEntity) (Object) this, effect);
        }
    }

    @Inject(method = "onEffectsRemoved", at = @At("TAIL"))
    private void vsq$clearVoidedRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        VoidedEffectState.clearRemoved((LivingEntity) (Object) this, effects);
    }

    @Inject(method = "tickEffects", at = @At("TAIL"))
    private void vsq$tickVoided(CallbackInfo ci) {
        VoidedEffectState.tick((LivingEntity) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("TAIL"))
    private void vsq$saveVoidedState(ValueOutput output, CallbackInfo ci) {
        VoidedEffectState.writeToNbt((LivingEntity) (Object) this, output);
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("HEAD"))
    private void vsq$beginLoadVoidedState(ValueInput input, CallbackInfo ci) {
        VoidedEffectState.beginLoadFromNbt((LivingEntity) (Object) this);
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"))
    private void vsq$loadVoidedState(ValueInput input, CallbackInfo ci) {
        VoidedEffectState.readFromNbt((LivingEntity) (Object) this, input);
        VoidedEffectState.endLoadFromNbt((LivingEntity) (Object) this);
    }

    @Inject(
            method = "applyItemBlocking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V"
            )
    )
    private void vsq$captureBlockingItem(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        vsq$blockingItem = ((LivingEntity) (Object) this).getItemBlockingWith();
        vsq$blockingSlot = ((LivingEntity) (Object) this).getUsedItemHand().asEquipmentSlot();
    }

    @Inject(
            method = "applyItemBlocking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void vsq$applyPostBlockEffects(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        ItemStack sourceItem = source.getEntity() instanceof LivingEntity attacker ? attacker.getWeaponItem() : null;
        EnchantmentPostBlockEffects.run(level, (LivingEntity) (Object) this, source, sourceItem, vsq$blockingItem, vsq$blockingSlot);
        vsq$blockingItem = null;
        vsq$blockingSlot = null;
    }
}
