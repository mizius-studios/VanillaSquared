package blob.vanillasquared.main.world.item.enchantment;

import blob.vanillasquared.util.api.modules.components.VSQDataComponents;
import blob.vanillasquared.util.api.modules.components.VSQItemComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VSQEnchantmentSlots {
    private static final ThreadLocal<Boolean> DERIVED_SYNC_GUARD = ThreadLocal.withInitial(() -> false);

    private VSQEnchantmentSlots() {
    }

    public static boolean isDerivedSyncInProgress() {
        return DERIVED_SYNC_GUARD.get();
    }

    public static void ensureSeeded(ItemStack stack) {
        if (stack.isEmpty() || VSQItemComponents.hasEnchantmentComponent(stack) || !supportsSlotComponent(stack)) {
            return;
        }

        VSQEnchantmentComponent seeded = createSeededComponent(stack);
        if (hasAnySlots(seeded)) {
            VSQItemComponents.setEnchantmentComponent(stack, seeded);
        }
    }

    public static void onVanillaEnchantmentsChanged(ItemStack stack, ItemEnchantments enchantments) {
        if (stack.isEmpty() || isDerivedSyncInProgress()) {
            return;
        }

        boolean alreadySeeded = VSQItemComponents.hasEnchantmentComponent(stack);
        ensureSeeded(stack);
        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        if (component == null || enchantments == null) {
            return;
        }

        Optional<VSQEnchantmentComponent> migrated = tryPopulateFromVanilla(component, stack, enchantments);
        if (migrated.isPresent()) {
            VSQItemComponents.setEnchantmentComponent(stack, migrated.get());
            syncDerivedEnchantments(stack);
        } else if (!alreadySeeded) {
            VSQItemComponents.removeEnchantmentComponent(stack);
        }
    }

    public static void syncDerivedEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        if (component == null) {
            return;
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (VSQEnchantmentSlotType slotType : VSQEnchantmentSlotType.values()) {
            component.slots(slotType).ifPresent(entries -> {
                for (VSQEnchantmentSlotEntry entry : entries) {
                    if (!entry.isEmpty()) {
                        mutable.set(entry.enchantment(), entry.level());
                    }
                }
            });
        }

        DERIVED_SYNC_GUARD.set(true);
        try {
            stack.set(vanillaTargetComponent(stack), mutable.toImmutable());
        } finally {
            DERIVED_SYNC_GUARD.set(false);
        }
    }

    public static ItemEnchantments aggregate(ItemStack stack) {
        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        if (component == null) {
            return stack.getOrDefault(vanillaTargetComponent(stack), ItemEnchantments.EMPTY);
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (VSQEnchantmentSlotType slotType : VSQEnchantmentSlotType.values()) {
            component.slots(slotType).ifPresent(entries -> entries.stream().filter(entry -> !entry.isEmpty()).forEach(entry -> mutable.set(entry.enchantment(), entry.level())));
        }
        return mutable.toImmutable();
    }

    public static int currentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return aggregate(stack).getLevel(enchantment);
    }

    public static boolean canApplyInSlots(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        ensureSeeded(stack);
        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        if (component == null) {
            return false;
        }

        VSQEnchantmentSlotType slotType = slotType(stack, enchantment);
        if (slotType == null) {
            return false;
        }

        Optional<List<VSQEnchantmentSlotEntry>> maybeEntries = component.slots(slotType);
        if (maybeEntries.isEmpty()) {
            return false;
        }

        List<VSQEnchantmentSlotEntry> entries = maybeEntries.get();
        for (VSQEnchantmentSlotEntry entry : entries) {
            if (!entry.isEmpty() && entry.enchantment().equals(enchantment)) {
                return true;
            }
        }

        ItemEnchantments aggregate = aggregate(stack);
        for (Holder<Enchantment> other : aggregate.keySet()) {
            if (!other.equals(enchantment) && !areCompatible(stack, enchantment, other)) {
                return false;
            }
        }

        for (VSQEnchantmentSlotEntry entry : entries) {
            if (entry.isEmpty()) {
                return level > 0;
            }
        }
        return false;
    }

    public static ItemStack applyEnchant(ItemStack originalStack, Holder<Enchantment> enchantment, int level) {
        ItemStack result = originalStack.copy();
        if (!setEnchantmentLevel(result, enchantment, level)) {
            return originalStack.copy();
        }
        return result;
    }

    public static boolean setEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        ensureSeeded(stack);
        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        VSQEnchantmentSlotType slotType = slotType(stack, enchantment);
        if (component == null || slotType == null) {
            return false;
        }

        Optional<List<VSQEnchantmentSlotEntry>> maybeEntries = component.slots(slotType);
        if (maybeEntries.isEmpty()) {
            return false;
        }

        List<VSQEnchantmentSlotEntry> updated = new ArrayList<>(maybeEntries.get());
        for (int index = 0; index < updated.size(); index++) {
            VSQEnchantmentSlotEntry entry = updated.get(index);
            if (!entry.isEmpty() && entry.enchantment().equals(enchantment)) {
                if (entry.level() == level) {
                    return false;
                }
                updated.set(index, VSQEnchantmentSlotEntry.of(enchantment, level));
                VSQItemComponents.setEnchantmentComponent(stack, component.withSlots(slotType, Optional.of(updated)));
                syncDerivedEnchantments(stack);
                return true;
            }
        }

        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).isEmpty()) {
                updated.set(index, VSQEnchantmentSlotEntry.of(enchantment, level));
                VSQItemComponents.setEnchantmentComponent(stack, component.withSlots(slotType, Optional.of(updated)));
                syncDerivedEnchantments(stack);
                return true;
            }
        }
        return false;
    }

    public static List<VSQEnchantmentSlotEntry> appendSlotEntry(List<VSQEnchantmentSlotEntry> entries, VSQEnchantmentSlotEntry entry) {
        List<VSQEnchantmentSlotEntry> updated = new ArrayList<>(entries);
        updated.add(entry);
        return List.copyOf(updated);
    }

    public static VSQEnchantmentComponent randomizeFreeSlots(VSQEnchantmentComponent component, Map<VSQEnchantmentSlotType, Integer> requestedFreeSlots) {
        VSQEnchantmentComponent updated = component;
        for (Map.Entry<VSQEnchantmentSlotType, Integer> entry : requestedFreeSlots.entrySet()) {
            Optional<List<VSQEnchantmentSlotEntry>> maybeEntries = updated.slots(entry.getKey());
            if (maybeEntries.isEmpty()) {
                continue;
            }

            List<VSQEnchantmentSlotEntry> source = maybeEntries.get();
            List<VSQEnchantmentSlotEntry> randomized = new ArrayList<>(source.size() + Math.max(0, entry.getValue()));
            for (VSQEnchantmentSlotEntry slotEntry : source) {
                if (!slotEntry.isEmpty()) {
                    randomized.add(slotEntry);
                }
            }
            for (int index = 0; index < Math.max(0, entry.getValue()); index++) {
                randomized.add(VSQEnchantmentSlotEntry.empty());
            }
            updated = updated.withSlots(entry.getKey(), Optional.of(randomized));
        }
        return updated;
    }

    public static boolean randomizeSlotCapacities(ItemStack stack, RandomSource random, int minCapacity, int maxCapacity) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean alreadySeeded = VSQItemComponents.hasEnchantmentComponent(stack);
        ensureSeeded(stack);
        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        if (component == null) {
            return false;
        }

        ItemEnchantments vanillaEnchantments = stack.getOrDefault(vanillaTargetComponent(stack), ItemEnchantments.EMPTY);
        if (!vanillaEnchantments.isEmpty()) {
            Optional<VSQEnchantmentComponent> migrated = tryPopulateFromVanilla(component, stack, vanillaEnchantments);
            if (migrated.isEmpty()) {
                if (!alreadySeeded) {
                    VSQItemComponents.removeEnchantmentComponent(stack);
                }
                return false;
            }
            component = migrated.get();
        }

        VSQItemComponents.setEnchantmentComponent(stack, randomizeSlotCapacities(component, random, minCapacity, maxCapacity));
        syncDerivedEnchantments(stack);
        return true;
    }

    public static VSQEnchantmentComponent randomizeSlotCapacities(VSQEnchantmentComponent component, RandomSource random, int minCapacity, int maxCapacity) {
        int min = Math.max(0, Math.min(minCapacity, maxCapacity));
        int max = Math.max(0, Math.max(minCapacity, maxCapacity));
        VSQEnchantmentComponent updated = component;
        for (VSQEnchantmentSlotType slotType : VSQEnchantmentSlotType.values()) {
            if (slotType == VSQEnchantmentSlotType.SPECIAL) {
                // SPECIAL capacity is fixed by item type and is not randomized by loot generation.
                continue;
            }

            Optional<List<VSQEnchantmentSlotEntry>> maybeEntries = updated.slots(slotType);
            if (maybeEntries.isEmpty()) {
                continue;
            }

            List<VSQEnchantmentSlotEntry> existingEnchantments = maybeEntries.get().stream()
                    .filter(entry -> !entry.isEmpty())
                    .toList();
            int targetCapacity = Math.max(Mth.nextInt(random, min, max), existingEnchantments.size());
            List<VSQEnchantmentSlotEntry> resized = new ArrayList<>(targetCapacity);
            resized.addAll(existingEnchantments);
            while (resized.size() < targetCapacity) {
                resized.add(VSQEnchantmentSlotEntry.empty());
            }
            updated = updated.withSlots(slotType, Optional.of(resized));
        }
        return updated;
    }

    public static VSQEnchantmentSlotType slotType(Holder<Enchantment> enchantment) {
        if ((Object) enchantment.value() instanceof VSQEnchantmentAccess access) {
            return access.vsq$getEnchantmentSlotType();
        }
        return null;
    }

    public static VSQEnchantmentSlotType slotType(ItemStack stack, Holder<Enchantment> enchantment) {
        return selectedProfile(stack, enchantment).map(VSQEnchantmentProfile::enchantmentSlot).orElse(null);
    }

    public static int maxLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return selectedProfile(stack, enchantment).map(VSQEnchantmentProfile::maxLevel).orElse(enchantment.value().getMaxLevel());
    }

    public static boolean areCompatible(ItemStack stack, Holder<Enchantment> enchantment, Holder<Enchantment> other) {
        if (enchantment.equals(other)) {
            return false;
        }

        Optional<VSQEnchantmentProfile> leftProfile = selectedProfile(stack, enchantment);
        Optional<VSQEnchantmentProfile> rightProfile = selectedProfile(stack, other);
        if (leftProfile.isPresent() || rightProfile.isPresent()) {
            boolean profileCompatible = !leftProfile.map(profile -> profile.exclusiveSet().contains(other)).orElse(false)
                    && !rightProfile.map(profile -> profile.exclusiveSet().contains(enchantment)).orElse(false);
            boolean vanillaCompatible = leftProfile.isPresent() && rightProfile.isPresent() || Enchantment.areCompatible(enchantment, other);
            return profileCompatible && vanillaCompatible;
        }
        return Enchantment.areCompatible(enchantment, other);
    }

    public static Optional<VSQEnchantmentProfile> selectedProfile(ItemStack stack, Holder<Enchantment> enchantment) {
        return selectedProfile(stack, enchantment.value());
    }

    public static Optional<VSQEnchantmentProfile> selectedProfile(ItemStack stack, Enchantment enchantment) {
        if (!((Object) enchantment instanceof VSQEnchantmentAccess access)) {
            return Optional.empty();
        }

        for (VSQEnchantmentProfile profile : access.vsq$getProfiles()) {
            if (profile.matches(stack)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public static Optional<VSQEnchantmentProfile> selectedProjectileTakeoverProfile(ItemStack sourceStack, Holder<Enchantment> enchantment) {
        return selectedProjectileTakeoverProfile(sourceStack, enchantment.value());
    }

    public static Optional<VSQEnchantmentProfile> selectedProjectileTakeoverProfile(ItemStack sourceStack, Enchantment enchantment) {
        if (!((Object) enchantment instanceof VSQEnchantmentAccess access)) {
            return Optional.empty();
        }

        for (VSQEnchantmentProfile profile : access.vsq$getProfiles()) {
            if (profile.matchesProjectileTakeover(sourceStack)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public static <T> List<T> profileEffects(ItemStack stack, Holder<Enchantment> enchantment, DataComponentType<List<T>> effectType) {
        return selectedProfile(stack, enchantment)
                .map(profile -> profile.effects().getOrDefault(effectType, List.<T>of()))
                .orElseGet(() -> enchantment.value().getEffects(effectType));
    }

    public static <T> T profileEffect(ItemStack stack, Holder<Enchantment> enchantment, DataComponentType<T> effectType) {
        Optional<VSQEnchantmentProfile> profile = selectedProfile(stack, enchantment);
        if (profile.isPresent()) {
            return profile.get().effects().get(effectType);
        }
        return enchantment.value().effects().get(effectType);
    }

    public static Map<VSQEnchantmentSlotType, Integer> definedCapacities(ItemStack stack) {
        Item item = stack.getItem();
        EnumMap<VSQEnchantmentSlotType, Integer> capacities = new EnumMap<>(VSQEnchantmentSlotType.class);
        String itemPath = BuiltInRegistries.ITEM.getKey(item).getPath();
        // These suffix checks intentionally mirror vanilla-style item names; generic ENCHANTABLE items get fallback slots below.
        boolean armor = itemPath.endsWith("_helmet") || itemPath.endsWith("_chestplate") || itemPath.endsWith("_leggings") || itemPath.endsWith("_boots");
        boolean sword = itemPath.endsWith("_sword");
        boolean pickaxe = itemPath.endsWith("_pickaxe");
        boolean elytra = itemPath.equals("elytra");
        boolean spear = itemPath.endsWith("_spear");
        boolean book = itemPath.equals("book");

        if (armor) {
            capacities.put(VSQEnchantmentSlotType.DEFENSE, 3);
            capacities.put(VSQEnchantmentSlotType.SECONDARY, 3);
            capacities.put(VSQEnchantmentSlotType.UTIL, 3);
            capacities.put(VSQEnchantmentSlotType.CURSE, 2);
        } else if (elytra) {
            capacities.put(VSQEnchantmentSlotType.DEFENSE, 2);
            capacities.put(VSQEnchantmentSlotType.SECONDARY, 3);
            capacities.put(VSQEnchantmentSlotType.UTIL, 2);
            capacities.put(VSQEnchantmentSlotType.CURSE, 1);
        } else if (item instanceof ShieldItem || itemPath.equals("shield")) {
            capacities.put(VSQEnchantmentSlotType.SECONDARY, 2);
            capacities.put(VSQEnchantmentSlotType.UTIL, 2);
            capacities.put(VSQEnchantmentSlotType.CURSE, 1);
        } else if (sword || item instanceof AxeItem || item instanceof MaceItem || item instanceof TridentItem || item instanceof BowItem || item instanceof CrossbowItem || spear) {
            capacities.put(VSQEnchantmentSlotType.DAMAGE, 3);
            capacities.put(VSQEnchantmentSlotType.SECONDARY, 2);
            capacities.put(VSQEnchantmentSlotType.UTIL, 3);
            capacities.put(VSQEnchantmentSlotType.SPECIAL, 1);
            capacities.put(VSQEnchantmentSlotType.CURSE, 1);
        } else if (pickaxe || item instanceof ShovelItem || item instanceof HoeItem || item instanceof ShearsItem || item instanceof FlintAndSteelItem) {
            capacities.put(VSQEnchantmentSlotType.SECONDARY, 4);
            capacities.put(VSQEnchantmentSlotType.UTIL, 3);
            capacities.put(VSQEnchantmentSlotType.CURSE, 1);
        } else if (item instanceof FishingRodItem) {
            capacities.put(VSQEnchantmentSlotType.SECONDARY, 4);
            capacities.put(VSQEnchantmentSlotType.UTIL, 3);
            capacities.put(VSQEnchantmentSlotType.CURSE, 1);
        } else if (book) {
            return capacities;
        } else if (stack.has(DataComponents.ENCHANTABLE)) {
            Enchantable enchantable = stack.get(DataComponents.ENCHANTABLE);
            if (enchantable != null) {
                capacities.put(VSQEnchantmentSlotType.UTIL, Math.max(1, Math.min(2, enchantable.value() / 12)));
                capacities.put(VSQEnchantmentSlotType.CURSE, 1);
            }
        }

        return capacities;
    }

    private static boolean supportsSlotComponent(ItemStack stack) {
        Item item = stack.getItem();
        return stack.has(DataComponents.ENCHANTABLE) || item instanceof ShieldItem || BuiltInRegistries.ITEM.getKey(item).getPath().equals("shield");
    }

    public static VSQEnchantmentComponent createSeededComponent(ItemStack stack) {
        Map<VSQEnchantmentSlotType, Integer> capacities = definedCapacities(stack);
        VSQEnchantmentComponent component = new VSQEnchantmentComponent(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        for (Map.Entry<VSQEnchantmentSlotType, Integer> entry : capacities.entrySet()) {
            List<VSQEnchantmentSlotEntry> slots = new ArrayList<>(entry.getValue());
            for (int index = 0; index < entry.getValue(); index++) {
                slots.add(VSQEnchantmentSlotEntry.empty());
            }
            component = component.withSlots(entry.getKey(), Optional.of(slots));
        }
        return component;
    }

    public static boolean hasAnySlots(VSQEnchantmentComponent component) {
        for (VSQEnchantmentSlotType slotType : VSQEnchantmentSlotType.values()) {
            if (component.slots(slotType).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<VSQEnchantmentComponent> tryPopulateFromVanilla(VSQEnchantmentComponent base, ItemStack stack, ItemEnchantments enchantments) {
        VSQEnchantmentComponent working = base;
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            VSQEnchantmentSlotType slotType = slotType(stack, enchantment);
            if (slotType == null) {
                continue;
            }

            Optional<List<VSQEnchantmentSlotEntry>> maybeEntries = working.slots(slotType);
            if (maybeEntries.isEmpty()) {
                return Optional.empty();
            }

            List<VSQEnchantmentSlotEntry> updated = new ArrayList<>(maybeEntries.get());
            boolean inserted = false;
            for (int index = 0; index < updated.size(); index++) {
                VSQEnchantmentSlotEntry existing = updated.get(index);
                if (!existing.isEmpty() && existing.enchantment().equals(enchantment)) {
                    updated.set(index, VSQEnchantmentSlotEntry.of(enchantment, level));
                    inserted = true;
                    break;
                }
            }
            if (!inserted) {
                for (int index = 0; index < updated.size(); index++) {
                    if (updated.get(index).isEmpty()) {
                        updated.set(index, VSQEnchantmentSlotEntry.of(enchantment, level));
                        inserted = true;
                        break;
                    }
                }
            }
            if (!inserted) {
                return Optional.empty();
            }

            working = working.withSlots(slotType, Optional.of(updated));
        }
        return Optional.of(working);
    }

    public static TypedDataComponent<VSQEnchantmentComponent> componentFor(ItemStack stack) {
        ensureSeeded(stack);
        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        return component == null ? null : new TypedDataComponent<>(VSQDataComponents.ENCHANTMENT, component);
    }

    public static List<VSQEnchantmentSlotType> definedSlotTypes(VSQEnchantmentComponent component) {
        List<VSQEnchantmentSlotType> slotTypes = new ArrayList<>();
        for (VSQEnchantmentSlotType slotType : VSQEnchantmentSlotType.values()) {
            if (component.slots(slotType).isPresent()) {
                slotTypes.add(slotType);
            }
        }
        return List.copyOf(slotTypes);
    }

    public static List<Component> buildTooltipLines(VSQEnchantmentComponent component, int selectedIndex, boolean expandSelected) {
        List<VSQEnchantmentSlotType> slotTypes = definedSlotTypes(component);
        if (slotTypes.isEmpty()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.translatable("vsq.tooltip.enchantment_slots.header").withStyle(ChatFormatting.GRAY));
        for (int index = 0; index < slotTypes.size(); index++) {
            VSQEnchantmentSlotType slotType = slotTypes.get(index);
            List<VSQEnchantmentSlotEntry> entries = component.slots(slotType).orElse(List.of());
            long filled = entries.stream().filter(entry -> !entry.isEmpty()).count();
            boolean selected = index == selectedIndex;
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

    private static net.minecraft.core.component.DataComponentType<ItemEnchantments> vanillaTargetComponent(ItemStack stack) {
        ItemEnchantments stored = stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) || !stored.isEmpty()) {
            return net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS;
        }
        return net.minecraft.core.component.DataComponents.ENCHANTMENTS;
    }
}
