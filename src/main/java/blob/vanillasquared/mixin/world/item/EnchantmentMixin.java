package blob.vanillasquared.mixin.world.item;

import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentAccess;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentProfile;
import blob.vanillasquared.main.world.item.components.enchantment.SpecialEffectMetadataIndex;
import blob.vanillasquared.main.world.item.components.enchantment.SpecialEnchantmentCooldowns;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlotType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.TargetedConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin implements VSQEnchantmentAccess {
    @Shadow
    @Final
    @Mutable
    public static Codec<Enchantment> DIRECT_CODEC;

    @Unique
    private VSQEnchantmentSlotType vsq$enchantmentSlotType;
    @Unique
    private List<VSQEnchantmentProfile> vsq$profiles = List.of();

    @Unique
    private <T> Optional<List<T>> vsq$profileEffects(ItemStack stack, DataComponentType<List<T>> effectType) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        Enchantment enchantment = (Enchantment) (Object) this;
        return VSQEnchantmentSlots.selectedProfile(stack, enchantment)
                .map(profile -> profile.effects().getOrDefault(effectType, List.of()));
    }

    @Unique
    private static ItemStack vsq$itemStack(ItemInstance item) {
        return item instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    @Unique
    private <T> boolean vsq$allowSpecialEffect(
            ServerLevel level,
            ItemStack stack,
            DataComponentType<List<T>> componentType,
            int effectIndex,
            @Nullable Entity contextEntity
    ) {
        if (stack.isEmpty()) {
            return true;
        }
        return SpecialEnchantmentCooldowns.shouldRunSpecialEffect(level, stack, (Enchantment) (Object) this, componentType, effectIndex, contextEntity);
    }

    @Inject(method = "modifyDamageProtection", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileDamageProtection(
            ServerLevel level,
            int enchantmentLevel,
            ItemStack item,
            Entity victim,
            DamageSource source,
            MutableFloat protection,
            CallbackInfo ci
    ) {
        Optional<List<ConditionalEffect<EnchantmentValueEffect>>> effects = this.vsq$profileEffects(item, EnchantmentEffectComponents.DAMAGE_PROTECTION);
        if (effects.isEmpty()) {
            return;
        }

        Enchantment.applyEffects(
                effects.get(),
                Enchantment.damageContext(level, enchantmentLevel, victim, source),
                protection,
                (effect, currentValue) -> effect.process(enchantmentLevel, victim.getRandom(), currentValue)
        );
        ci.cancel();
    }

    @Inject(method = "doPostAttack", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfilePostAttack(
            ServerLevel serverLevel,
            int enchantmentLevel,
            EnchantedItemInUse item,
            EnchantmentTarget forTarget,
            Entity victim,
            DamageSource damageSource,
            CallbackInfo ci
    ) {
        Optional<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> effects = this.vsq$profileEffects(item.itemStack(), EnchantmentEffectComponents.POST_ATTACK);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.damageContext(serverLevel, enchantmentLevel, victim, damageSource);
        List<TargetedConditionalEffect<EnchantmentEntityEffect>> list = effects.get();
        for (int index = 0; index < list.size(); index++) {
            TargetedConditionalEffect<EnchantmentEntityEffect> effect = list.get(index);
            if (forTarget == effect.enchanted()
                    && effect.matches(context)
                    && vsq$allowSpecialEffect(serverLevel, item.itemStack(), EnchantmentEffectComponents.POST_ATTACK, index, item.owner())) {
                Entity affected = effect.affected() == EnchantmentTarget.ATTACKER && item.owner() != null ? item.owner() : victim;
                effect.effect().apply(serverLevel, enchantmentLevel, item, affected, affected.position());
            }
        }
        ci.cancel();
    }

    @Inject(method = "doPostPiercingAttack", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfilePostPiercingAttack(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity user, CallbackInfo ci) {
        Optional<List<ConditionalEffect<EnchantmentEntityEffect>>> effects = this.vsq$profileEffects(item.itemStack(), EnchantmentEffectComponents.POST_PIERCING_ATTACK);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.entityContext(level, enchantmentLevel, user, user.position());
        List<ConditionalEffect<EnchantmentEntityEffect>> list = effects.get();
        for (int index = 0; index < list.size(); index++) {
            ConditionalEffect<EnchantmentEntityEffect> conditional = list.get(index);
            if (conditional.matches(context) && vsq$allowSpecialEffect(level, item.itemStack(), EnchantmentEffectComponents.POST_PIERCING_ATTACK, index, item.owner())) {
                conditional.effect().apply(level, enchantmentLevel, item, user, user.position());
            }
        }
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileTick(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, CallbackInfo ci) {
        Optional<List<ConditionalEffect<EnchantmentEntityEffect>>> effects = this.vsq$profileEffects(item.itemStack(), EnchantmentEffectComponents.TICK);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.entityContext(level, enchantmentLevel, entity, entity.position());
        List<ConditionalEffect<EnchantmentEntityEffect>> list = effects.get();
        for (int index = 0; index < list.size(); index++) {
            ConditionalEffect<EnchantmentEntityEffect> conditional = list.get(index);
            if (conditional.matches(context) && vsq$allowSpecialEffect(level, item.itemStack(), EnchantmentEffectComponents.TICK, index, item.owner())) {
                conditional.effect().apply(level, enchantmentLevel, item, entity, entity.position());
            }
        }
        ci.cancel();
    }

    @Inject(method = "onProjectileSpawned", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileProjectileSpawned(ServerLevel level, int enchantmentLevel, EnchantedItemInUse weapon, Entity projectile, CallbackInfo ci) {
        Optional<List<ConditionalEffect<EnchantmentEntityEffect>>> effects = this.vsq$profileEffects(weapon.itemStack(), EnchantmentEffectComponents.PROJECTILE_SPAWNED);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.entityContext(level, enchantmentLevel, projectile, projectile.position());
        List<ConditionalEffect<EnchantmentEntityEffect>> list = effects.get();
        for (int index = 0; index < list.size(); index++) {
            ConditionalEffect<EnchantmentEntityEffect> conditional = list.get(index);
            if (conditional.matches(context) && vsq$allowSpecialEffect(level, weapon.itemStack(), EnchantmentEffectComponents.PROJECTILE_SPAWNED, index, weapon.owner())) {
                conditional.effect().apply(level, enchantmentLevel, weapon, projectile, projectile.position());
            }
        }
        ci.cancel();
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileHitBlock(
            ServerLevel level,
            int enchantmentLevel,
            EnchantedItemInUse weapon,
            Entity projectile,
            Vec3 position,
            BlockState hitBlock,
            CallbackInfo ci
    ) {
        Optional<List<ConditionalEffect<EnchantmentEntityEffect>>> effects = this.vsq$profileEffects(weapon.itemStack(), EnchantmentEffectComponents.HIT_BLOCK);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.blockHitContext(level, enchantmentLevel, projectile, position, hitBlock);
        List<ConditionalEffect<EnchantmentEntityEffect>> list = effects.get();
        for (int index = 0; index < list.size(); index++) {
            ConditionalEffect<EnchantmentEntityEffect> conditional = list.get(index);
            if (conditional.matches(context) && vsq$allowSpecialEffect(level, weapon.itemStack(), EnchantmentEffectComponents.HIT_BLOCK, index, weapon.owner())) {
                conditional.effect().apply(level, enchantmentLevel, weapon, projectile, position);
            }
        }
        ci.cancel();
    }

    @Inject(method = "modifyItemFilteredCount", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileItemFilteredValue(
            DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType,
            ServerLevel level,
            int enchantmentLevel,
            ItemInstance item,
            MutableFloat value,
            CallbackInfo ci
    ) {
        Optional<List<ConditionalEffect<EnchantmentValueEffect>>> effects = this.vsq$profileEffects(vsq$itemStack(item), effectType);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.itemContext(level, enchantmentLevel, item);
        for (int index = 0; index < effects.get().size(); index++) {
            ConditionalEffect<EnchantmentValueEffect> conditional = effects.get().get(index);
            if (conditional.matches(context) && vsq$allowSpecialEffect(level, vsq$itemStack(item), effectType, index, null)) {
                value.setValue(conditional.effect().process(enchantmentLevel, level.getRandom(), value.floatValue()));
            }
        }
        ci.cancel();
    }

    @Inject(method = "modifyEntityFilteredValue", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileEntityFilteredValue(
            DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType,
            ServerLevel level,
            int enchantmentLevel,
            ItemStack item,
            Entity entity,
            MutableFloat value,
            CallbackInfo ci
    ) {
        Optional<List<ConditionalEffect<EnchantmentValueEffect>>> effects = this.vsq$profileEffects(item, effectType);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.entityContext(level, enchantmentLevel, entity, entity.position());
        for (int index = 0; index < effects.get().size(); index++) {
            ConditionalEffect<EnchantmentValueEffect> conditional = effects.get().get(index);
            if (conditional.matches(context) && vsq$allowSpecialEffect(level, item, effectType, index, entity)) {
                value.setValue(conditional.effect().process(enchantmentLevel, entity.getRandom(), value.floatValue()));
            }
        }
        ci.cancel();
    }

    @Inject(method = "modifyDamageFilteredValue", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileDamageEffects(
            DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> effectType,
            ServerLevel level,
            int enchantmentLevel,
            ItemStack stack,
            Entity entity,
            DamageSource damageSource,
            MutableFloat value,
            CallbackInfo ci
    ) {
        Optional<List<ConditionalEffect<EnchantmentValueEffect>>> effects = this.vsq$profileEffects(stack, effectType);
        if (effects.isEmpty()) {
            return;
        }

        var context = Enchantment.damageContext(level, enchantmentLevel, entity, damageSource);
        for (int index = 0; index < effects.get().size(); index++) {
            ConditionalEffect<EnchantmentValueEffect> conditional = effects.get().get(index);
            if (conditional.matches(context) && vsq$allowSpecialEffect(level, stack, effectType, index, entity)) {
                value.setValue(conditional.effect().process(enchantmentLevel, entity.getRandom(), value.floatValue()));
            }
        }
        ci.cancel();
    }

    @Inject(method = "runLocationChangedEffects", at = @At("HEAD"), cancellable = true)
    private void vsq$useSelectedProfileLocationChanged(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, LivingEntity entity, CallbackInfo ci) {
        Optional<List<ConditionalEffect<EnchantmentLocationBasedEffect>>> effects = this.vsq$profileEffects(item.itemStack(), EnchantmentEffectComponents.LOCATION_CHANGED);
        if (effects.isEmpty()) {
            return;
        }

        EquipmentSlot slot = item.inSlot();
        if (slot != null) {
            Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEffects = entity.activeLocationDependentEnchantments(slot);
            if (!VSQEnchantmentSlots.selectedProfile(item.itemStack(), (Enchantment) (Object) this)
                    .map(profile -> profile.slots().stream().anyMatch(group -> group.test(slot)))
                    .orElseGet(() -> ((Enchantment) (Object) this).matchingSlot(slot))) {
                Set<EnchantmentLocationBasedEffect> activeEffects = activeLocationDependentEffects.remove((Enchantment) (Object) this);
                if (activeEffects != null) {
                    activeEffects.forEach(effect -> effect.onDeactivated(item, entity, entity.position(), enchantmentLevel));
                }
            } else {
                Set<EnchantmentLocationBasedEffect> activeEffects = activeLocationDependentEffects.get((Enchantment) (Object) this);

                for (int index = 0; index < effects.get().size(); index++) {
                    ConditionalEffect<EnchantmentLocationBasedEffect> filteredEffect = effects.get().get(index);
                    EnchantmentLocationBasedEffect effect = filteredEffect.effect();
                    boolean wasActive = activeEffects != null && activeEffects.contains(effect);
                    if (filteredEffect.matches(Enchantment.locationContext(level, enchantmentLevel, entity, wasActive))
                            && vsq$allowSpecialEffect(level, item.itemStack(), EnchantmentEffectComponents.LOCATION_CHANGED, index, item.owner())) {
                        if (!wasActive) {
                            if (activeEffects == null) {
                                activeEffects = new HashSet<>();
                                activeLocationDependentEffects.put((Enchantment) (Object) this, activeEffects);
                            }

                            activeEffects.add(effect);
                        }

                        effect.onChangedBlock(level, enchantmentLevel, item, entity, entity.position(), !wasActive);
                    } else if (activeEffects != null && activeEffects.remove(effect)) {
                        effect.onDeactivated(item, entity, entity.position(), enchantmentLevel);
                    }
                }

                if (activeEffects != null && activeEffects.isEmpty()) {
                    activeLocationDependentEffects.remove((Enchantment) (Object) this);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void vsq$extendCodec(CallbackInfo ci) {
        DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("description").forGetter(Enchantment::description),
                RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("supported_items").forGetter(enchantment -> enchantment.definition().supportedItems()),
                RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("primary_items").forGetter(enchantment -> enchantment.definition().primaryItems()),
                Codec.INT.fieldOf("weight").forGetter(enchantment -> enchantment.definition().weight()),
                Codec.INT.fieldOf("anvil_cost").forGetter(enchantment -> enchantment.definition().anvilCost()),
                VSQEnchantmentProfile.CODEC.listOf().optionalFieldOf("profiles").forGetter(enchantment -> Optional.of(((VSQEnchantmentAccess) (Object) enchantment).vsq$getProfiles())),
                RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set").forGetter(enchantment -> Optional.empty()),
                EnchantmentEffectComponents.CODEC.optionalFieldOf("effects").forGetter(enchantment -> Optional.empty()),
                VSQEnchantmentSlotType.CODEC.optionalFieldOf("enchantment_slot").forGetter(enchantment -> Optional.empty()),
                Codec.intRange(1, Enchantment.MAX_LEVEL).optionalFieldOf("max_level").forGetter(enchantment -> Optional.empty()),
                Enchantment.Cost.CODEC.optionalFieldOf("min_cost").forGetter(enchantment -> Optional.empty()),
                Enchantment.Cost.CODEC.optionalFieldOf("max_cost").forGetter(enchantment -> Optional.empty()),
                EquipmentSlotGroup.CODEC.listOf().optionalFieldOf("slots").forGetter(enchantment -> Optional.empty())
        ).apply(instance, EnchantmentMixin::vsq$createEnchantment));
    }

    @Unique
    private static Enchantment vsq$createEnchantment(
            net.minecraft.network.chat.Component description,
            HolderSet<Item> supportedItems,
            Optional<HolderSet<Item>> primaryItems,
            int weight,
            int anvilCost,
            Optional<List<VSQEnchantmentProfile>> profiles,
            Optional<HolderSet<Enchantment>> legacyExclusiveSet,
            Optional<DataComponentMap> legacyEffects,
            Optional<VSQEnchantmentSlotType> legacySlotType,
            Optional<Integer> legacyMaxLevel,
            Optional<Enchantment.Cost> legacyMinCost,
            Optional<Enchantment.Cost> legacyMaxCost,
            Optional<List<EquipmentSlotGroup>> legacySlots
    ) {
        Optional<List<VSQEnchantmentProfile>> providedProfiles = profiles.filter(list -> !list.isEmpty());
        List<VSQEnchantmentProfile> resolvedProfiles = providedProfiles.orElseGet(() -> legacySlotType.map(slotType -> List.of(new VSQEnchantmentProfile(
                Optional.empty(),
                slotType,
                legacyExclusiveSet.orElse(HolderSet.empty()),
                legacyMaxLevel.orElseThrow(() -> new IllegalArgumentException("Missing max_level")),
                legacyEffects.orElse(DataComponentMap.EMPTY),
                Optional.empty(),
                SpecialEffectMetadataIndex.EMPTY,
                legacySlots.orElseThrow(() -> new IllegalArgumentException("Missing slots")),
                legacyMaxCost.orElseThrow(() -> new IllegalArgumentException("Missing max_cost")),
                legacyMinCost.orElseThrow(() -> new IllegalArgumentException("Missing min_cost"))
        ))).orElse(List.of()));

        int maxLevel = providedProfiles.map(list -> list.getFirst().maxLevel()).orElseGet(() -> legacyMaxLevel.orElseThrow(() -> new IllegalArgumentException("Missing max_level or profiles")));
        Enchantment.Cost minCost = providedProfiles.map(list -> list.getFirst().minCost()).orElseGet(() -> legacyMinCost.orElseThrow(() -> new IllegalArgumentException("Missing min_cost or profiles")));
        Enchantment.Cost maxCost = providedProfiles.map(list -> list.getFirst().maxCost()).orElseGet(() -> legacyMaxCost.orElseThrow(() -> new IllegalArgumentException("Missing max_cost or profiles")));
        List<EquipmentSlotGroup> slots = providedProfiles.map(list -> list.getFirst().slots()).orElseGet(() -> legacySlots.orElseThrow(() -> new IllegalArgumentException("Missing slots or profiles")));
        HolderSet<Enchantment> exclusiveSet = providedProfiles.map(list -> list.getFirst().exclusiveSet()).orElseGet(() -> legacyExclusiveSet.orElse(HolderSet.empty()));
        DataComponentMap effects = providedProfiles.map(list -> list.getFirst().effects()).orElseGet(() -> legacyEffects.orElse(DataComponentMap.EMPTY));

        Enchantment.EnchantmentDefinition definition = new Enchantment.EnchantmentDefinition(
                supportedItems,
                primaryItems,
                weight,
                maxLevel,
                minCost,
                maxCost,
                anvilCost,
                slots
        );
        Enchantment enchantment = new Enchantment(description, definition, exclusiveSet, effects);
        ((VSQEnchantmentAccess) (Object) enchantment).vsq$setProfiles(resolvedProfiles);
        ((VSQEnchantmentAccess) (Object) enchantment).vsq$setEnchantmentSlotType(resolvedProfiles.isEmpty() ? null : resolvedProfiles.getFirst().enchantmentSlot());
        return enchantment;
    }

    @Override
    public VSQEnchantmentSlotType vsq$getEnchantmentSlotType() {
        return this.vsq$enchantmentSlotType;
    }

    @Override
    public void vsq$setEnchantmentSlotType(VSQEnchantmentSlotType slotType) {
        this.vsq$enchantmentSlotType = slotType;
    }

    @Override
    public List<VSQEnchantmentProfile> vsq$getProfiles() {
        return this.vsq$profiles;
    }

    @Override
    public void vsq$setProfiles(List<VSQEnchantmentProfile> profiles) {
        this.vsq$profiles = List.copyOf(profiles);
    }
}
