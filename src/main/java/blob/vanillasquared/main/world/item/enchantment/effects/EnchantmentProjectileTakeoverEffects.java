package blob.vanillasquared.main.world.item.enchantment.effects;

import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentProfile;
import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.TargetedConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.List;
import java.util.Optional;

public final class EnchantmentProjectileTakeoverEffects {
    private EnchantmentProjectileTakeoverEffects() {
    }

    public static float modifyDamage(
            ServerLevel level,
            LivingEntity owner,
            ItemStack sourceStack,
            Entity victim,
            DamageSource damageSource,
            float baseDamage
    ) {
        MutableFloat damage = new MutableFloat(baseDamage);
        runTakeoverValueEffects(owner, sourceStack, level, victim, damageSource, EnchantmentEffectComponents.DAMAGE, damage);
        return damage.floatValue();
    }

    public static float modifyKnockback(
            ServerLevel level,
            LivingEntity owner,
            ItemStack sourceStack,
            Entity victim,
            DamageSource damageSource,
            float baseKnockback
    ) {
        MutableFloat knockback = new MutableFloat(baseKnockback);
        runTakeoverValueEffects(owner, sourceStack, level, victim, damageSource, EnchantmentEffectComponents.KNOCKBACK, knockback);
        return knockback.floatValue();
    }

    public static void runPostAttackEffects(
            ServerLevel level,
            LivingEntity owner,
            ItemStack sourceStack,
            Entity victim,
            DamageSource damageSource
    ) {
        runTakeoverPostAttackEffects(owner, sourceStack, level, victim, damageSource, EnchantmentEffectComponents.POST_ATTACK);
    }

    public static void runProjectileSpawnedEffects(
            ServerLevel level,
            LivingEntity owner,
            ItemStack sourceStack,
            Entity projectile
    ) {
        runTakeoverEntityEffects(owner, sourceStack, level, projectile, EnchantmentEffectComponents.PROJECTILE_SPAWNED);
    }

    public static void runHitBlockEffects(
            ServerLevel level,
            LivingEntity owner,
            ItemStack sourceStack,
            Entity projectile,
            Vec3 position,
            BlockState hitBlock
    ) {
        runTakeoverHitBlockEffects(owner, sourceStack, level, projectile, position, hitBlock, EnchantmentEffectComponents.HIT_BLOCK);
    }

    private static void runTakeoverValueEffects(
            LivingEntity owner,
            ItemStack sourceStack,
            ServerLevel level,
            Entity victim,
            DamageSource damageSource,
            DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType,
            MutableFloat value
    ) {
        if (sourceStack.isEmpty()) {
            return;
        }

        EnchantmentHelper.runIterationOnEquipment(owner, (enchantment, enchantmentLevel, item) -> {
            // Skip the item that is the projectile source itself; reference identity is intentional.
            if (item.itemStack() == sourceStack) {
                return;
            }

            Optional<VSQEnchantmentProfile> profile = VSQEnchantments.selectedProjectileTakeoverProfile(sourceStack, enchantment);
            if (profile.isEmpty() || !matchesSlot(profile.get(), item.inSlot())) {
                return;
            }

            Enchantment.applyEffects(
                    profile.get().effects().getOrDefault(effectType, List.of()),
                    Enchantment.damageContext(level, enchantmentLevel, victim, damageSource),
                    value,
                    (effect, currentValue) -> effect.process(enchantmentLevel, victim.getRandom(), currentValue)
            );
        });
    }

