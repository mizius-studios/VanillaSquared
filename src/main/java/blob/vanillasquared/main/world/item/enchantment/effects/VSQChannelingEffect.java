package blob.vanillasquared.main.world.item.enchantment.effects;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.effect.ChannelingState;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.util.api.references.RegistryReference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.TargetedConditionalEffect;
import net.minecraft.world.item.enchantment.effects.AllOf;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record VSQChannelingEffect(
        EntityPredicate algorithm,
        LevelBasedValue targetLimit,
        LevelBasedValue blockLimit,
        Optional<Identifier> particlePath,
        LevelBasedValue duration,
        RegistryReference passThrough
) implements EnchantmentEntityEffect {
    private static final Identifier DEFAULT_PARTICLE_PATH = Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "lightning");
    private static final LevelBasedValue DEFAULT_BLOCK_LIMIT = LevelBasedValue.constant(16.0F);
    private static final Codec<RegistryReference> BLOCK_REFERENCE_CODEC = RegistryReference.CODEC.comapFlatMap(
            reference -> {
                if (!reference.tag() && !BuiltInRegistries.BLOCK.containsKey(reference.id())) {
                    return DataResult.error(() -> "Unknown block id for channeling pass_through: " + reference.asString());
                }
                return DataResult.success(reference);
            },
            reference -> reference
    );

    public static final MapCodec<VSQChannelingEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntityPredicate.CODEC.fieldOf("algorithm").forGetter(VSQChannelingEffect::algorithm),
            LevelBasedValue.CODEC.fieldOf("target_limit").forGetter(VSQChannelingEffect::targetLimit),
            LevelBasedValue.CODEC.optionalFieldOf("block_limit", DEFAULT_BLOCK_LIMIT).forGetter(VSQChannelingEffect::blockLimit),
            Identifier.CODEC.optionalFieldOf("particle_path").forGetter(VSQChannelingEffect::particlePath),
            LevelBasedValue.CODEC.fieldOf("duration").forGetter(VSQChannelingEffect::duration),
            BLOCK_REFERENCE_CODEC.fieldOf("pass_through").forGetter(VSQChannelingEffect::passThrough)
    ).apply(instance, VSQChannelingEffect::new));

    public VSQChannelingEffect {
        algorithm = Objects.requireNonNull(algorithm, "algorithm");
        targetLimit = Objects.requireNonNull(targetLimit, "targetLimit");
        blockLimit = Objects.requireNonNull(blockLimit, "blockLimit");
        particlePath = Objects.requireNonNull(particlePath, "particlePath");
        duration = Objects.requireNonNull(duration, "duration");
        passThrough = Objects.requireNonNull(passThrough, "passThrough");
    }

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        if (!(entity instanceof LivingEntity victim) || item.owner() == null) {
            return;
        }

        Holder<Enchantment> enchantment = resolveEnchantment(item, this);
        if (enchantment == null) {
            VanillaSquared.LOGGER.warn("Failed to resolve channeling enchantment holder for {}", item.itemStack().getHoverName().getString());
            return;
        }

        ChannelingState.startActivation(
                level,
                enchantment,
                enchantmentLevel,
                item,
                victim,
                this.algorithm,
                this.targetLimit,
                this.blockLimit,
                this.duration,
                this.particlePath.orElse(DEFAULT_PARTICLE_PATH),
                this.passThrough,
                indexedDirectHitEffects(item.itemStack(), enchantment, this)
        );
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return MAP_CODEC;
    }

    private static Holder<Enchantment> resolveEnchantment(EnchantedItemInUse item, VSQChannelingEffect effect) {
        for (var entry : VSQEnchantmentSlots.aggregate(item.itemStack()).entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            List<TargetedConditionalEffect<EnchantmentEntityEffect>> effects =
                    VSQEnchantmentSlots.profileEffects(item.itemStack(), enchantment, EnchantmentEffectComponents.POST_ATTACK);
            for (TargetedConditionalEffect<EnchantmentEntityEffect> conditional : effects) {
                if (containsEffect(conditional.effect(), effect)) {
                    return enchantment;
                }
            }
        }
        return null;
    }

    public static List<IndexedDirectHitEffect> indexedDirectHitEffects(ItemStack stack, Holder<Enchantment> enchantment, VSQChannelingEffect effect) {
        List<TargetedConditionalEffect<EnchantmentEntityEffect>> effects =
                VSQEnchantmentSlots.profileEffects(stack, enchantment, EnchantmentEffectComponents.POST_ATTACK);
        List<IndexedDirectHitEffect> resolved = new java.util.ArrayList<>(effects.size());
        for (int index = 0; index < effects.size(); index++) {
            int effectIndex = index;
            TargetedConditionalEffect<EnchantmentEntityEffect> conditional = effects.get(index);
            stripCurrentEffect(conditional, effect)
                    .ifPresent(stripped -> resolved.add(new IndexedDirectHitEffect(effectIndex, stripped)));
        }
        return List.copyOf(resolved);
    }

    private static Optional<TargetedConditionalEffect<EnchantmentEntityEffect>> stripCurrentEffect(TargetedConditionalEffect<EnchantmentEntityEffect> conditional, VSQChannelingEffect target) {
        return stripCurrentEffect(conditional.effect(), target)
                .map(effect -> new TargetedConditionalEffect<>(conditional.enchanted(), conditional.affected(), effect, conditional.requirements()));
    }

    private static boolean containsEffect(EnchantmentEntityEffect root, VSQChannelingEffect target) {
        if (root.equals(target)) {
            return true;
        }
        if (root instanceof AllOf.EntityEffects allOf) {
            for (EnchantmentEntityEffect child : allOf.effects()) {
                if (containsEffect(child, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<EnchantmentEntityEffect> stripCurrentEffect(EnchantmentEntityEffect root, VSQChannelingEffect target) {
        if (root.equals(target)) {
            return Optional.empty();
        }
        if (root instanceof AllOf.EntityEffects allOf) {
            List<EnchantmentEntityEffect> stripped = allOf.effects().stream()
                    .map(effect -> stripCurrentEffect(effect, target))
                    .flatMap(Optional::stream)
                    .toList();
            if (stripped.isEmpty()) {
                return Optional.empty();
            }
            if (stripped.size() == 1) {
                return Optional.of(stripped.getFirst());
            }
            return Optional.of(new AllOf.EntityEffects(stripped));
        }
        return Optional.of(root);
    }

    public record IndexedDirectHitEffect(
            int index,
            TargetedConditionalEffect<EnchantmentEntityEffect> effect
    ) {
    }
}
