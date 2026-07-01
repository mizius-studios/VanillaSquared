package blob.vanillasquared.main.world.effect;

import blob.vanillasquared.main.network.VSQNetworking;
import blob.vanillasquared.main.network.payload.SwirlingStatePayload;
import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SwirlingState {
    private static final WeakHashMap<ServerLevel, Map<UUID, Activation>> STATES = new WeakHashMap<>();

    private SwirlingState() {
    }

    public static void start(
            ServerLevel level,
            Holder<Enchantment> enchantment,
            int enchantmentLevel,
            EnchantedItemInUse item,
            LivingEntity owner,
            int durationTicks,
            int warmupTicks,
            double radius,
            int hitInterval,
            ResourceKey<DamageType> damageType,
            float damage
    ) {
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }

        Identifier enchantmentId = enchantment.unwrapKey()
                .map(ResourceKey::identifier)
                .orElseThrow(() -> new IllegalStateException("Cannot start swirl for enchantment holder without a registry key"));
        Activation previous = levelState(level).remove(owner.getUUID());
        if (previous != null) {
            previous.deactivate(level);
        }

        ItemStack stackSnapshot = item.itemStack().copy();
        Activation activation = new Activation(
                owner.getUUID(),
                enchantmentLevel,
                enchantmentId,
                stackSnapshot,
                item.inSlot(),
                Math.max(1, durationTicks),
                Math.max(0, warmupTicks),
                Math.max(0.0D, radius),
                Math.max(1, hitInterval),
                damageType,
                Math.max(0.0F, damage)
        );
        levelState(level).put(owner.getUUID(), activation);
        VSQNetworking.sendSwirlingState(player, true, activation.ticksRemaining, activation.totalTicks, activation.warmupTicks, false);
    }

    public static void tick(ServerLevel level) {
        Map<UUID, Activation> activations = STATES.get(level);
        if (activations == null || activations.isEmpty()) {
            return;
        }

        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Activation> entry : activations.entrySet()) {
            Activation activation = entry.getValue();
            LivingEntity owner = activation.resolveOwner(level);
            if (!(owner instanceof ServerPlayer) || !owner.isAlive()) {
                activation.deactivate(level);
                expired.add(entry.getKey());
                continue;
            }

            ServerPlayer player = (ServerPlayer) owner;
            if (activation.updatePaused(level, player)) {
                continue;
            }

            if (activation.isWarmingUp()) {
                activation.applyWarmupMovement(owner);
            }

            if (activation.shouldPulse()) {
                activation.applyPulse(level, owner);
            }

            activation.ticksRemaining--;
            if (activation.ticksRemaining <= 0) {
                activation.deactivate(level);
                expired.add(entry.getKey());
            }
        }

        for (UUID uuid : expired) {
            activations.remove(uuid);
        }
        if (activations.isEmpty()) {
            STATES.remove(level);
        }
    }

    public static void clear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Activation activation = levelState(level).remove(player.getUUID());
        if (activation != null) {
            activation.deactivate(level);
        }
    }

    public static void syncToTrackingPlayer(LivingEntity entity, ServerPlayer trackingPlayer) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Activation activation = STATES.getOrDefault(level, Map.of()).get(entity.getUUID());
        if (activation == null || activation.ticksRemaining <= 0) {
            return;
        }
        ServerPlayNetworking.send(
                trackingPlayer,
                new SwirlingStatePayload(entity.getId(), true, activation.ticksRemaining, activation.totalTicks, activation.warmupTicks, activation.paused)
        );
    }

    private static Map<UUID, Activation> levelState(ServerLevel level) {
        return STATES.computeIfAbsent(level, ignored -> new HashMap<>());
    }

    private static final class Activation {
        private final UUID ownerId;
        private final int enchantmentLevel;
        private final Identifier enchantmentId;
        private final ItemStack stackSnapshot;
        private final EquipmentSlot slot;
        private final double radius;
        private final int hitInterval;
        private final ResourceKey<DamageType> damageType;
        private final float damage;
        private final int totalTicks;
        private final int warmupTicks;
        private int ticksRemaining;
        private int ticksSinceLastHit;
        private boolean paused;

        private Activation(
                UUID ownerId,
                int enchantmentLevel,
                Identifier enchantmentId,
                ItemStack stackSnapshot,
                EquipmentSlot slot,
                int ticksRemaining,
                int warmupTicks,
                double radius,
                int hitInterval,
                ResourceKey<DamageType> damageType,
                float damage
        ) {
            this.ownerId = ownerId;
            this.enchantmentLevel = enchantmentLevel;
            this.enchantmentId = enchantmentId;
            this.stackSnapshot = stackSnapshot;
            this.slot = slot;
            this.warmupTicks = warmupTicks;
            this.ticksRemaining = ticksRemaining + warmupTicks;
            this.totalTicks = this.ticksRemaining;
            this.radius = radius;
            this.hitInterval = hitInterval;
            this.damageType = damageType;
            this.damage = damage;
        }

        private LivingEntity resolveOwner(ServerLevel level) {
            Entity entity = level.getEntity(this.ownerId);
            return entity instanceof LivingEntity living ? living : null;
        }

        private boolean updatePaused(ServerLevel level, ServerPlayer owner) {
            boolean shouldPause = !this.isMatchingEnchantmentHeld(owner);
            if (this.paused != shouldPause) {
                this.paused = shouldPause;
                VSQNetworking.sendSwirlingState(owner, true, this.ticksRemaining, this.totalTicks, this.warmupTicks, this.paused);
            }
            return this.paused;
        }

        private boolean isMatchingEnchantmentHeld(ServerPlayer owner) {
            return this.hasMatchingEnchantment(owner.getMainHandItem()) || this.hasMatchingEnchantment(owner.getOffhandItem());
        }

        private boolean hasMatchingEnchantment(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            for (Holder<Enchantment> enchantment : VSQEnchantments.aggregate(stack).keySet()) {
                if (enchantment.unwrapKey().map(ResourceKey::identifier).filter(this.enchantmentId::equals).isPresent()) {
                    return true;
                }
            }
            return false;
        }

        private boolean shouldPulse() {
            if (this.isWarmingUp()) {
                return false;
            }
            if (this.ticksSinceLastHit > 0) {
                this.ticksSinceLastHit--;
                return false;
            }
            this.ticksSinceLastHit = this.hitInterval - 1;
            return true;
        }

        private boolean isWarmingUp() {
            return this.ticksRemaining > this.totalTicks - this.warmupTicks;
        }

        private void applyWarmupMovement(LivingEntity owner) {
            Vec3 movement = owner.getDeltaMovement();
            owner.setDeltaMovement(movement.x * 0.35D, movement.y, movement.z * 0.35D);
            owner.setSprinting(false);
            owner.hurtMarked = true;
            owner.needsSync = true;
        }

        private void applyPulse(ServerLevel level, LivingEntity owner) {
            if (this.damage <= 0.0F) {
                return;
            }

            double radiusSqr = this.radius * this.radius;
            AABB bounds = owner.getBoundingBox().inflate(this.radius);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bounds, entity ->
                    entity.isAlive()
                            && entity != owner
                            && entity.distanceToSqr(owner) <= radiusSqr
            );
            if (targets.isEmpty()) {
                return;
            }

            DamageSource damageSource = level.damageSources().source(this.damageType, owner, owner);
            for (LivingEntity target : targets) {
                int previousInvulnerableTime = target.invulnerableTime;
                target.invulnerableTime = 0;
                target.hurtServer(level, damageSource, this.damage);
                target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
            }
        }

        private void deactivate(ServerLevel level) {
            LivingEntity owner = this.resolveOwner(level);
            if (owner instanceof ServerPlayer player) {
                VSQNetworking.sendSwirlingState(player, false, 0, this.totalTicks, this.warmupTicks, false);
            }
        }
    }
}
