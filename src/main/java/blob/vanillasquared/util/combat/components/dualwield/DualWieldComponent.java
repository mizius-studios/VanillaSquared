package blob.vanillasquared.util.combat.components.dualwield;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

public record DualWieldComponent(
        List<String> identifiers,
        int cooldown,
        int criticalHits,
        Identifier blockedEnchantmentsTag,
        int sweepingDamage,
        int criticalDamage
) {
    public static final Codec<DualWieldComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("identifiers").forGetter(DualWieldComponent::identifiers),
                    Codec.INT.fieldOf("cooldown").forGetter(DualWieldComponent::cooldown),
                    Codec.INT.fieldOf("critical_hits").forGetter(DualWieldComponent::criticalHits),
                    Identifier.CODEC.fieldOf("blocked_enchantments_tag").forGetter(DualWieldComponent::blockedEnchantmentsTag),
                    Codec.INT.fieldOf("sweeping_dmg").forGetter(DualWieldComponent::sweepingDamage),
                    Codec.INT.fieldOf("critical_dmg").forGetter(DualWieldComponent::criticalDamage)
            ).apply(instance, DualWieldComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DualWieldComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), DualWieldComponent::identifiers,
            ByteBufCodecs.VAR_INT, DualWieldComponent::cooldown,
            ByteBufCodecs.VAR_INT, DualWieldComponent::criticalHits,
            Identifier.STREAM_CODEC, DualWieldComponent::blockedEnchantmentsTag,
            ByteBufCodecs.VAR_INT, DualWieldComponent::sweepingDamage,
            ByteBufCodecs.VAR_INT, DualWieldComponent::criticalDamage,
            DualWieldComponent::new
    );

    public DualWieldComponent {
        identifiers = List.copyOf(identifiers);
        blockedEnchantmentsTag = Objects.requireNonNull(blockedEnchantmentsTag, "blockedEnchantmentsTag");
        cooldown = Math.max(0, cooldown);
        criticalHits = Math.max(0, criticalHits);
        sweepingDamage = Math.max(0, sweepingDamage);
        criticalDamage = Math.max(0, criticalDamage);
    }
}
