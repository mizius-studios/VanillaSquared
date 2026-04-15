package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record VSQEnchantmentComponent(
        Optional<List<VSQEnchantmentSlotEntry>> special,
        Optional<List<VSQEnchantmentSlotEntry>> damage,
        Optional<List<VSQEnchantmentSlotEntry>> secondary,
        Optional<List<VSQEnchantmentSlotEntry>> defense,
        Optional<List<VSQEnchantmentSlotEntry>> util,
        Optional<List<VSQEnchantmentSlotEntry>> curse
) {
    private static final Codec<VSQEnchantmentSlotEntry> NULLABLE_ENTRY_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<com.mojang.datafixers.util.Pair<VSQEnchantmentSlotEntry, T>> decode(DynamicOps<T> ops, T input) {
            if (Objects.equals(input, ops.empty())) {
                return DataResult.success(com.mojang.datafixers.util.Pair.of(null, input));
            }
            var mapValues = ops.getMapValues(input).result();
            if (mapValues.isPresent() && mapValues.get().findAny().isEmpty()) {
                return DataResult.success(com.mojang.datafixers.util.Pair.of(null, input));
            }
            return VSQEnchantmentSlotEntry.CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(VSQEnchantmentSlotEntry input, DynamicOps<T> ops, T prefix) {
            if (input == null) {
                if (ops == JsonOps.INSTANCE) {
                    return DataResult.success(ops.empty());
                }
                return DataResult.success(ops.emptyMap());
            }
            return VSQEnchantmentSlotEntry.CODEC.encode(input, ops, prefix);
        }
    };

    private static final Codec<List<VSQEnchantmentSlotEntry>> SLOT_LIST_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<List<VSQEnchantmentSlotEntry>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getList(input).setLifecycle(com.mojang.serialization.Lifecycle.stable()).flatMap(listInput -> {
                List<T> rawEntries = new ArrayList<>();
                listInput.accept(rawEntries::add);
                List<VSQEnchantmentSlotEntry> entries = new ArrayList<>();
                for (T element : rawEntries) {
                    DataResult<Pair<VSQEnchantmentSlotEntry, T>> decoded = NULLABLE_ENTRY_CODEC.decode(ops, element);
                    var result = decoded.result();
                    if (result.isEmpty()) {
                        String message = decoded.error().map(DataResult.Error::message).orElse("Failed to decode slotted enchantment entry");
                        return DataResult.error(() -> message);
                    }
                    entries.add(result.get().getFirst());
                }
                return DataResult.success(Pair.of(entries, input));
            });
        }

        @Override
        public <T> DataResult<T> encode(List<VSQEnchantmentSlotEntry> input, DynamicOps<T> ops, T prefix) {
            List<T> encoded = new ArrayList<>(input.size());
            for (VSQEnchantmentSlotEntry entry : input) {
                DataResult<T> entryResult = NULLABLE_ENTRY_CODEC.encodeStart(ops, entry);
                var result = entryResult.result();
                if (result.isEmpty()) {
                    return entryResult;
                }
                encoded.add(result.get());
            }
            return ops.mergeToList(prefix, encoded);
        }
    };
    public static final MapCodec<VSQEnchantmentComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SLOT_LIST_CODEC.optionalFieldOf("special").forGetter(VSQEnchantmentComponent::special),
            SLOT_LIST_CODEC.optionalFieldOf("damage").forGetter(VSQEnchantmentComponent::damage),
            SLOT_LIST_CODEC.optionalFieldOf("secondary").forGetter(VSQEnchantmentComponent::secondary),
            SLOT_LIST_CODEC.optionalFieldOf("defense").forGetter(VSQEnchantmentComponent::defense),
            SLOT_LIST_CODEC.optionalFieldOf("util").forGetter(VSQEnchantmentComponent::util),
            SLOT_LIST_CODEC.optionalFieldOf("curse").forGetter(VSQEnchantmentComponent::curse)
    ).apply(instance, VSQEnchantmentComponent::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, VSQEnchantmentSlotEntry> NULLABLE_ENTRY_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public VSQEnchantmentSlotEntry decode(RegistryFriendlyByteBuf buf) {
            return buf.readBoolean() ? VSQEnchantmentSlotEntry.STREAM_CODEC.decode(buf) : null;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, VSQEnchantmentSlotEntry value) {
            buf.writeBoolean(value != null);
            if (value != null) {
                VSQEnchantmentSlotEntry.STREAM_CODEC.encode(buf, value);
            }
        }
    };

    private static final StreamCodec<RegistryFriendlyByteBuf, List<VSQEnchantmentSlotEntry>> SLOT_LIST_STREAM_CODEC = NULLABLE_ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list());
    public static final StreamCodec<RegistryFriendlyByteBuf, VSQEnchantmentComponent> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public VSQEnchantmentComponent decode(RegistryFriendlyByteBuf buf) {
            return new VSQEnchantmentComponent(
                    decodeOptionalList(buf),
                    decodeOptionalList(buf),
                    decodeOptionalList(buf),
                    decodeOptionalList(buf),
                    decodeOptionalList(buf),
                    decodeOptionalList(buf)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, VSQEnchantmentComponent value) {
            encodeOptionalList(buf, value.special);
            encodeOptionalList(buf, value.damage);
            encodeOptionalList(buf, value.secondary);
            encodeOptionalList(buf, value.defense);
            encodeOptionalList(buf, value.util);
            encodeOptionalList(buf, value.curse);
        }

        private Optional<List<VSQEnchantmentSlotEntry>> decodeOptionalList(RegistryFriendlyByteBuf buf) {
            return buf.readBoolean() ? Optional.of(Collections.unmodifiableList(new ArrayList<>(SLOT_LIST_STREAM_CODEC.decode(buf)))) : Optional.empty();
        }

        private void encodeOptionalList(RegistryFriendlyByteBuf buf, Optional<List<VSQEnchantmentSlotEntry>> value) {
            buf.writeBoolean(value.isPresent());
            value.ifPresent(entries -> SLOT_LIST_STREAM_CODEC.encode(buf, entries));
        }
    };

    public VSQEnchantmentComponent {
        special = immutableOptionalList(special);
        damage = immutableOptionalList(damage);
        secondary = immutableOptionalList(secondary);
        defense = immutableOptionalList(defense);
        util = immutableOptionalList(util);
        curse = immutableOptionalList(curse);
    }

    public Optional<List<VSQEnchantmentSlotEntry>> slots(VSQEnchantmentSlotType slotType) {
        return switch (slotType) {
            case SPECIAL -> this.special;
            case DAMAGE -> this.damage;
            case SECONDARY -> this.secondary;
            case DEFENSE -> this.defense;
            case UTIL -> this.util;
            case CURSE -> this.curse;
        };
    }

    public VSQEnchantmentComponent withSlots(VSQEnchantmentSlotType slotType, Optional<List<VSQEnchantmentSlotEntry>> entries) {
        return switch (slotType) {
            case SPECIAL -> new VSQEnchantmentComponent(entries, this.damage, this.secondary, this.defense, this.util, this.curse);
            case DAMAGE -> new VSQEnchantmentComponent(this.special, entries, this.secondary, this.defense, this.util, this.curse);
            case SECONDARY -> new VSQEnchantmentComponent(this.special, this.damage, entries, this.defense, this.util, this.curse);
            case DEFENSE -> new VSQEnchantmentComponent(this.special, this.damage, this.secondary, entries, this.util, this.curse);
            case UTIL -> new VSQEnchantmentComponent(this.special, this.damage, this.secondary, this.defense, entries, this.curse);
            case CURSE -> new VSQEnchantmentComponent(this.special, this.damage, this.secondary, this.defense, this.util, entries);
        };
    }

    private static Optional<List<VSQEnchantmentSlotEntry>> immutableOptionalList(Optional<List<VSQEnchantmentSlotEntry>> entries) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Collections.unmodifiableList(new ArrayList<>(entries.get())));
    }
}
