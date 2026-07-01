package blob.vanillasquared.main.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.enchantment.LevelBasedValue;

public record SpecialEffectSettings(
        LevelBasedValue limit
) {
    public static final MapCodec<SpecialEffectSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LevelBasedValue.CODEC.optionalFieldOf("limit", LevelBasedValue.constant(1.0F)).forGetter(SpecialEffectSettings::limit)
    ).apply(instance, SpecialEffectSettings::new));

    public static final Codec<SpecialEffectSettings> CODEC = MAP_CODEC.codec();
}
