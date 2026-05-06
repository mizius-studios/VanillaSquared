package blob.vanillasquared.mixin.world.entity.entities;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.item.enchantment.effects.EnchantmentProjectileTakeoverEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingRodHookMixin extends Projectile {

    @Unique
    private static final float BASE_DAMAGE = 0.5F;
    @Unique
    private static final float BASE_KNOCKBACK = 0.4F;
    @Unique
    private static final ResourceKey<DamageType> FISHED_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "fished")
    );

    public FishingRodHookMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("TAIL"), method = "onHitEntity")
    private void onHitEntityTail(EntityHitResult hitResult, CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(this.getOwner() instanceof Player player)) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack fishingRod = resolveFishingRod(main, off);

        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity living)) {
            return;
        }

        DamageSource source = serverLevel.damageSources().source(FISHED_DAMAGE_TYPE, this, player);
        float damage = BASE_DAMAGE;
        damage = EnchantmentProjectileTakeoverEffects.modifyDamage(serverLevel, player, fishingRod, living, source, damage);

        living.hurtServer(serverLevel, source, damage);

        float kb = EnchantmentProjectileTakeoverEffects.modifyKnockback(serverLevel, player, fishingRod, living, source, BASE_KNOCKBACK);
        if (kb > 0) {
            double yaw = Math.toRadians(player.getYRot());

            living.push(-Math.sin(yaw) * kb, 0.1, Math.cos(yaw) * kb);

            living.hurtMarked = true;
        }

        EnchantmentProjectileTakeoverEffects.runPostAttackEffects(serverLevel, player, fishingRod, living, source);

        if (living instanceof Player playerTarget && resolveWeapon(main, off).getItem() instanceof AxeItem && playerTarget.isBlocking()) {
            ItemStack using = playerTarget.getUseItem();

            if (!using.isEmpty() && using.is(Items.SHIELD)) {
                EquipmentSlot slot = playerTarget.getUsedItemHand().asEquipmentSlot();

                using.hurtAndBreak(5, playerTarget, slot);
                playerTarget.getCooldowns().addCooldown(using, 100);
                playerTarget.stopUsingItem();

                serverLevel.playSound(
                        null,
                        playerTarget.getX(),
                        playerTarget.getY(),
                        playerTarget.getZ(),
                        SoundEvents.SHIELD_BREAK,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
        }
    }

    @Unique
    private static ItemStack resolveFishingRod(ItemStack main, ItemStack off) {
        if (main.getItem() instanceof FishingRodItem) {
            return main;
        }
        if (off.getItem() instanceof FishingRodItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    @Unique
    private static ItemStack resolveWeapon(ItemStack main, ItemStack off) {
        if (main.getItem() instanceof FishingRodItem) {
            return off;
        }
        if (off.getItem() instanceof FishingRodItem) {
            return main;
        }
        return main;
    }
}
