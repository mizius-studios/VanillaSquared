package blob.vanillasquared.main.world.item.components.specialeffect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record SpecialEffectComponent(int cooldown, String cooldownGroup) {
    public static final Codec<SpecialEffectComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("cooldown").forGetter(SpecialEffectComponent::cooldown),
                    Codec.STRING.fieldOf("cooldown_group").forGetter(SpecialEffectComponent::cooldownGroup)
            ).apply(instance, SpecialEffectComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SpecialEffectComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpecialEffectComponent::cooldown,
            ByteBufCodecs.STRING_UTF8, SpecialEffectComponent::cooldownGroup,
            SpecialEffectComponent::new
    );

    public SpecialEffectComponent {
        cooldown = Math.max(0, cooldown);
        cooldownGroup = Objects.requireNonNull(cooldownGroup, "cooldownGroup").trim();
    }
}
