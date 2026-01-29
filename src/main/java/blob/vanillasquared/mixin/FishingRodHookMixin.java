package blob.vanillasquared.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug(export = true)
@Mixin(FishingHook.class)
public abstract class FishingRodHookMixin extends Projectile {

    public FishingRodHookMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    } // needed for extends
    @Unique // Damage type
    private static final ResourceKey<DamageType> FISHED = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("vanillasquared", "fished"));

    // iInject starts

    @Inject(at = @At("TAIL"), method = "onHitEntity")
    private void init(EntityHitResult hit, CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (!(this.getOwner() instanceof Player player)) return;

        ItemStack weapon = player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();

        Entity target = hit.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = serverLevel.damageSources().source(FISHED, this, owner);

        float damage = 0.5F;
        var enchants = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        int fire = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.FIRE_ASPECT), weapon);
        int smite = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SMITE), weapon);
        int bane = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), weapon);
        if (bane > 0 && target instanceof LivingEntity living && living.getType().is(EntityTypeTags.UNDEAD)) {
            damage += bane * 2.5F;

            int duration = 20 + serverLevel.random.nextInt(10 * bane);
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 3));
        }
        if (smite > 0 && target instanceof LivingEntity living && living.getType().is(EntityTypeTags.UNDEAD)) {

            damage += smite * 2.5F;
        }
        if (fire > 0) {
            target.igniteForSeconds(4 * fire);
            serverLevel.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS,
                    0.7F,
                    1.0F + serverLevel.random.nextFloat() * 0.4F
            );
        }

        target.hurtServer(serverLevel, source, damage);
        EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);

        if (target instanceof Player playerTarget && weapon.getItem() instanceof AxeItem && playerTarget.isBlocking())  {
            ItemStack using = playerTarget.getUseItem();
            if (!using.isEmpty() && using.is(Items.SHIELD)) {
                playerTarget.getCooldowns().addCooldown(using, 100);
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