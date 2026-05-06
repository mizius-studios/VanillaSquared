package blob.vanillasquared.main.world.effect;

import blob.vanillasquared.main.network.VSQNetworking;
import blob.vanillasquared.main.world.item.enchantment.SpecialEnchantmentCooldowns;
import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.TargetedConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class LungingState {
    private static final WeakHashMap<ServerLevel, Map<UUID, Activation>> STATES = new WeakHashMap<>();
    private static final double COLLISION_EPSILON = 0.05D;
    private static final double MIN_ACTIVE_SPEED = 0.08D;
    private LungingState() {
    }

    public static void start(
            ServerLevel level,
            Holder<Enchantment> enchantment,
            int enchantmentLevel,
            EnchantedItemInUse item,
            LivingEntity owner,
            double range,
            Vec3 velocity,
            double incomingDamageMultiplier
    ) {
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }

        Identifier enchantmentId = enchantment.unwrapKey()
                .map(ResourceKey::identifier)
                .orElseThrow(() -> new IllegalStateException("Cannot start lunge for enchantment holder without a registry key"));
        double speedPerTick = Math.max(0.25D, velocity.length());
        Activation previous = levelState(level).remove(owner.getUUID());
        if (previous != null) {
            previous.deactivate(level);
        }

        Vec3 direction = velocity.normalize();
        if (direction.lengthSqr() <= 1.0E-6D) {
            direction = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot()).normalize();
        }

        Activation activation = new Activation(
                owner.getUUID(),
                enchantmentLevel,
                enchantmentId,
                item.itemStack().copy(),
                item.inSlot(),
                direction,
                owner.position(),
                Math.max(0.0D, range),
                speedPerTick,
                Math.max(0.0D, incomingDamageMultiplier),
                Math.max(1, (int) Math.ceil(range / speedPerTick) + 5),
                owner.getYRot(),
                owner.getXRot()
        );
        owner.addEffect(new MobEffectInstance(VSQMobEffects.LUNGING, activation.ticksRemaining + 1, 0, false, false, false));
        levelState(level).put(owner.getUUID(), activation);
        owner.setDeltaMovement(velocity);
        owner.hurtMarked = true;
        owner.needsSync = true;
        VSQNetworking.sendLungingState(player, true);
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

            Vec3 start = activation.lastPosition;
            Vec3 current = owner.position();
            owner.absSnapRotationTo(activation.yRot, activation.xRot);

            BlockHitResult hitResult = level.clip(new ClipContext(
                    start,
                    current.add(activation.direction.scale(COLLISION_EPSILON)),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    owner
            ));
            boolean hitBlock = hitResult.getType() == HitResult.Type.BLOCK;
            Vec3 end = hitBlock ? hitResult.getLocation() : current;

            double travelled = start.distanceTo(end);
            activation.remainingDistance = Math.max(0.0D, activation.remainingDistance - travelled);
            if (travelled > 0.0D) {
                activation.applyEntityHits(level, owner, start, end);
            }

            boolean blocked = hitBlock || owner.horizontalCollision || (owner.verticalCollision && !owner.onGround());
            if (blocked) {
                owner.setDeltaMovement(Vec3.ZERO);
                activation.deactivate(level);
                expired.add(entry.getKey());
                continue;
            }

            activation.ticksRemaining--;
            if (activation.remainingDistance <= 1.0E-3D || activation.ticksRemaining <= 0) {
                activation.deactivate(level);
                expired.add(entry.getKey());
                continue;
            }

            double nextSpeed = Math.min(activation.speedPerTick, Math.max(MIN_ACTIVE_SPEED, activation.remainingDistance));
            owner.setDeltaMovement(activation.direction.scale(nextSpeed));
            owner.resetFallDistance();
            owner.hurtMarked = true;
            owner.needsSync = true;
            activation.lastPosition = current;
        }

        for (UUID uuid : expired) {
            activations.remove(uuid);
        }
        if (activations.isEmpty()) {
            STATES.remove(level);
        }
    }

    public static boolean isLunging(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        Map<UUID, Activation> activations = STATES.get(level);
        return activations != null && activations.containsKey(entity.getUUID());
    }

    public static float modifyIncomingDamage(LivingEntity entity, float damage) {
        if (damage <= 0.0F || !(entity.level() instanceof ServerLevel level)) {
            return Math.max(damage, 0.0F);
        }

        Activation activation = activation(level, entity.getUUID());
        if (activation == null) {
            return Math.max(damage, 0.0F);
        }

        return Math.max(damage * (float) activation.incomingDamageMultiplier, 0.0F);
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

    private static Activation activation(ServerLevel level, UUID uuid) {
        Map<UUID, Activation> activations = STATES.get(level);
        return activations == null ? null : activations.get(uuid);
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
        private final Vec3 direction;
        private final double speedPerTick;
        private final double incomingDamageMultiplier;
        private int ticksRemaining;
        private final float yRot;
        private final float xRot;
        private final Set<UUID> impactedEntities = new HashSet<>();
        private Vec3 lastPosition;
        private double remainingDistance;

        private Activation(
                UUID ownerId,
                int enchantmentLevel,
                Identifier enchantmentId,
                ItemStack stackSnapshot,
                EquipmentSlot slot,
                Vec3 direction,
                Vec3 lastPosition,
                double remainingDistance,
                double speedPerTick,
                double incomingDamageMultiplier,
                int ticksRemaining,
                float yRot,
                float xRot
        ) {
            this.ownerId = ownerId;
            this.enchantmentLevel = enchantmentLevel;
            this.enchantmentId = enchantmentId;
            this.stackSnapshot = stackSnapshot;
            this.slot = slot;
            this.direction = direction;
            this.lastPosition = lastPosition;
            this.remainingDistance = remainingDistance;
            this.speedPerTick = speedPerTick;
            this.incomingDamageMultiplier = incomingDamageMultiplier;
            this.ticksRemaining = ticksRemaining;
            this.yRot = yRot;
            this.xRot = xRot;
        }

        private LivingEntity resolveOwner(ServerLevel level) {
            Entity entity = level.getEntity(this.ownerId);
            return entity instanceof LivingEntity living ? living : null;
        }

        private Holder<Enchantment> resolveEnchantment(ServerLevel level) {
            return level.registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, this.enchantmentId));
        }

        private void applyEntityHits(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 end) {
            AABB bounds = sweptBounds(owner.getBoundingBox(), start, end);
            List<LivingEntity> touched = level.getEntitiesOfClass(LivingEntity.class, bounds, entity ->
                    entity.isAlive() && entity != owner
            );
            if (touched.isEmpty()) {
                return;
            }

            Holder<Enchantment> enchantment = resolveEnchantment(level);
            EnchantedItemInUse itemInUse = new EnchantedItemInUse(this.stackSnapshot.copy(), this.slot, owner, ignored -> {});
            List<TargetedConditionalEffect<EnchantmentEntityEffect>> effects =
                    VSQEnchantments.profileEffects(this.stackSnapshot, enchantment, EnchantmentEffectComponents.POST_ATTACK);
            for (LivingEntity target : touched) {
                if (!this.impactedEntities.add(target.getUUID())) {
                    continue;
                }

                var damageSource = owner instanceof ServerPlayer player
                        ? owner.damageSources().playerAttack(player)
                        : owner.damageSources().mobAttack(owner);
                var context = Enchantment.damageContext(level, this.enchantmentLevel, target, damageSource);
                for (int index = 0; index < effects.size(); index++) {
                    TargetedConditionalEffect<EnchantmentEntityEffect> effect = effects.get(index);
                    if (!shouldApplyToEnchantedTarget(effect.enchanted())
                            || !effect.matches(context)
                            || !SpecialEnchantmentCooldowns.shouldRunSpecialEffect(level, this.stackSnapshot, enchantment.value(), EnchantmentEffectComponents.POST_ATTACK, index, owner)) {
                        continue;
                    }

                    Entity affected = resolveAffectedEntity(effect, owner, target);
                    if (!shouldApplyEffectTo(affected)) {
                        continue;
                    }
                    ChannelingState.pushExecutionDamageSource(damageSource);
                    try {
                        effect.effect().apply(level, this.enchantmentLevel, itemInUse, affected, affected.position());
                    } finally {
                        ChannelingState.popExecutionDamageSource();
                    }
                }
            }
        }

        private void deactivate(ServerLevel level) {
            LivingEntity owner = this.resolveOwner(level);
            if (owner != null) {
                owner.removeEffect(VSQMobEffects.LUNGING);
                if (owner instanceof ServerPlayer player) {
                    VSQNetworking.sendLungingState(player, false);
                }
            }
        }

        private static Entity resolveAffectedEntity(TargetedConditionalEffect<EnchantmentEntityEffect> effect, LivingEntity owner, LivingEntity target) {
            return LungingState.resolveAffectedEntity(effect.affected(), owner, target);
        }

        private static AABB sweptBounds(AABB currentBounds, Vec3 start, Vec3 end) {
            Vec3 movement = end.subtract(start);
            AABB previousBounds = currentBounds.move(movement.reverse());
            return currentBounds.minmax(previousBounds).inflate(0.2D);
        }
    }

    static Entity resolveAffectedEntity(EnchantmentTarget target, Entity owner, Entity victim) {
        return shouldAffectOwner(target) ? owner : victim;
    }

    static boolean shouldAffectOwner(EnchantmentTarget target) {
        return switch (target) {
            case ATTACKER, DAMAGING_ENTITY -> true;
            case VICTIM -> false;
        };
    }

    static boolean shouldApplyEffectTo(Entity affected) {
        return affected != null;
    }

    static boolean shouldApplyToEnchantedTarget(EnchantmentTarget target) {
        return target == EnchantmentTarget.ATTACKER || target == EnchantmentTarget.DAMAGING_ENTITY;
    }
}
