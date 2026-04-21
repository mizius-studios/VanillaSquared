package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
public record SpecialEffectSettings(
        int limit
) {
    public static final Codec<SpecialEffectSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("limit", 1).forGetter(SpecialEffectSettings::limit)
    ).apply(instance, SpecialEffectSettings::new));
}
