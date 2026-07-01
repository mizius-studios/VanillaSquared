package blob.vanillasquared.main.world.item.enchantment;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Objects;

public record VSQEnchantmentSlotEntry(Holder<Enchantment> enchantment, int level) {
    private static final String LEGACY_EMPTY_MARKER = "null";

    public static final VSQEnchantmentSlotEntry EMPTY = new VSQEnchantmentSlotEntry(null, 0);

    private static final Codec<VSQEnchantmentSlotEntry> PRESENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Enchantment.CODEC.fieldOf("id").forGetter(VSQEnchantmentSlotEntry::enchantment),
            Codec.intRange(1, 255).fieldOf("level").forGetter(VSQEnchantmentSlotEntry::level)
    ).apply(instance, VSQEnchantmentSlotEntry::new));
    private static final Codec<VSQEnchantmentSlotEntry> EMPTY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("empty", false).forGetter(entry -> true)
    ).apply(instance, empty -> empty ? VSQEnchantmentSlotEntry.EMPTY : null));

    public static final Codec<VSQEnchantmentSlotEntry> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<VSQEnchantmentSlotEntry, T>> decode(DynamicOps<T> ops, T input) {
            if (Objects.equals(input, ops.empty())) {
                return DataResult.success(new Pair<>(VSQEnchantmentSlotEntry.EMPTY, input));
            }
            var mapResult = ops.getMap(input).result();
            if (mapResult.isPresent() && mapResult.get().entries().findAny().isEmpty()) {
                return DataResult.success(new Pair<>(VSQEnchantmentSlotEntry.EMPTY, input));
            }
            DataResult<Pair<VSQEnchantmentSlotEntry, T>> emptyDecoded = EMPTY_CODEC.decode(ops, input);
            if (emptyDecoded.result().isPresent()) {
                Pair<VSQEnchantmentSlotEntry, T> result = emptyDecoded.result().get();
                if (result.getFirst() != null) {
                    return DataResult.success(result);
                }
            }

            DataResult<String> stringResult = ops.getStringValue(input);
            if (stringResult.result().isPresent()) {
                String value = stringResult.result().get();
                if (LEGACY_EMPTY_MARKER.equals(value)) {
                    return DataResult.success(new Pair<>(VSQEnchantmentSlotEntry.EMPTY, input));
                }
                return DataResult.error(() -> "Expected \"" + LEGACY_EMPTY_MARKER + "\" for empty enchantment slot entry");
            }

            return PRESENT_CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(VSQEnchantmentSlotEntry input, DynamicOps<T> ops, T prefix) {
            if (input == null || input.isEmpty()) {
                return EMPTY_CODEC.encode(VSQEnchantmentSlotEntry.EMPTY, ops, prefix);
            }
            return PRESENT_CODEC.encode(input, ops, prefix);
        }
    };

    private static final StreamCodec<RegistryFriendlyByteBuf, VSQEnchantmentSlotEntry> PRESENT_STREAM_CODEC = StreamCodec.composite(
            Enchantment.STREAM_CODEC, VSQEnchantmentSlotEntry::enchantment,
            ByteBufCodecs.VAR_INT, VSQEnchantmentSlotEntry::level,
            VSQEnchantmentSlotEntry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, VSQEnchantmentSlotEntry> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public VSQEnchantmentSlotEntry decode(RegistryFriendlyByteBuf buf) {
            if (!buf.readBoolean()) {
                return EMPTY;
            }
            return PRESENT_STREAM_CODEC.decode(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, VSQEnchantmentSlotEntry value) {
            if (value == null || value.isEmpty()) {
                buf.writeBoolean(false);
                return;
            }

            buf.writeBoolean(true);
            PRESENT_STREAM_CODEC.encode(buf, value);
        }
    };

    public static VSQEnchantmentSlotEntry empty() {
        return EMPTY;
    }

    public static VSQEnchantmentSlotEntry of(Holder<Enchantment> enchantment, int level) {
        return new VSQEnchantmentSlotEntry(enchantment, level);
    }

    public boolean isEmpty() {
        return this.enchantment == null && this.level == 0;
    }
}
