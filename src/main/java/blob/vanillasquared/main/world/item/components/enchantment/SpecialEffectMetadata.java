package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record SpecialEffectMetadata(
        String id,
        Optional<SpecialEffectSettings> special
) {
    public static final Codec<SpecialEffectMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(SpecialEffectMetadata::id),
            SpecialEffectSettings.CODEC.optionalFieldOf("special").forGetter(SpecialEffectMetadata::special)
    ).apply(instance, SpecialEffectMetadata::new));
}
