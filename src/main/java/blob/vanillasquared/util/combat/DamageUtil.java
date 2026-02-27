package blob.vanillasquared.util.combat;

import blob.vanillasquared.util.modules.attributes.RegisterAttributes;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

public final class DamageUtil {

    private DamageUtil() {
    }

    public static float applyCustomProtections(LivingEntity entity, DamageSource source, float amount) {
        float protectedAmount = applyMaceProtection(entity, source, amount);
        protectedAmount = applyMagicProtection(entity, source, protectedAmount);
        protectedAmount = applyDripstoneProtection(entity, source, protectedAmount);
        protectedAmount = applySpearProtection(entity, source, protectedAmount);
        return Math.max(protectedAmount, 0.0F);
    }

    public static float applyMaceProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.IS_MACE_SMASH)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, RegisterAttributes.maceProtectionAttribute, amount);
    }

    public static float applyMagicProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, RegisterAttributes.magicProtectionAttribute, amount);
    }

    public static float applyDripstoneProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypes.STALAGMITE) && !source.is(DamageTypes.FALLING_STALACTITE)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, RegisterAttributes.dripstoneProtectionAttribute, amount);
    }

    public static float applySpearProtection(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypes.SPEAR)) {
            return Math.max(amount, 0.0F);
        }

        return applyPercentageProtection(entity, RegisterAttributes.spearProtectionAttribute, amount);
    }

    private static float applyPercentageProtection(LivingEntity entity, Holder<Attribute> attribute, float amount) {
        double protection = entity.getAttributeValue(attribute);
        protection = Math.clamp(protection, 0.0, 1.0);
        return Math.max(amount * (1.0F - (float) protection), 0.0F);
    }
}
