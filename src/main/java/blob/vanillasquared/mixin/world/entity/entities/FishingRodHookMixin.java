package blob.vanillasquared.mixin.world.entity.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

@Mixin(FishingHook.class)
public abstract class FishingRodHookMixin extends Projectile {

    @Unique
    private static final float BASE_DAMAGE = 0.5F;
    @Unique
    private static final float BASE_KNOCKBACK = 0.4F;

    public FishingRodHookMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("TAIL"), method = "onHitEntity")
    private void onHitEntityTail(EntityHitResult hit, CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(this.getOwner() instanceof Player player)) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();
        ItemStack weapon = main.getItem() instanceof FishingRodItem ? off : main;

        Entity target = hit.getEntity();
        if (!(target instanceof LivingEntity living)) {
            return;
        }

        DamageSource source = serverLevel.damageSources().playerAttack(player);
        float damage = BASE_DAMAGE;
        var enchants = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        int fire  = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.FIRE_ASPECT), weapon);
        int smite = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SMITE), weapon);
        int bane  = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), weapon);

        if (smite > 0 && living.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
            damage += smite * 2.5F;
        }

        if (bane > 0 && living.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD)) {
            damage += bane * 2.5F;

            int duration = 20 + serverLevel.getRandom().nextInt(10 * bane);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 3));
        }

        if (fire > 0) {
            living.igniteForSeconds(4 * fire);

            serverLevel.playSound(
                    null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS,
                    0.7F, 1.0F + serverLevel.getRandom().nextFloat() * 0.4F
            );
        }

        living.hurtServer(serverLevel, source, damage);

        float kb = EnchantmentHelper.modifyKnockback(serverLevel, weapon, living, source, BASE_KNOCKBACK);
        if (kb > 0) {
            double yaw = Math.toRadians(player.getYRot());

            living.push(-Math.sin(yaw) * kb, 0.1, Math.cos(yaw) * kb);

            living.hurtMarked = true;
        }

        int wind = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.WIND_BURST), weapon);
        if (wind > 0) {
            serverLevel.explode(
                    null,
                    null,
                    new SimpleExplosionDamageCalculator(false, false, Optional.of(1.0F * wind), Optional.empty()),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    1.2F + 0.35F * wind,
                    false,
                    Level.ExplosionInteraction.TRIGGER,
                    ParticleTypes.GUST_EMITTER_SMALL,
                    ParticleTypes.GUST_EMITTER_LARGE,
                    WeightedList.of(),
                    SoundEvents.WIND_CHARGE_BURST
            );
        }

        if (living instanceof Player playerTarget && weapon.getItem() instanceof AxeItem && playerTarget.isBlocking()) {
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
}
