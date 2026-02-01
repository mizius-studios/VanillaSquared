package blob.vanillasquared.mixin;

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
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Debug(export = true)
@Mixin(FishingHook.class)
public abstract class FishingRodHookMixin extends Projectile {

    public FishingRodHookMixin(EntityType<? extends Projectile> entityType, Level level) {super(entityType, level);} // needed for extends

    // @Inject starts

    @Inject(at = @At("TAIL"), method = "onHitEntity")
    private void init(EntityHitResult hit, CallbackInfo ci) {


        // <-- Get Values & Check Values
        if (!(this.level() instanceof ServerLevel serverLevel)) return; // check for serverLevel
        if (!(this.getOwner() instanceof Player player)) return; // check if the owner is a player

        ItemStack main = player.getMainHandItem(); // get offhand and mainhand item
        ItemStack off  = player.getOffhandItem();
        ItemStack weapon = main.getItem() instanceof FishingRodItem ? off : main;

        Entity target = hit.getEntity(); // get the entity hit by the fishing rod
        if (!(target instanceof LivingEntity living)) return; // check if its a LivingEntity


        // <-- Generate dmg -->
        DamageSource source = serverLevel.damageSources().playerAttack(player); // dmg source

        float damage = 0.5F;

        var enchants = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT); // accessing enchants registry

        int fire  = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.FIRE_ASPECT), weapon);
        int smite = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SMITE), weapon);
        int bane  = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), weapon);

        // <- Smite ->
        if (smite > 0 && living.getType().is(EntityTypeTags.UNDEAD)) {
            damage += smite * 2.5F;
        }

        // <- Bane of Arthropods ->
        if (bane > 0 && living.getType().is(EntityTypeTags.ARTHROPOD)) {
            damage += bane * 2.5F;

            int duration = 20 + serverLevel.random.nextInt(10 * bane);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 3));
        }

        // <- Fire Aspect ->
        if (fire > 0) {
            living.igniteForSeconds(4 * fire);

            serverLevel.playSound(
                    null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS,
                    0.7F, 1.0F + serverLevel.random.nextFloat() * 0.4F
            );
        }

        // <- Damage Player ->
        living.hurtServer(serverLevel, source, damage);

        // <- Knockback ->
        float kb = EnchantmentHelper.modifyKnockback(serverLevel, weapon, living, source, 0.4F);
        if (kb > 0) {
            double yaw = Math.toRadians(player.getYRot());

            living.push(-Math.sin(yaw) * kb, 0.1, Math.cos(yaw) * kb);

            living.hurtMarked = true;
        }

        // <- Wind Burst ->
        int wind = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.WIND_BURST), weapon);
        if (wind > 0) {
            serverLevel.explode(null, null,
                    new SimpleExplosionDamageCalculator(false, false, Optional.of(1.0F * wind), Optional.empty()),
                    player.getX(), player.getY(), player.getZ(),
                    1.2F + 0.35F * wind, false,
                    Level.ExplosionInteraction.TRIGGER,
                    ParticleTypes.GUST_EMITTER_SMALL,
                    ParticleTypes.GUST_EMITTER_LARGE,
                    WeightedList.of(),
                    SoundEvents.WIND_CHARGE_BURST
            );
        }

        // <- Shield Break ->
        if (living instanceof Player playerTarget && weapon.getItem() instanceof AxeItem && playerTarget.isBlocking()) {

            ItemStack using = playerTarget.getUseItem();

            if (!using.isEmpty() && using.is(Items.SHIELD)) {

                EquipmentSlot slot = playerTarget.getUsedItemHand().asEquipmentSlot();

                // <- break shield ->
                using.hurtAndBreak(5, playerTarget, slot); // dmg shield
                playerTarget.getCooldowns().addCooldown(using, 100); // add shield cooldown
                playerTarget.stopUsingItem(); // cancel shield holding

                serverLevel.playSound(
                        null,
                        playerTarget.getX(), playerTarget.getY(), playerTarget.getZ(),
                        SoundEvents.SHIELD_BREAK,
                        SoundSource.PLAYERS,
                        1.0F, 1.0F
                );
            }
        }
    }
}