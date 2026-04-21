package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record SpecialEnchantmentProfileConfig(
        int cooldown,
        Optional<String> displayLimit,
        Optional<String> cooldownAfterLimit
) {
    public static final Codec<SpecialEnchantmentProfileConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("cooldown").forGetter(SpecialEnchantmentProfileConfig::cooldown),
            Codec.STRING.optionalFieldOf("display_limit").forGetter(SpecialEnchantmentProfileConfig::displayLimit),
            Codec.STRING.optionalFieldOf("cooldown_after_limit").forGetter(SpecialEnchantmentProfileConfig::cooldownAfterLimit)
    ).apply(instance, SpecialEnchantmentProfileConfig::new));

    public long cooldownTicks() {
        return Math.max(0, this.cooldown) * 20L;
    }
}
