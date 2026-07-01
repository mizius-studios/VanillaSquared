package blob.vanillasquared.main.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record VSQEnchantmentComponent(
        Optional<List<VSQEnchantmentSlotEntry>> special,
        Optional<List<VSQEnchantmentSlotEntry>> damage,
        Optional<List<VSQEnchantmentSlotEntry>> secondary,
        Optional<List<VSQEnchantmentSlotEntry>> defense,
        Optional<List<VSQEnchantmentSlotEntry>> util,
        Optional<List<VSQEnchantmentSlotEntry>> curse
) {
    private static final Codec<List<VSQEnchantmentSlotEntry>> SLOT_LIST_CODEC = VSQEnchantmentSlotEntry.CODEC.listOf();

    public static final MapCodec<VSQEnchantmentComponent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SLOT_LIST_CODEC.optionalFieldOf("special").forGetter(VSQEnchantmentComponent::special),
            SLOT_LIST_CODEC.optionalFieldOf("damage").forGetter(VSQEnchantmentComponent::damage),
            SLOT_LIST_CODEC.optionalFieldOf("secondary").forGetter(VSQEnchantmentComponent::secondary),
            SLOT_LIST_CODEC.optionalFieldOf("defense").forGetter(VSQEnchantmentComponent::defense),
            SLOT_LIST_CODEC.optionalFieldOf("util").forGetter(VSQEnchantmentComponent::util),
            SLOT_LIST_CODEC.optionalFieldOf("curse").forGetter(VSQEnchantmentComponent::curse)
    ).apply(instance, VSQEnchantmentComponent::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<VSQEnchantmentSlotEntry>> SLOT_LIST_STREAM_CODEC = VSQEnchantmentSlotEntry.STREAM_CODEC.apply(ByteBufCodecs.list());
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
            return buf.readBoolean() ? Optional.of(List.copyOf(SLOT_LIST_STREAM_CODEC.decode(buf))) : Optional.empty();
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

    /**
     * Returns the slot groups represented by this component in tooltip order.
     */
    public List<VSQEnchantmentSlotType> definedSlotTypes() {
        List<VSQEnchantmentSlotType> slotTypes = new ArrayList<>();
        for (VSQEnchantmentSlotType slotType : VSQEnchantmentSlotType.values()) {
            if (this.slots(slotType).isPresent()) {
                slotTypes.add(slotType);
            }
        }
        return List.copyOf(slotTypes);
    }

    /**
     * Builds the complete enchantment-slot tooltip owned by this component.
     */
    public List<Component> tooltipLines(int selectedIndex, boolean expandSelected) {
        List<VSQEnchantmentSlotType> slotTypes = this.definedSlotTypes();
        if (slotTypes.isEmpty()) {
            return List.of();
        }

        int normalizedSelection = Math.floorMod(selectedIndex, slotTypes.size());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.translatable("vsq.tooltip.enchantment_slots.header").withStyle(ChatFormatting.GRAY));
        for (int index = 0; index < slotTypes.size(); index++) {
            VSQEnchantmentSlotType slotType = slotTypes.get(index);
            List<VSQEnchantmentSlotEntry> entries = this.slots(slotType).orElse(List.of());
            long filled = entries.stream().filter(entry -> !entry.isEmpty()).count();
            boolean selected = index == normalizedSelection;
            lines.add(Component.translatable(
                    selected ? "vsq.tooltip.enchantment_slots.slot.selected" : "vsq.tooltip.enchantment_slots.slot",
                    Component.translatable("vsq.enchantment_slot." + slotType.serializedName()),
                    filled,
                    entries.size()
            ).withStyle(selected ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA));
            if (selected && expandSelected) {
                for (VSQEnchantmentSlotEntry entry : entries) {
                    Component entryLine = entry.isEmpty()
                            ? Component.translatable("vsq.tooltip.enchantment_slots.empty").withStyle(ChatFormatting.DARK_GRAY)
                            : Enchantment.getFullname(entry.enchantment(), entry.level()).copy().withStyle(ChatFormatting.GRAY);
                    lines.add(Component.literal("  ").append(entryLine));
                }
            }
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("vsq.tooltip.enchantment_slots.hint").withStyle(ChatFormatting.DARK_GRAY));
        return List.copyOf(lines);
    }

    private static Optional<List<VSQEnchantmentSlotEntry>> immutableOptionalList(Optional<List<VSQEnchantmentSlotEntry>> entries) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        List<VSQEnchantmentSlotEntry> normalized = new ArrayList<>(entries.get().size());
        for (VSQEnchantmentSlotEntry entry : entries.get()) {
            normalized.add(entry == null ? VSQEnchantmentSlotEntry.empty() : entry);
        }
        return Optional.of(Collections.unmodifiableList(normalized));
    }
}
