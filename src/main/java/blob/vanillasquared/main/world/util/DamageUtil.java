package blob.vanillasquared.main.world.util;

import blob.vanillasquared.main.world.effect.LungingState;
import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.effect.VSQMobEffects;
import blob.vanillasquared.main.world.effect.VoidedEffectState;
import blob.vanillasquared.util.api.modules.attributes.VSQAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class DamageUtil {
    private static final TagKey<DamageType> BYPASSES_VOIDED = TagKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "bypasses_voided")
    );

    private DamageUtil() {
    }

    public static float applyCustomProtections(LivingEntity entity, DamageSource source, float amount) {
        float protectedAmount = applyMaceProtection(entity, source, amount);
        protectedAmount = applyMagicProtection(entity, source, protectedAmount);
        protectedAmount = applyDripstoneProtection(entity, source, protectedAmount);
        protectedAmount = applySpearProtection(entity, source, protectedAmount);
        protectedAmount = applyVoided(entity, source, protectedAmount);
        protectedAmount = LungingState.amplifyIncomingDamage(entity, protectedAmount);
        return Math.max(protectedAmount, 0.0F);
    }

    public static float applyMaceProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.IS_MACE_SMASH)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, VSQAttributes.MACE_PROTECTION, amount);
    }

    public static float applyMagicProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && !isBreachAttack(source)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, VSQAttributes.MAGIC_PROTECTION, amount);
    }

    public static float applyDripstoneProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypes.STALAGMITE) && !source.is(DamageTypes.FALLING_STALACTITE)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, VSQAttributes.DRIPSTONE_PROTECTION, amount);
    }

    public static float applySpearProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypes.SPEAR)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, VSQAttributes.SPEAR_PROTECTION, amount);
    }

    public static float applyVoided(LivingEntity entity, DamageSource source, float amount) {
        if (!entity.hasEffect(VSQMobEffects.VOIDED) || source.is(BYPASSES_VOIDED) || amount <= 0.0F) {
            return Math.max(amount, 0.0F);
        }

        float multiplier = VoidedEffectState.consume(entity);
        VoidedEffectState.scheduleRemoveEffect(entity);
        return Math.max(amount * multiplier, 0.0F);
    }

    private static float applyPercentageProtection(LivingEntity entity, Holder<Attribute> attribute, float amount) {
        double protection = entity.getAttributeValue(attribute);
        protection = Math.clamp(protection, 0.0, 1.0);
        return Math.max(amount * (1.0F - (float) protection), 0.0F);
    }

    private static boolean isBreachAttack(DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return false;
        }

        var enchantments = livingAttacker.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.BREACH),
                livingAttacker.getMainHandItem()
        ) > 0;
    }
}
