package blob.vanillasquared.mixin.world.entity.entities;

import blob.vanillasquared.util.combat.DamageUtil;
import blob.vanillasquared.util.combat.components.dualwield.DualWieldPlayerData;
import blob.vanillasquared.util.combat.components.dualwield.DualWieldUtil;
import blob.vanillasquared.util.combat.components.dualwield.DualWieldComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin implements DualWieldPlayerData {

    @Unique
    private ItemEnchantments vsq$originalMainEnchantments = ItemEnchantments.EMPTY;
    @Unique
    private boolean vsq$hadMainEnchantments;
    @Unique
    private boolean vsq$dualEnchantSwapActive;
    @Unique
    private ItemStack vsq$enchantSwapStack = ItemStack.EMPTY;
    @Unique
    private int vsq$dualWieldCritCharges;
    @Unique
    private float vsq$queuedDualExtraDamage;
    @Unique
    private boolean vsq$queuedDualCriticalFeedback;

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float vsq$applyMaceProtection(float amount, ServerLevel level, DamageSource source) {
        return DamageUtil.applyCustomProtections((Player) (Object) this, source, amount);
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void vsq$applyMergedEnchantments(Entity target, CallbackInfo ci) {
        this.vsq$queuedDualExtraDamage = 0.0F;
        this.vsq$queuedDualCriticalFeedback = false;

        Player player = (Player) (Object) this;
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        var active = DualWieldUtil.getActiveDualWield(mainHand, offHand);
        if (active.isEmpty()) {
            return;
        }

        this.vsq$originalMainEnchantments = mainHand.getEnchantments();
        this.vsq$hadMainEnchantments = mainHand.has(DataComponents.ENCHANTMENTS);
        this.vsq$enchantSwapStack = mainHand;
        this.vsq$dualEnchantSwapActive = true;
        mainHand.set(DataComponents.ENCHANTMENTS, DualWieldUtil.mergeEnchantments(mainHand, offHand, active.get()));

        if (!(target instanceof LivingEntity)) {
            return;
        }
        if (player.getAttackStrengthScale(0.5F) <= 0.9F) {
            return;
        }

        DualWieldComponent offHandComponent = active.get().offHand();
        float offHandAttackDamage = DualWieldUtil.getItemAttackDamage(offHand);
        if (offHandAttackDamage <= 0.0F) {
            return;
        }

        boolean criticalBuffActive = this.vsq$dualWieldCritCharges > 0;
        float extraHitDamage = DualWieldUtil.calculateExtraSweepDamage(offHandAttackDamage, offHandComponent, criticalBuffActive);
        if (extraHitDamage <= 0.0F) {
            return;
        }

        this.vsq$queuedDualExtraDamage = extraHitDamage;
        this.vsq$queuedDualCriticalFeedback = criticalBuffActive;
    }

    @ModifyArg(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            index = 1
    )
    private float vsq$addDualExtraDamageToMainHit(float damage) {
        return damage + this.vsq$queuedDualExtraDamage;
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void vsq$restoreOriginalEnchantments(Entity target, CallbackInfo ci) {
        if (!this.vsq$dualEnchantSwapActive || this.vsq$enchantSwapStack.isEmpty()) {
            this.vsq$resetEnchantSwapState();
            return;
        }

        if (this.vsq$hadMainEnchantments) {
            this.vsq$enchantSwapStack.set(DataComponents.ENCHANTMENTS, this.vsq$originalMainEnchantments);
        } else {
            this.vsq$enchantSwapStack.remove(DataComponents.ENCHANTMENTS);
        }

        this.vsq$resetEnchantSwapState();
    }

    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void vsq$playDualWieldFeedback(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(target instanceof LivingEntity primaryTarget)) {
            return;
        }
        if (this.vsq$queuedDualExtraDamage <= 0.0F) {
            return;
        }

        if (this.vsq$queuedDualCriticalFeedback) {
            DualWieldUtil.playCriticalEffects(player, primaryTarget);
            this.vsq$consumeDualWieldCritCharge();
        } else {
            DualWieldUtil.spawnSweepEffects(serverLevel, player);
        }
    }

    @Unique
    private void vsq$resetEnchantSwapState() {
        this.vsq$originalMainEnchantments = ItemEnchantments.EMPTY;
        this.vsq$hadMainEnchantments = false;
        this.vsq$dualEnchantSwapActive = false;
        this.vsq$enchantSwapStack = ItemStack.EMPTY;
        this.vsq$queuedDualExtraDamage = 0.0F;
        this.vsq$queuedDualCriticalFeedback = false;
    }

    @Override
    public int vsq$getDualWieldCritCharges() {
        return this.vsq$dualWieldCritCharges;
    }

    @Override
    public void vsq$setDualWieldCritCharges(int charges) {
        this.vsq$dualWieldCritCharges = Math.max(0, charges);
    }

    @Override
    public boolean vsq$consumeDualWieldCritCharge() {
        if (this.vsq$dualWieldCritCharges <= 0) {
            return false;
        }

        this.vsq$dualWieldCritCharges--;
        return true;
    }
}