    private static void runTakeoverPostAttackEffects(
            LivingEntity owner,
            ItemStack sourceStack,
            ServerLevel level,
            Entity victim,
            DamageSource damageSource,
            DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> effectType
    ) {
        if (sourceStack.isEmpty()) {
            return;
        }

        EnchantmentHelper.runIterationOnEquipment(owner, (enchantment, enchantmentLevel, item) -> {
            // Skip the item that is the projectile source itself; reference identity is intentional.
            if (item.itemStack() == sourceStack) {
                return;
            }

            Optional<VSQEnchantmentProfile> profile = VSQEnchantments.selectedProjectileTakeoverProfile(sourceStack, enchantment);
            if (profile.isEmpty() || !matchesSlot(profile.get(), item.inSlot())) {
                return;
            }

            var context = Enchantment.damageContext(level, enchantmentLevel, victim, damageSource);
            List<TargetedConditionalEffect<EnchantmentEntityEffect>> effects = profile.get().effects().getOrDefault(effectType, List.of());
            for (TargetedConditionalEffect<EnchantmentEntityEffect> effect : effects) {
                if (effect.matches(context)) {
                    Entity affected = resolveAffectedEntity(effect, victim, damageSource);
                    if (affected != null) {
                        effect.effect().apply(level, enchantmentLevel, item, affected, affected.position());
                    }
                }
            }
        });
    }

    private static void runTakeoverEntityEffects(
            LivingEntity owner,
            ItemStack sourceStack,
            ServerLevel level,
            Entity target,
            DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> effectType
    ) {
        if (sourceStack.isEmpty()) {
            return;
        }

        EnchantmentHelper.runIterationOnEquipment(owner, (enchantment, enchantmentLevel, item) -> {
            // Skip the item that is the projectile source itself; reference identity is intentional.
            if (item.itemStack() == sourceStack) {
                return;
            }

            Optional<VSQEnchantmentProfile> profile = VSQEnchantments.selectedProjectileTakeoverProfile(sourceStack, enchantment);
            if (profile.isEmpty() || !matchesSlot(profile.get(), item.inSlot())) {
                return;
            }

            var context = Enchantment.entityContext(level, enchantmentLevel, target, target.position());
            List<ConditionalEffect<EnchantmentEntityEffect>> effects = profile.get().effects().getOrDefault(effectType, List.of());
            for (ConditionalEffect<EnchantmentEntityEffect> effect : effects) {
                if (effect.matches(context)) {
                    effect.effect().apply(level, enchantmentLevel, item, target, target.position());
                }
            }
        });
    }

    private static void runTakeoverHitBlockEffects(
            LivingEntity owner,
            ItemStack sourceStack,
            ServerLevel level,
            Entity projectile,
            Vec3 position,
            BlockState hitBlock,
            DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> effectType
    ) {
        if (sourceStack.isEmpty()) {
            return;
        }

        EnchantmentHelper.runIterationOnEquipment(owner, (enchantment, enchantmentLevel, item) -> {
            // Skip the item that is the projectile source itself; reference identity is intentional.
            if (item.itemStack() == sourceStack) {
                return;
            }

            Optional<VSQEnchantmentProfile> profile = VSQEnchantments.selectedProjectileTakeoverProfile(sourceStack, enchantment);
            if (profile.isEmpty() || !matchesSlot(profile.get(), item.inSlot())) {
                return;
            }

            var context = Enchantment.blockHitContext(level, enchantmentLevel, projectile, position, hitBlock);
            List<ConditionalEffect<EnchantmentEntityEffect>> effects = profile.get().effects().getOrDefault(effectType, List.of());
            for (ConditionalEffect<EnchantmentEntityEffect> effect : effects) {
                if (effect.matches(context)) {
                    effect.effect().apply(level, enchantmentLevel, item, projectile, position);
                }
            }
        });
    }

    private static boolean matchesSlot(VSQEnchantmentProfile profile, EquipmentSlot slot) {
        return profile.slots().stream().anyMatch(group -> group.test(slot));
    }

    private static Entity resolveAffectedEntity(TargetedConditionalEffect<EnchantmentEntityEffect> effect, Entity victim, DamageSource damageSource) {
        return switch (effect.affected()) {
            case ATTACKER -> damageSource.getEntity();
            case DAMAGING_ENTITY -> damageSource.getDirectEntity();
            case VICTIM -> victim;
        };
    }
}
