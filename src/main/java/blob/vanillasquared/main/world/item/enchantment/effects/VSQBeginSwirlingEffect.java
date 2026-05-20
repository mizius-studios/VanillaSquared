package blob.vanillasquared.main.world.item.enchantment.effects;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.effect.SwirlingState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record VSQBeginSwirlingEffect(
        LevelBasedValue duration,
        LevelBasedValue warmupDuration,
        LevelBasedValue radius,
        int hitInterval,
        Damage damage
) implements EnchantmentEntityEffect {
    private static final LevelBasedValue DEFAULT_WARMUP_DURATION = LevelBasedValue.constant(0.35F);

    public static final MapCodec<VSQBeginSwirlingEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.fieldOf("duration").forGetter(VSQBeginSwirlingEffect::duration),
            LevelBasedValue.CODEC.optionalFieldOf("warmup_duration", DEFAULT_WARMUP_DURATION).forGetter(VSQBeginSwirlingEffect::warmupDuration),
            LevelBasedValue.CODEC.fieldOf("radius").forGetter(VSQBeginSwirlingEffect::radius),
            Codec.INT.optionalFieldOf("hit_interval", 4).forGetter(VSQBeginSwirlingEffect::hitInterval),
            Damage.CODEC.fieldOf("damage").forGetter(VSQBeginSwirlingEffect::damage)
    ).apply(instance, VSQBeginSwirlingEffect::new));

    @Override
    public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position) {
        this.applyWithEnchantment(serverLevel, enchantmentLevel, item, entity, position, activeEnchantment());
    }

    public void applyWithEnchantment(
            ServerLevel serverLevel,
            int enchantmentLevel,
            EnchantedItemInUse item,
            Entity entity,
            Vec3 position,
            Holder<Enchantment> enchantment
    ) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (enchantment == null) {
            VanillaSquared.LOGGER.warn("Failed to resolve swirling enchantment holder for {}", item.itemStack().getHoverName().getString());
            return;
        }

        int durationTicks = Math.max(1, Math.round(this.duration.calculate(enchantmentLevel) * 20.0F));
        int warmupTicks = Math.max(0, Math.round(this.warmupDuration.calculate(enchantmentLevel) * 20.0F));
        double resolvedRadius = Math.clamp(this.radius.calculate(enchantmentLevel), 0.0D, 16.0D);
        int resolvedHitInterval = Math.max(1, this.hitInterval);
        float resolvedDamage = Math.max(0.0F, this.damage.amount());
        if (resolvedRadius <= 1.0E-6D || resolvedDamage <= 0) {
            return;
        }

        SwirlingState.start(serverLevel, enchantment, enchantmentLevel, item, livingEntity, durationTicks, warmupTicks, resolvedRadius, resolvedHitInterval, this.damage.damageType(), resolvedDamage);
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return MAP_CODEC;
    }

    public static void runWithActiveEnchantment(Holder<Enchantment> enchantment, Runnable action) {
        Holder<Enchantment> previous = ACTIVE_ENCHANTMENT.get();
        ACTIVE_ENCHANTMENT.set(enchantment);
        try {
            action.run();
        } finally {
            ACTIVE_ENCHANTMENT.set(previous);
        }
    }

    public static Holder<Enchantment> activeEnchantment() {
        return ACTIVE_ENCHANTMENT.get();
    }

    private static final ThreadLocal<Holder<Enchantment>> ACTIVE_ENCHANTMENT = new ThreadLocal<>();

    public record Damage(ResourceKey<DamageType> damageType, float amount) {
        public static final Codec<Damage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DAMAGE_TYPE).fieldOf("damage_type").forGetter(Damage::damageType),
                Codec.FLOAT.fieldOf("amount").forGetter(Damage::amount)
        ).apply(instance, Damage::new));
    }
}
