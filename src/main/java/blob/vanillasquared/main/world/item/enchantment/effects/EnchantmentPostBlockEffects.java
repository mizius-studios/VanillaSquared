package blob.vanillasquared.main.world.item.enchantment.effects;

import blob.vanillasquared.util.api.enchantment.VSQEnchantmentEffects;
import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import blob.vanillasquared.main.world.item.enchantment.SpecialEnchantmentCooldowns;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.TargetedConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;

import java.util.List;

public final class EnchantmentPostBlockEffects {
    private EnchantmentPostBlockEffects() {
    }

    public static void run(ServerLevel serverLevel, Entity victim, DamageSource damageSource, ItemStack itemStack, ItemStack blockingItem, EquipmentSlot blockingSlot) {
        if (victim instanceof LivingEntity livingVictim) {
            if (blockingItem != null && !blockingItem.isEmpty()) {
                apply(serverLevel, blockingItem, blockingSlot, livingVictim, EnchantmentTarget.VICTIM, victim, damageSource);
            }
        }

        if (itemStack != null && !itemStack.isEmpty() && damageSource.getEntity() instanceof LivingEntity attacker) {
            apply(serverLevel, itemStack, null, attacker, EnchantmentTarget.ATTACKER, victim, damageSource);
        }
    }

    private static void apply(ServerLevel serverLevel, ItemStack stack, EquipmentSlot slot, LivingEntity owner, EnchantmentTarget forTarget, Entity victim, DamageSource damageSource) {
        ItemEnchantments enchantments = VSQEnchantments.aggregate(stack);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int enchantmentLevel = entry.getIntValue();
            List<TargetedConditionalEffect<EnchantmentEntityEffect>> effects =
                    VSQEnchantments.profileEffects(stack, enchantment, VSQEnchantmentEffects.POST_BLOCK);
            if (effects.isEmpty()) {
                continue;
            }

            var context = Enchantment.damageContext(serverLevel, enchantmentLevel, victim, damageSource);
            for (int index = 0; index < effects.size(); index++) {
                TargetedConditionalEffect<EnchantmentEntityEffect> effect = effects.get(index);
                if (forTarget == effect.enchanted()
                        && effect.matches(context)
                        && SpecialEnchantmentCooldowns.shouldRunSpecialEffect(serverLevel, stack, enchantment.value(), VSQEnchantmentEffects.POST_BLOCK, index, owner)) {
                    EnchantedItemInUse item = new EnchantedItemInUse(stack, slot, owner);
                    Entity affected = resolveAffectedEntity(effect, victim, damageSource);
                    if (affected != null) {
                        effect.effect().apply(serverLevel, enchantmentLevel, item, affected, affected.position());
                    }
                }
            }
        }
    }

    private static Entity resolveAffectedEntity(TargetedConditionalEffect<EnchantmentEntityEffect> effect, Entity victim, DamageSource damageSource) {
        return switch (effect.affected()) {
            case ATTACKER -> damageSource.getEntity();
            case DAMAGING_ENTITY -> damageSource.getDirectEntity();
            case VICTIM -> victim;
        };
    }
}
