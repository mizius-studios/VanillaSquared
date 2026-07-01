package blob.vanillasquared.main.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record SpecialEffectMetadata(
        String effectId,
        Optional<SpecialEffectSettings> special
) {
    public static final Codec<SpecialEffectMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("effect_id").forGetter(SpecialEffectMetadata::effectId),
            SpecialEffectSettings.CODEC.optionalFieldOf("special").forGetter(SpecialEffectMetadata::special)
    ).apply(instance, SpecialEffectMetadata::new));
}
