package blob.vanillasquared.mixin.world.item;

import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentProfile;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.main.world.item.components.enchantment.SpecialEnchantmentCooldowns;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Unique
    private static <T> boolean vsq$allowSpecialEffect(
            ServerLevel level,
            ItemStack stack,
            Holder<Enchantment> enchantment,
            DataComponentType<List<T>> componentType,
            int index,
            LivingEntity contextEntity
    ) {
        return SpecialEnchantmentCooldowns.shouldRunSpecialEffect(level, stack, enchantment.value(), componentType, index, contextEntity);
    }

    @Inject(method = "isImmuneToDamage", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileDamageImmunity(ServerLevel level, LivingEntity victim, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        MutableBoolean result = new MutableBoolean();
        EnchantmentHelper.runIterationOnEquipment(victim, (enchantment, enchantmentLevel, item) -> {
            if (result.booleanValue()) {
                return;
            }
            LootContext context = Enchantment.damageContext(level, enchantmentLevel, victim, source);
            List<ConditionalEffect<DamageImmunity>> effects = VSQEnchantmentSlots.profileEffects(item.itemStack(), enchantment, EnchantmentEffectComponents.DAMAGE_IMMUNITY);
            for (int index = 0; index < effects.size(); index++) {
                ConditionalEffect<DamageImmunity> effect = effects.get(index);
                if (effect.matches(context) && vsq$allowSpecialEffect(level, item.itemStack(), enchantment, EnchantmentEffectComponents.DAMAGE_IMMUNITY, index, item.owner())) {
                    result.setTrue();
                    return;
                }
            }
        });
        cir.setReturnValue(result.booleanValue());
    }

    @Inject(method = "processAmmoUse", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileAmmoUse(ServerLevel level, ItemStack weapon, ItemStack ammo, int amount, CallbackInfoReturnable<Integer> cir) {
        MutableFloat modifiedAmount = new MutableFloat(amount);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, enchantmentLevel) -> Enchantment.applyEffects(
                VSQEnchantmentSlots.profileEffects(weapon, enchantment, EnchantmentEffectComponents.AMMO_USE),
                Enchantment.itemContext(level, enchantmentLevel, ammo),
                modifiedAmount,
                (effect, currentValue) -> effect.process(enchantmentLevel, level.getRandom(), currentValue)
        ));
        cir.setReturnValue(modifiedAmount.intValue());
    }

    @Inject(method = "getPiercingCount", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfilePiercingCount(ServerLevel level, ItemStack weapon, ItemStack ammo, CallbackInfoReturnable<Integer> cir) {
        MutableFloat modifiedAmount = new MutableFloat(0.0F);
        EnchantmentHelper.runIterationOnItem(weapon, (enchantment, enchantmentLevel) -> Enchantment.applyEffects(
                VSQEnchantmentSlots.profileEffects(weapon, enchantment, EnchantmentEffectComponents.PROJECTILE_PIERCING),
                Enchantment.itemContext(level, enchantmentLevel, ammo),
                modifiedAmount,
                (effect, currentValue) -> effect.process(enchantmentLevel, level.getRandom(), currentValue)
        ));
        cir.setReturnValue(Math.max(0, modifiedAmount.intValue()));
    }

    @Inject(method = "processEquipmentDropChance", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileEquipmentDropChance(
            ServerLevel level,
            LivingEntity entity,
            DamageSource killingBlow,
            float chance,
            CallbackInfoReturnable<Float> cir
    ) {
        MutableFloat modifiedChance = new MutableFloat(chance);
        RandomSource random = entity.getRandom();
        EnchantmentHelper.runIterationOnEquipment(entity, (enchantment, enchantmentLevel, item) -> {
            LootContext context = Enchantment.damageContext(level, enchantmentLevel, entity, killingBlow);
            List<net.minecraft.world.item.enchantment.TargetedConditionalEffect<EnchantmentValueEffect>> effects =
                    VSQEnchantmentSlots.profileEffects(item.itemStack(), enchantment, EnchantmentEffectComponents.EQUIPMENT_DROPS);
            for (int index = 0; index < effects.size(); index++) {
                net.minecraft.world.item.enchantment.TargetedConditionalEffect<EnchantmentValueEffect> effect = effects.get(index);
                if (effect.enchanted() == EnchantmentTarget.VICTIM
                        && effect.affected() == EnchantmentTarget.VICTIM
                        && effect.matches(context)
                        && vsq$allowSpecialEffect(level, item.itemStack(), enchantment, EnchantmentEffectComponents.EQUIPMENT_DROPS, index, item.owner())) {
                    modifiedChance.setValue(((EnchantmentValueEffect) effect.effect()).process(enchantmentLevel, random, modifiedChance.floatValue()));
                }
            }
        });
        if (killingBlow.getEntity() instanceof LivingEntity attacker) {
            EnchantmentHelper.runIterationOnEquipment(attacker, (enchantment, enchantmentLevel, item) -> {
                LootContext context = Enchantment.damageContext(level, enchantmentLevel, entity, killingBlow);
                List<net.minecraft.world.item.enchantment.TargetedConditionalEffect<EnchantmentValueEffect>> effects =
                        VSQEnchantmentSlots.profileEffects(item.itemStack(), enchantment, EnchantmentEffectComponents.EQUIPMENT_DROPS);
                for (int index = 0; index < effects.size(); index++) {
                    net.minecraft.world.item.enchantment.TargetedConditionalEffect<EnchantmentValueEffect> effect = effects.get(index);
                    if (effect.enchanted() == EnchantmentTarget.ATTACKER
                            && effect.affected() == EnchantmentTarget.VICTIM
                            && effect.matches(context)
                            && vsq$allowSpecialEffect(level, item.itemStack(), enchantment, EnchantmentEffectComponents.EQUIPMENT_DROPS, index, item.owner())) {
                        modifiedChance.setValue(((EnchantmentValueEffect) effect.effect()).process(enchantmentLevel, random, modifiedChance.floatValue()));
                    }
                }
            });
        }

        cir.setReturnValue(modifiedChance.floatValue());
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileAttributeGroup(
            ItemStack stack,
            EquipmentSlotGroup slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo ci
    ) {
        EnchantmentHelper.runIterationOnItem(stack, (enchantment, level) -> {
            if (vsq$matchingSlotGroup(stack, enchantment, slot)) {
                for (EnchantmentAttributeEffect effect : VSQEnchantmentSlots.profileEffects(stack, enchantment, EnchantmentEffectComponents.ATTRIBUTES)) {
                    consumer.accept(effect.attribute(), effect.getModifier(level, slot));
                }
            }
        });
        ci.cancel();
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileAttributeSlot(
            ItemStack stack,
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo ci
    ) {
        EnchantmentHelper.runIterationOnItem(stack, (enchantment, level) -> {
            if (vsq$matchingSlot(stack, enchantment, slot)) {
                for (EnchantmentAttributeEffect effect : VSQEnchantmentSlots.profileEffects(stack, enchantment, EnchantmentEffectComponents.ATTRIBUTES)) {
                    consumer.accept(effect.attribute(), effect.getModifier(level, slot));
                }
            }
        });
        ci.cancel();
    }

    @Inject(method = "modifyCrossbowChargingTime", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileCrossbowChargeTime(ItemStack crossbow, LivingEntity holder, float time, CallbackInfoReturnable<Float> cir) {
        MutableFloat modifiedTime = new MutableFloat(time);
        EnchantmentHelper.runIterationOnItem(crossbow, (enchantment, level) -> {
            EnchantmentValueEffect effect = VSQEnchantmentSlots.profileEffect(crossbow, enchantment, EnchantmentEffectComponents.CROSSBOW_CHARGE_TIME);
            if (effect != null) {
                modifiedTime.setValue(effect.process(level, holder.getRandom(), modifiedTime.floatValue()));
            }
        });
        cir.setReturnValue(Math.max(0.0F, modifiedTime.floatValue()));
    }

    @Inject(method = "getTridentSpinAttackStrength", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileTridentSpinStrength(ItemStack trident, LivingEntity holder, CallbackInfoReturnable<Float> cir) {
        MutableFloat strength = new MutableFloat(0.0F);
        EnchantmentHelper.runIterationOnItem(trident, (enchantment, level) -> {
            EnchantmentValueEffect effect = VSQEnchantmentSlots.profileEffect(trident, enchantment, EnchantmentEffectComponents.TRIDENT_SPIN_ATTACK_STRENGTH);
            if (effect != null) {
                strength.setValue(effect.process(level, holder.getRandom(), strength.floatValue()));
            }
        });
        cir.setReturnValue(strength.floatValue());
    }

    @Inject(method = "has", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileHas(ItemStack item, DataComponentType<?> effectType, CallbackInfoReturnable<Boolean> cir) {
        MutableBoolean found = new MutableBoolean(false);
        EnchantmentHelper.runIterationOnItem(item, (enchantment, level) -> {
            if (VSQEnchantmentSlots.profileEffect(item, enchantment, effectType) != null) {
                found.setTrue();
            }
        });
        cir.setReturnValue(found.booleanValue());
    }

    @Inject(method = "pickHighestLevel", at = @At("HEAD"), cancellable = true)
    private static <T> void vsq$useSelectedProfilePickHighestLevel(
            ItemStack item,
            DataComponentType<List<T>> componentType,
            CallbackInfoReturnable<Optional<T>> cir
    ) {
        Pair<List<T>, Integer> picked = vsq$getHighestLevel(item, componentType);
        if (picked != null) {
            List<T> list = picked.getFirst();
            cir.setReturnValue(Optional.of(list.get(Math.max(0, Math.min(picked.getSecond(), list.size()) - 1))));
        } else {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "getHighestLevel", at = @At("HEAD"), cancellable = true)
    private static <T> void vsq$useSelectedProfileHighestLevel(
            ItemStack item,
            DataComponentType<T> effectType,
            CallbackInfoReturnable<Pair<T, Integer>> cir
    ) {
        cir.setReturnValue(vsq$getHighestLevel(item, effectType));
    }

    @Inject(method = "getRandomItemWith", at = @At("HEAD"), cancellable = true)
    private static void vsq$useSelectedProfileRandomItemWith(
            DataComponentType<?> componentType,
            LivingEntity source,
            Predicate<ItemStack> predicate,
            CallbackInfoReturnable<Optional<EnchantedItemInUse>> cir
    ) {
        List<EnchantedItemInUse> items = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack item = source.getItemBySlot(slot);
            if (!predicate.test(item)) {
                continue;
            }

            ItemEnchantments enchantments = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Holder<Enchantment> enchantment = entry.getKey();
                if (VSQEnchantmentSlots.profileEffect(item, enchantment, componentType) != null && vsq$matchingSlot(item, enchantment, slot)) {
                    items.add(new EnchantedItemInUse(item, slot, source));
                }
            }
        }

        cir.setReturnValue(Util.getRandomSafe(items, source.getRandom()));
    }

    @Unique
    private static <T> Pair<T, Integer> vsq$getHighestLevel(ItemStack item, DataComponentType<T> effectType) {
        MutableObject<Pair<T, Integer>> found = new MutableObject<>();
        EnchantmentHelper.runIterationOnItem(item, (enchantment, level) -> {
            if (found.get() == null || found.get().getSecond() < level) {
                T effect = VSQEnchantmentSlots.profileEffect(item, enchantment, effectType);
                if (effect != null) {
                    found.setValue(Pair.of(effect, level));
                }
            }
        });
        return found.get();
    }

    @Unique
    private static boolean vsq$matchingSlot(ItemStack stack, Holder<Enchantment> enchantment, EquipmentSlot slot) {
        return VSQEnchantmentSlots.selectedProfile(stack, enchantment)
                .map(profile -> profile.slots().stream().anyMatch(group -> group.test(slot)))
                .orElseGet(() -> enchantment.value().matchingSlot(slot));
    }

    @Unique
    private static boolean vsq$matchingSlotGroup(ItemStack stack, Holder<Enchantment> enchantment, EquipmentSlotGroup slot) {
        return VSQEnchantmentSlots.selectedProfile(stack, enchantment)
                .map(VSQEnchantmentProfile::slots)
                .map(slots -> slots.contains(slot))
                .orElseGet(() -> enchantment.value().definition().slots().contains(slot));
    }
}
