package blob.vanillasquared.main.world.item.enchantment.effects;

import blob.vanillasquared.main.world.effect.LungingState;
import blob.vanillasquared.main.VanillaSquared;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record VSQBeginLungingEffect(
        Vec3 direction,
        Vec3 coordinateScale,
        LevelBasedValue magnitude,
        double speed,
        LevelBasedValue damageTakenMultiplier,
        LevelBasedValue range
) implements EnchantmentEntityEffect {
    private static final double MAX_RANGE = 7.0D;
    private static final double MIN_SPEED_PER_TICK = 0.25D;
    private static final double MAX_SPEED_PER_TICK = 1.6D;
    private static final double MAX_UPWARD_SPEED = 0.6D;
    private static final double MIN_DOWNWARD_LOOK_Y = -0.35D;

    public static final MapCodec<VSQBeginLungingEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.optionalFieldOf("direction", new Vec3(0.0D, 0.0D, 1.0D)).forGetter(VSQBeginLungingEffect::direction),
            Vec3.CODEC.optionalFieldOf("coordinate_scale", new Vec3(1.0D, 1.0D, 1.0D)).forGetter(VSQBeginLungingEffect::coordinateScale),
            LevelBasedValue.CODEC.fieldOf("magnitude").forGetter(VSQBeginLungingEffect::magnitude),
            Codec.DOUBLE.optionalFieldOf("speed", 1.0D).forGetter(VSQBeginLungingEffect::speed),
            LevelBasedValue.CODEC.optionalFieldOf("damage_taken_multiplier", LevelBasedValue.constant(1.0F)).forGetter(VSQBeginLungingEffect::damageTakenMultiplier),
            LevelBasedValue.CODEC.fieldOf("range").forGetter(VSQBeginLungingEffect::range)
    ).apply(instance, VSQBeginLungingEffect::new));

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
            VanillaSquared.LOGGER.warn("Failed to resolve lunging enchantment holder for {}", item.itemStack().getHoverName().getString());
            return;
        }

        double range = Math.clamp(this.range.calculate(enchantmentLevel), 0.0D, MAX_RANGE);
        Vec3 launchVelocity = this.resolveLaunchVelocity(livingEntity, enchantmentLevel, range);
        if (launchVelocity.lengthSqr() <= 1.0E-6D || range <= 1.0E-6D) {
            return;
        }

        double incomingDamageMultiplier = Math.clamp(this.damageTakenMultiplier.calculate(enchantmentLevel), 0.0D, 10.0D);
        LungingState.start(serverLevel, enchantment, enchantmentLevel, item, livingEntity, range, launchVelocity, incomingDamageMultiplier);
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

    private Vec3 resolveLaunchVelocity(LivingEntity entity, int enchantmentLevel, double range) {
        Vec3 look = entity.getLookAngle();
        Vec3 rawVelocity = look.addLocalCoordinates(this.direction)
                .multiply(this.coordinateScale)
                .scale(this.magnitude.calculate(enchantmentLevel) * this.speed);

        double horizontalLength = Math.sqrt(rawVelocity.x * rawVelocity.x + rawVelocity.z * rawVelocity.z);
        if (horizontalLength <= 1.0E-6D) {
            return Vec3.ZERO;
        }

        double verticalSpeed = rawVelocity.y;
        if (verticalSpeed > 0.0D) {
            verticalSpeed = Math.min(verticalSpeed, MAX_UPWARD_SPEED);
        } else if (look.y > MIN_DOWNWARD_LOOK_Y) {
            verticalSpeed = 0.0D;
        } else {
            verticalSpeed = Math.max(verticalSpeed, -MAX_UPWARD_SPEED);
        }

        Vec3 normalized = new Vec3(rawVelocity.x, verticalSpeed, rawVelocity.z).normalize();
        if (normalized.lengthSqr() <= 1.0E-6D) {
            return Vec3.ZERO;
        }

        double maxSpeedForRange = Math.max(MIN_SPEED_PER_TICK, range / 2.0D);
        double speedPerTick = Math.min(MAX_SPEED_PER_TICK, Math.min(maxSpeedForRange, rawVelocity.length()));
        return normalized.scale(speedPerTick);
    }

    private static final ThreadLocal<Holder<Enchantment>> ACTIVE_ENCHANTMENT = new ThreadLocal<>();
}
