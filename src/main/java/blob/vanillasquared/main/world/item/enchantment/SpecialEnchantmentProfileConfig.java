package blob.vanillasquared.main.world.item.enchantment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.LevelBasedValue;

import java.util.Optional;

public record SpecialEnchantmentProfileConfig(
        LevelBasedValue cooldown,
        Optional<String> displayLimit,
        Optional<String> cooldownAfterLimit
) {
    private static final Codec<LevelBasedValue> COOLDOWN_CODEC = Codec.either(Codec.FLOAT, LevelBasedValue.CODEC).xmap(
            either -> either.map(LevelBasedValue::constant, value -> value),
            value -> value instanceof LevelBasedValue.Constant constant
                    ? Either.left(constant.value())
                    : Either.right(value)
    );

    public static final Codec<SpecialEnchantmentProfileConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            COOLDOWN_CODEC.fieldOf("cooldown").forGetter(SpecialEnchantmentProfileConfig::cooldown),
            Codec.STRING.optionalFieldOf("display_limit").forGetter(SpecialEnchantmentProfileConfig::displayLimit),
            Codec.STRING.optionalFieldOf("cooldown_after_limit").forGetter(SpecialEnchantmentProfileConfig::cooldownAfterLimit)
    ).apply(instance, SpecialEnchantmentProfileConfig::new));

    public long cooldownTicks(int level) {
        return Math.max(0L, Mth.floor(this.cooldown.calculate(level))) * 20L;
    }
}
