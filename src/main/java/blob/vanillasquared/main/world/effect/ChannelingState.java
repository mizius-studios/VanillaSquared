package blob.vanillasquared.main.world.effect;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.item.enchantment.SpecialEnchantmentCooldowns;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentEffectComponents;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.main.world.item.enchantment.effects.VSQChannelingEffect;
import blob.vanillasquared.main.world.particle.particles.LightningBoltParticleOptions;
import blob.vanillasquared.util.api.references.RegistryReference;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.TargetedConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ChannelingState {
    private static final WeakHashMap<ServerLevel, LevelState> STATES = new WeakHashMap<>();
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final ThreadLocal<DamageSource> EXECUTION_DAMAGE_SOURCE = new ThreadLocal<>();
    private static final double LINE_SAMPLE_STEP = 0.25;
    private static final double OCCUPANCY_SAMPLE_STEP = 0.45;

    private ChannelingState() {
    }

    public static void pushExecutionDamageSource(DamageSource damageSource) {
        EXECUTION_DAMAGE_SOURCE.set(damageSource);
    }

    public static void popExecutionDamageSource() {
        EXECUTION_DAMAGE_SOURCE.remove();
    }

    public static DamageSource currentExecutionDamageSource() {
        return EXECUTION_DAMAGE_SOURCE.get();
    }

    public static void startActivation(
            ServerLevel level,
            Holder<Enchantment> enchantment,
            int enchantmentLevel,
            EnchantedItemInUse item,
            LivingEntity originalVictim,
            EntityPredicate algorithm,
            LevelBasedValue targetLimit,
            LevelBasedValue blockLimit,
            LevelBasedValue duration,
            Identifier particlePath,
            RegistryReference passThrough,
            List<VSQChannelingEffect.IndexedDirectHitEffect> directHitEffects
    ) {
        assert item.owner() != null;
        Activation activation = new Activation(
                NEXT_ID.incrementAndGet(),
                enchantment,
                enchantmentLevel,
                item.itemStack().copy(),
                item.inSlot(),
                item.owner().getUUID(),
                originalVictim.getUUID(),
                resolveDirectAttackerUuid(),
                Math.max(0, Math.round(targetLimit.calculate(enchantmentLevel))),
                Math.max(1.0, blockLimit.calculate(enchantmentLevel)),
                Math.max(1, Math.round(duration.calculate(enchantmentLevel))),
                particlePath,
                passThrough,
                algorithm,
                directHitEffects,
                VSQEnchantmentSlots.profileEffects(item.itemStack(), enchantment, VSQEnchantmentEffectComponents.CHANNELING_PATH)
        );
        applyDirectHitEffects(level, activation, originalVictim);
        chainFrom(level, activation, originalVictim);
        if (!activation.segments.isEmpty()) {
            tickActivation(level, activation);
        }
        STATES.computeIfAbsent(level, ignored -> new LevelState()).activations.put(activation.id, activation);
    }

    public static void tick(ServerLevel level) {
        LevelState levelState = STATES.get(level);
        if (levelState == null || levelState.activations.isEmpty()) {
            return;
        }

        List<Long> expired = new ArrayList<>();
        for (Activation activation : levelState.activations.values()) {
            LivingEntity owner = activation.resolveOwner(level);
            if (owner == null || !owner.isAlive()) {
                deactivateActivation(level, activation);
                expired.add(activation.id);
                continue;
            }

            activation.segments.removeIf(segment -> segment.expiresAt <= level.getGameTime());
            if (activation.segments.isEmpty()) {
                deactivateActivation(level, activation);
                expired.add(activation.id);
                continue;
            }

            tickActivation(level, activation);
        }

        for (Long id : expired) {
            levelState.activations.remove(id);
        }
    }

    private static void tickActivation(ServerLevel level, Activation activation) {
        Set<BlockPos> occupiedBlocks = new LinkedHashSet<>();
        Map<UUID, LivingEntity> occupiedEntities = new HashMap<>();

        for (Segment segment : activation.segments) {
            occupiedBlocks.addAll(segment.blocks);
            spawnPathParticles(level, activation, segment);
            for (LivingEntity entity : findIntersectingEntities(level, segment)) {
                occupiedEntities.putIfAbsent(entity.getUUID(), entity);
            }
        }

        applyBlockEffects(level, activation, occupiedBlocks);
        applyEntityEffects(level, activation, occupiedEntities.values());
    }

    private static void applyBlockEffects(ServerLevel level, Activation activation, Set<BlockPos> occupiedBlocks) {
        if (activation.pathEffects.isEmpty()) {
            return;
        }

        Entity contextVictim = activation.resolveOriginalVictim(level);
        if (contextVictim == null) {
            contextVictim = activation.resolveOwner(level);
        }
        if (contextVictim == null) {
            return;
        }

        DamageSource damageSource = activation.createDamageSource(level, contextVictim);
        Map<BlockPos, Set<Integer>> nextActive = new HashMap<>();
        for (BlockPos pos : occupiedBlocks) {
            Vec3 center = Vec3.atCenterOf(pos);
            Set<Integer> activeIndices = activation.activeBlockEffects.getOrDefault(pos, Set.of());
            Set<Integer> nextIndices = new HashSet<>();
            for (int index = 0; index < activation.pathEffects.size(); index++) {
                TargetedConditionalEffect<EnchantmentLocationBasedEffect> effect = activation.pathEffects.get(index);
                if (!effect.matches(Enchantment.damageContext(level, activation.enchantmentLevel, contextVictim, damageSource))) {
                    continue;
                }
                Entity affected = activation.resolveAffectedEntity(level, effect.affected(), contextVictim);
                if (affected == null) {
                    continue;
                }

                boolean becameActive = !activeIndices.contains(index);
                if (becameActive && !activation.shouldRunSpecialPathEffect(level, index)) {
                    continue;
                }

                effect.effect().onChangedBlock(level, activation.enchantmentLevel, activation.createItemInUse(level), affected, center, becameActive);
                nextIndices.add(index);
            }

            deactivateRemovedBlockEffects(level, activation, pos, center, contextVictim, activeIndices, nextIndices);
            if (!nextIndices.isEmpty()) {
                nextActive.put(pos.immutable(), nextIndices);
            }
        }

        for (Map.Entry<BlockPos, Set<Integer>> entry : activation.activeBlockEffects.entrySet()) {
            if (nextActive.containsKey(entry.getKey())) {
                continue;
            }
            deactivateAllBlockEffects(level, activation, entry.getKey(), contextVictim, entry.getValue());
        }
        activation.activeBlockEffects.clear();
        activation.activeBlockEffects.putAll(nextActive);
    }

    private static void applyEntityEffects(ServerLevel level, Activation activation, Collection<LivingEntity> occupiedEntities) {
        Object2IntArrayMap<UUID> stillActive = new Object2IntArrayMap<>();
        for (LivingEntity occupant : occupiedEntities) {
            if (!occupant.isAlive()) {
                continue;
            }

            DamageSource damageSource = activation.createDamageSource(level, occupant);
            Set<Integer> activeIndices = activation.activeEntityEffects.computeIfAbsent(occupant.getUUID(), ignored -> new HashSet<>());
            Set<Integer> nextIndices = new HashSet<>();
            for (int index = 0; index < activation.pathEffects.size(); index++) {
                TargetedConditionalEffect<EnchantmentLocationBasedEffect> effect = activation.pathEffects.get(index);
                if (!effect.matches(Enchantment.damageContext(level, activation.enchantmentLevel, occupant, damageSource))) {
                    continue;
                }

                Entity affected = activation.resolveAffectedEntity(level, effect.affected(), occupant);
                if (affected == null) {
                    continue;
                }

                boolean becameActive = !activeIndices.contains(index);
                if (becameActive && !activation.shouldRunSpecialPathEffect(level, index)) {
                    continue;
                }
                effect.effect().onChangedBlock(level, activation.enchantmentLevel, activation.createItemInUse(level), affected, occupant.position(), becameActive);
                nextIndices.add(index);
            }

            for (Integer previousIndex : Set.copyOf(activeIndices)) {
                if (!nextIndices.contains(previousIndex)) {
                    TargetedConditionalEffect<EnchantmentLocationBasedEffect> effect = activation.pathEffects.get(previousIndex);
                    effect.effect().onDeactivated(activation.createItemInUse(level), occupant, occupant.position(), activation.enchantmentLevel);
                    activeIndices.remove(previousIndex);
                }
            }

            activeIndices.addAll(nextIndices);
            if (!activeIndices.isEmpty()) {
                stillActive.put(occupant.getUUID(), 1);
            }
        }

        for (UUID entityId : Set.copyOf(activation.activeEntityEffects.keySet())) {
            if (stillActive.containsKey(entityId)) {
                continue;
            }

            LivingEntity entity = activation.resolveLiving(level, entityId);
            if (entity != null) {
                for (Integer activeIndex : activation.activeEntityEffects.get(entityId)) {
                    TargetedConditionalEffect<EnchantmentLocationBasedEffect> effect = activation.pathEffects.get(activeIndex);
                    effect.effect().onDeactivated(activation.createItemInUse(level), entity, entity.position(), activation.enchantmentLevel);
                }
            }
            activation.activeEntityEffects.remove(entityId);
        }
    }

    private static void deactivateActivation(ServerLevel level, Activation activation) {
        for (Map.Entry<UUID, Set<Integer>> entry : activation.activeEntityEffects.entrySet()) {
            LivingEntity entity = activation.resolveLiving(level, entry.getKey());
            if (entity == null) {
                continue;
            }
            for (Integer index : entry.getValue()) {
                activation.pathEffects.get(index).effect().onDeactivated(activation.createItemInUse(level), entity, entity.position(), activation.enchantmentLevel);
            }
        }
        activation.activeEntityEffects.clear();

        Entity contextVictim = activation.resolveOriginalVictim(level);
        if (contextVictim == null) {
            contextVictim = activation.resolveOwner(level);
        }
        if (contextVictim != null) {
            for (Map.Entry<BlockPos, Set<Integer>> entry : activation.activeBlockEffects.entrySet()) {
                deactivateAllBlockEffects(level, activation, entry.getKey(), contextVictim, entry.getValue());
            }
        }
        activation.activeBlockEffects.clear();
    }

    private static void chainFrom(ServerLevel level, Activation activation, LivingEntity sourceVictim) {
        while (activation.remainingTargets > 0) {
            Candidate candidate = findNextCandidate(level, activation, sourceVictim);
            if (candidate == null) {
                return;
            }

            activation.segments.add(candidate.segment);
            if (candidate.target() != null) {
                activation.remainingTargets--;
                applyDirectHitEffects(level, activation, candidate.target());
                sourceVictim = candidate.target();
                continue;
            }

            activation.remainingTargets--;
            strikeLightningAt(level, candidate.rodPosition());
            return;
        }
    }

    private static void applyDirectHitEffects(ServerLevel level, Activation activation, LivingEntity victim) {
        DamageSource damageSource = activation.createDamageSource(level, victim);
        var context = Enchantment.damageContext(level, activation.enchantmentLevel, victim, damageSource);
        for (VSQChannelingEffect.IndexedDirectHitEffect indexedEffect : activation.directHitEffects) {
            TargetedConditionalEffect<EnchantmentEntityEffect> effect = indexedEffect.effect();
            if (!effect.matches(context) || !activation.shouldRunSpecialDirectHitEffect(level, indexedEffect.index())) {
                continue;
            }
            Entity affected = activation.resolveAffectedEntity(level, effect.affected(), victim);
            if (affected == null) {
                continue;
            }
            pushExecutionDamageSource(damageSource);
            try {
                effect.effect().apply(level, activation.enchantmentLevel, activation.createItemInUse(level), affected, affected.position());
            } finally {
                popExecutionDamageSource();
            }
        }
    }

    private static Candidate findNextCandidate(ServerLevel level, Activation activation, LivingEntity sourceVictim) {
        Candidate rodCandidate = findLightningRodCandidate(level, activation, sourceVictim);
        if (rodCandidate != null) {
            return rodCandidate;
        }

        double searchRadius = activation.blockLimit;
        AABB searchBounds = sourceVictim.getBoundingBox().inflate(searchRadius);
        List<LivingEntity> candidates = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, searchBounds, entity -> entity.isAlive() && entity != sourceVictim));
        candidates.sort((left, right) -> Double.compare(left.distanceToSqr(sourceVictim), right.distanceToSqr(sourceVictim)));
        Candidate best = null;
        Vec3 sourceAnchor = channelAnchor(sourceVictim);

        for (LivingEntity candidate : candidates) {
            if (isExcluded(level, activation, sourceVictim, candidate)) {
                continue;
            }
            if (!activation.algorithm.matches(level, sourceVictim.position(), candidate)) {
                continue;
            }

            Vec3 targetPos = channelAnchor(candidate);
            if (sourceAnchor.distanceToSqr(targetPos) > activation.blockLimit * activation.blockLimit) {
                continue;
            }

            List<BlockPos> blocks = tracePath(level, sourceAnchor, targetPos, activation.passThrough, null);
            if (blocks == null) {
                continue;
            }

            long expiresAt = level.getGameTime() + activation.durationTicks;
            Candidate resolved = new Candidate(
                    candidate,
                    null,
                    new Segment(sourceVictim.getUUID(), candidate.getUUID(), null, sourceAnchor, targetPos, expiresAt, List.copyOf(blocks)),
                    sourceAnchor.distanceToSqr(targetPos)
            );
            if (best == null || resolved.distanceSqr() < best.distanceSqr()) {
                best = resolved;
            }
        }
        return best;
    }

    private static Candidate findLightningRodCandidate(ServerLevel level, Activation activation, LivingEntity sourceVictim) {
        int limit = (int) Math.ceil(activation.blockLimit);
        BlockPos sourcePos = sourceVictim.blockPosition();
        Candidate best = null;
        Vec3 sourceAnchor = channelAnchor(sourceVictim);

        for (BlockPos pos : BlockPos.betweenClosed(sourcePos.offset(-limit, -limit, -limit), sourcePos.offset(limit, limit, limit))) {
            BlockPos immutablePos = pos.immutable();
            BlockState state = level.getBlockState(immutablePos);
            if (!(state.getBlock() instanceof LightningRodBlock)) {
                continue;
            }
            if (activation.hasActiveChannelRod(immutablePos)) {
                continue;
            }

            Vec3 targetPos = lightningRodAnchor(immutablePos, state);
            double distanceSqr = sourceAnchor.distanceToSqr(targetPos);
            if (distanceSqr > activation.blockLimit * activation.blockLimit) {
                continue;
            }

            List<BlockPos> blocks = tracePath(level, sourceAnchor, targetPos, activation.passThrough, immutablePos);
            if (blocks == null) {
                continue;
            }

            long expiresAt = level.getGameTime() + activation.durationTicks;
            Candidate candidate = new Candidate(
                    null,
                    immutablePos,
                    new Segment(sourceVictim.getUUID(), null, immutablePos, sourceAnchor, targetPos, expiresAt, List.copyOf(blocks)),
                    distanceSqr
            );
            if (best == null || candidate.distanceSqr() < best.distanceSqr()) {
                best = candidate;
            }
        }

        return best;
    }

    private static boolean isExcluded(ServerLevel level, Activation activation, LivingEntity sourceVictim, LivingEntity candidate) {
        if (candidate == sourceVictim) {
            return true;
        }
        if (activation.hasActiveChannelEntity(candidate.getUUID())) {
            return true;
        }
        LivingEntity owner = activation.resolveOwner(level);
        if (owner != null && candidate.getUUID().equals(owner.getUUID())) {
            return true;
        }
        Entity direct = activation.resolveDirectAttacker(level);
        return direct != null && candidate.getUUID().equals(direct.getUUID());
    }

    private static Vec3 channelAnchor(LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        return new Vec3(entity.getX(), box.minY + box.getYsize() * 0.65D, entity.getZ());
    }

    private static Vec3 lightningRodAnchor(BlockPos pos, BlockState state) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (state.getBlock() instanceof LightningRodBlock rod) {
            var facing = state.getValue(LightningRodBlock.FACING);
            Vec3 offset = Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(0.45D);
            return center.add(offset);
        }
        return center;
    }

    private static List<BlockPos> tracePath(ServerLevel level, Vec3 start, Vec3 end, RegistryReference passThrough, BlockPos terminalBlock) {
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance <= 1.0E-4) {
            return List.of();
        }

        int steps = Math.max(1, (int) Math.ceil(distance / LINE_SAMPLE_STEP));
        Set<BlockPos> blocks = new LinkedHashSet<>();
        for (int step = 0; step <= steps; step++) {
            double t = (double) step / (double) steps;
            Vec3 point = start.add(delta.scale(t));
            BlockPos pos = BlockPos.containing(point);
            if (!blocks.add(pos.immutable())) {
                continue;
            }
            if (step == 0 || step == steps) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (terminalBlock != null && terminalBlock.equals(pos)) {
                continue;
            }
            if (!canPassThrough(state, passThrough)) {
                return null;
            }
        }
        return List.copyOf(blocks);
    }

    private static boolean canPassThrough(BlockState state, RegistryReference passThrough) {
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty() && !state.blocksMotion()) {
            return true;
        }
        if (passThrough.tag()) {
            return state.is(passThrough.tagKey(Registries.BLOCK));
        }
        Block block = BuiltInRegistries.BLOCK.getValue(passThrough.id());
        return state.is(block);
    }

    private static List<LivingEntity> findIntersectingEntities(ServerLevel level, Segment segment) {
        AABB bounds = segment.bounds.inflate(0.75);
        return level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> intersectsSegment(entity.getBoundingBox(), segment.start, segment.end));
    }

    private static boolean intersectsSegment(AABB box, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        if (distance <= 1.0E-4) {
            return box.contains(start);
        }
        int steps = Math.max(1, (int) Math.ceil(distance / OCCUPANCY_SAMPLE_STEP));
        AABB expanded = box.inflate(0.2);
        for (int step = 0; step <= steps; step++) {
            double t = (double) step / (double) steps;
            if (expanded.contains(start.add(delta.scale(t)))) {
                return true;
            }
        }
        return false;
    }

    private static void spawnPathParticles(ServerLevel level, Activation activation, Segment segment) {
        if (activation.particlePath.equals(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "lightning"))) {
            int variant = (int) (activation.id % 4L);
            level.sendParticles(
                    new LightningBoltParticleOptions(segment.yaw, segment.pitch, variant, (float) segment.length),
                    segment.midpoint.x,
                    segment.midpoint.y,
                    segment.midpoint.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
            return;
        }

        ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(activation.particlePath);
        if (particleType instanceof SimpleParticleType simpleParticleType) {
            for (BlockPos pos : segment.blocks) {
                Vec3 center = Vec3.atCenterOf(pos);
                level.sendParticles(simpleParticleType, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void strikeLightningAt(ServerLevel level, BlockPos pos) {
        strikeLightningAt(level, Vec3.atCenterOf(pos));
    }

    private static void strikeLightningAt(ServerLevel level, Vec3 position) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (bolt == null) {
            return;
        }
        bolt.snapTo(position);
        level.addFreshEntity(bolt);
    }

    private static UUID resolveDirectAttackerUuid() {
        DamageSource source = EXECUTION_DAMAGE_SOURCE.get();
        return source == null || source.getDirectEntity() == null ? null : source.getDirectEntity().getUUID();
    }

    private static void deactivateRemovedBlockEffects(
            ServerLevel level,
            Activation activation,
            BlockPos pos,
            Vec3 center,
            Entity contextVictim,
            Set<Integer> activeIndices,
            Set<Integer> nextIndices
    ) {
        for (Integer previousIndex : activeIndices) {
            if (nextIndices.contains(previousIndex)) {
                continue;
            }
            TargetedConditionalEffect<EnchantmentLocationBasedEffect> effect = activation.pathEffects.get(previousIndex);
            Entity affected = activation.resolveAffectedEntity(level, effect.affected(), contextVictim);
            if (affected != null) {
                effect.effect().onDeactivated(activation.createItemInUse(level), affected, center, activation.enchantmentLevel);
            }
        }
    }

    private static void deactivateAllBlockEffects(
            ServerLevel level,
            Activation activation,
            BlockPos pos,
            Entity contextVictim,
            Set<Integer> activeIndices
    ) {
        if (activeIndices == null || activeIndices.isEmpty()) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        for (Integer index : activeIndices) {
            TargetedConditionalEffect<EnchantmentLocationBasedEffect> effect = activation.pathEffects.get(index);
            Entity affected = activation.resolveAffectedEntity(level, effect.affected(), contextVictim);
            if (affected != null) {
                effect.effect().onDeactivated(activation.createItemInUse(level), affected, center, activation.enchantmentLevel);
            }
        }
    }

    private static final class LevelState {
        private final Map<Long, Activation> activations = new HashMap<>();
    }

    private record Candidate(LivingEntity target, BlockPos rodPosition, Segment segment, double distanceSqr) {
    }

    private static final class Activation {
        private final long id;
        private final Holder<Enchantment> enchantment;
        private final int enchantmentLevel;
        private final ItemStack stackSnapshot;
        private final EquipmentSlot slot;
        private final UUID ownerId;
        private final UUID originalVictimId;
        private final UUID directAttackerId;
        private int remainingTargets;
        private final double blockLimit;
        private final int durationTicks;
        private final Identifier particlePath;
        private final RegistryReference passThrough;
        private final EntityPredicate algorithm;
        private final List<VSQChannelingEffect.IndexedDirectHitEffect> directHitEffects;
        private final List<TargetedConditionalEffect<EnchantmentLocationBasedEffect>> pathEffects;
        private final List<Segment> segments = new ArrayList<>();
        private final Map<UUID, Set<Integer>> activeEntityEffects = new HashMap<>();
        private final Map<BlockPos, Set<Integer>> activeBlockEffects = new HashMap<>();

        private Activation(
                long id,
                Holder<Enchantment> enchantment,
                int enchantmentLevel,
                ItemStack stackSnapshot,
                EquipmentSlot slot,
                UUID ownerId,
                UUID originalVictimId,
                UUID directAttackerId,
                int remainingTargets,
                double blockLimit,
                int durationTicks,
                Identifier particlePath,
                RegistryReference passThrough,
                EntityPredicate algorithm,
                List<VSQChannelingEffect.IndexedDirectHitEffect> directHitEffects,
                List<TargetedConditionalEffect<EnchantmentLocationBasedEffect>> pathEffects
        ) {
            this.id = id;
            this.enchantment = enchantment;
            this.enchantmentLevel = enchantmentLevel;
            this.stackSnapshot = stackSnapshot;
            this.slot = slot;
            this.ownerId = ownerId;
            this.originalVictimId = originalVictimId;
            this.directAttackerId = directAttackerId;
            this.remainingTargets = remainingTargets;
            this.blockLimit = blockLimit;
            this.durationTicks = durationTicks;
            this.particlePath = particlePath;
            this.passThrough = passThrough;
            this.algorithm = algorithm;
            this.directHitEffects = List.copyOf(directHitEffects);
            this.pathEffects = List.copyOf(pathEffects);
        }

        private EnchantedItemInUse createItemInUse(ServerLevel level) {
            LivingEntity owner = this.resolveOwner(level);
            return new EnchantedItemInUse(this.stackSnapshot.copy(), this.slot, owner, item -> {});
        }

        private LivingEntity resolveOwner(ServerLevel level) {
            return this.resolveLiving(level, this.ownerId);
        }

        private LivingEntity resolveOriginalVictim(ServerLevel level) {
            return this.resolveLiving(level, this.originalVictimId);
        }

        private LivingEntity resolveLiving(ServerLevel level, UUID uuid) {
            Entity entity = level.getEntity(uuid);
            return entity instanceof LivingEntity living ? living : null;
        }

        private Entity resolveDirectAttacker(ServerLevel level) {
            return this.directAttackerId == null ? null : level.getEntity(this.directAttackerId);
        }

        private Entity resolveAffectedEntity(ServerLevel level, EnchantmentTarget target, Entity victim) {
            return switch (target) {
                case VICTIM -> victim;
                case ATTACKER -> this.resolveOwner(level);
                case DAMAGING_ENTITY -> {
                    Entity direct = this.resolveDirectAttacker(level);
                    yield direct != null ? direct : this.resolveOwner(level);
                }
            };
        }

        private DamageSource createDamageSource(ServerLevel level, Entity victim) {
            Entity directAttacker = this.resolveDirectAttacker(level);
            LivingEntity owner = this.resolveOwner(level);
            if (directAttacker != null) {
                return victim.damageSources().trident(directAttacker, owner);
            }
            if (owner instanceof ServerPlayer player) {
                return victim.damageSources().playerAttack(player);
            }
            if (owner != null) {
                return victim.damageSources().mobAttack(owner);
            }
            return victim.damageSources().magic();
        }

        private boolean hasActiveChannelEntity(UUID entityId) {
            for (Segment segment : this.segments) {
                if (entityId.equals(segment.sourceEntityId) || entityId.equals(segment.targetEntityId)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasActiveChannelRod(BlockPos pos) {
            for (Segment segment : this.segments) {
                if (pos.equals(segment.targetRodPos)) {
                    return true;
                }
            }
            return false;
        }

        private boolean shouldRunSpecialDirectHitEffect(ServerLevel level, int effectIndex) {
            LivingEntity owner = this.resolveOwner(level);
            return owner == null || SpecialEnchantmentCooldowns.shouldRunSpecialEffect(
                    level,
                    this.stackSnapshot,
                    this.enchantment.value(),
                    EnchantmentEffectComponents.POST_ATTACK,
                    effectIndex,
                    owner
            );
        }

        private boolean shouldRunSpecialPathEffect(ServerLevel level, int effectIndex) {
            LivingEntity owner = this.resolveOwner(level);
            return owner == null || SpecialEnchantmentCooldowns.shouldRunSpecialEffect(
                    level,
                    this.stackSnapshot,
                    this.enchantment.value(),
                    VSQEnchantmentEffectComponents.CHANNELING_PATH,
                    effectIndex,
                    owner
            );
        }
    }

    private static final class Segment {
        private final UUID sourceEntityId;
        private final UUID targetEntityId;
        private final BlockPos targetRodPos;
        private final Vec3 start;
        private final Vec3 end;
        private final long expiresAt;
        private final List<BlockPos> blocks;
        private final AABB bounds;
        private final Vec3 midpoint;
        private final double length;
        private final float yaw;
        private final float pitch;

        private Segment(UUID sourceEntityId, UUID targetEntityId, BlockPos targetRodPos, Vec3 start, Vec3 end, long expiresAt, List<BlockPos> blocks) {
            this.sourceEntityId = sourceEntityId;
            this.targetEntityId = targetEntityId;
            this.targetRodPos = targetRodPos;
            this.start = start;
            this.end = end;
            this.expiresAt = expiresAt;
            this.blocks = blocks;
            this.bounds = new AABB(start, end);
            this.midpoint = start.add(end).scale(0.5);
            Vec3 delta = end.subtract(start);
            this.length = delta.length();
            Vec3 direction = delta.normalize();
            this.yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
            this.pitch = (float) Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        }
    }
}
