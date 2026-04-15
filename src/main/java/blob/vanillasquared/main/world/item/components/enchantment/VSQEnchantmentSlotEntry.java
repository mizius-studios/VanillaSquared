package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

public record VSQEnchantmentSlotEntry(Holder<Enchantment> enchantment, int level) {
    public static final Codec<VSQEnchantmentSlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Enchantment.CODEC.fieldOf("id").forGetter(VSQEnchantmentSlotEntry::enchantment),
            Codec.intRange(1, 255).fieldOf("level").forGetter(VSQEnchantmentSlotEntry::level)
    ).apply(instance, VSQEnchantmentSlotEntry::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VSQEnchantmentSlotEntry> STREAM_CODEC = StreamCodec.composite(
            Enchantment.STREAM_CODEC, VSQEnchantmentSlotEntry::enchantment,
            ByteBufCodecs.VAR_INT, VSQEnchantmentSlotEntry::level,
            VSQEnchantmentSlotEntry::new
    );
}
