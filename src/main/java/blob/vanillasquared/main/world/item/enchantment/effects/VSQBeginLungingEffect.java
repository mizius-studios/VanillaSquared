package blob.vanillasquared.main.world.item.enchantment.effects;

import blob.vanillasquared.main.world.effect.LungingState;
import blob.vanillasquared.main.VanillaSquared;
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

public record VSQBeginLungingEffect(LevelBasedValue range) implements EnchantmentEntityEffect {
    public static final MapCodec<VSQBeginLungingEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
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

        LungingState.start(serverLevel, enchantment, enchantmentLevel, item, livingEntity, Math.clamp(this.range.calculate(enchantmentLevel), 0.0D, 7.0D));
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
}
