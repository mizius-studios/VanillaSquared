package blob.vanillasquared.util.api.enchantment;

import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentProfile;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentSlotEntry;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentSlotType;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentSlots;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VSQEnchantments {
    private VSQEnchantments() {
    }

    public static boolean isDerivedSyncInProgress() {
        return VSQEnchantmentSlots.isDerivedSyncInProgress();
    }

    public static void ensureSeeded(ItemStack stack) {
        VSQEnchantmentSlots.ensureSeeded(stack);
    }

    public static void onVanillaEnchantmentsChanged(ItemStack stack, ItemEnchantments enchantments) {
        VSQEnchantmentSlots.onVanillaEnchantmentsChanged(stack, enchantments);
    }

    public static void syncDerivedEnchantments(ItemStack stack) {
        VSQEnchantmentSlots.syncDerivedEnchantments(stack);
    }

    public static void restoreVanillaEnchantmentDefaults(ItemStack stack) {
        VSQEnchantmentSlots.restoreVanillaEnchantmentDefaults(stack);
    }

    public static ItemEnchantments aggregate(ItemStack stack) {
        return VSQEnchantmentSlots.aggregate(stack);
    }

    public static int currentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return VSQEnchantmentSlots.currentLevel(stack, enchantment);
    }

    public static boolean canApply(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        return VSQEnchantmentSlots.canApplyInSlots(stack, enchantment, level);
    }

    public static ItemStack apply(ItemStack originalStack, Holder<Enchantment> enchantment, int level) {
        return VSQEnchantmentSlots.applyEnchant(originalStack, enchantment, level);
    }

    public static boolean setLevel(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        return VSQEnchantmentSlots.setEnchantmentLevel(stack, enchantment, level);
    }

    public static VSQEnchantmentSlotType slotType(Holder<Enchantment> enchantment) {
        return VSQEnchantmentSlots.slotType(enchantment);
    }

    public static VSQEnchantmentSlotType slotType(ItemStack stack, Holder<Enchantment> enchantment) {
        return VSQEnchantmentSlots.slotType(stack, enchantment);
    }

    public static int maxLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return VSQEnchantmentSlots.maxLevel(stack, enchantment);
    }

    public static boolean areCompatible(ItemStack stack, Holder<Enchantment> enchantment, Holder<Enchantment> other) {
        return VSQEnchantmentSlots.areCompatible(stack, enchantment, other);
    }

    public static Optional<VSQEnchantmentProfile> selectedProfile(ItemStack stack, Holder<Enchantment> enchantment) {
        return VSQEnchantmentSlots.selectedProfile(stack, enchantment);
    }

    public static Optional<VSQEnchantmentProfile> selectedProfile(ItemStack stack, Enchantment enchantment) {
        return VSQEnchantmentSlots.selectedProfile(stack, enchantment);
    }

    public static Optional<VSQEnchantmentProfile> selectedProjectileTakeoverProfile(ItemStack sourceStack, Holder<Enchantment> enchantment) {
        return VSQEnchantmentSlots.selectedProjectileTakeoverProfile(sourceStack, enchantment);
    }

    public static Optional<VSQEnchantmentProfile> selectedProjectileTakeoverProfile(ItemStack sourceStack, Enchantment enchantment) {
        return VSQEnchantmentSlots.selectedProjectileTakeoverProfile(sourceStack, enchantment);
    }

    public static <T> List<T> profileEffects(ItemStack stack, Holder<Enchantment> enchantment, DataComponentType<List<T>> effectType) {
        return VSQEnchantmentSlots.profileEffects(stack, enchantment, effectType);
    }

    public static <T> T profileEffect(ItemStack stack, Holder<Enchantment> enchantment, DataComponentType<T> effectType) {
        return VSQEnchantmentSlots.profileEffect(stack, enchantment, effectType);
    }

    public static Map<VSQEnchantmentSlotType, Integer> definedCapacities(ItemStack stack) {
        return VSQEnchantmentSlots.definedCapacities(stack);
    }

    public static VSQEnchantmentComponent createSeededComponent(ItemStack stack) {
        return VSQEnchantmentSlots.createSeededComponent(stack);
    }

    public static boolean hasAnySlots(VSQEnchantmentComponent component) {
        return VSQEnchantmentSlots.hasAnySlots(component);
    }

    public static Optional<VSQEnchantmentComponent> tryPopulateFromVanilla(VSQEnchantmentComponent base, ItemStack stack, ItemEnchantments enchantments) {
        return VSQEnchantmentSlots.tryPopulateFromVanilla(base, stack, enchantments);
    }

    public static boolean randomizeSlotCapacities(ItemStack stack, RandomSource random, int minCapacity, int maxCapacity) {
        return VSQEnchantmentSlots.randomizeSlotCapacities(stack, random, minCapacity, maxCapacity);
    }

    public static VSQEnchantmentComponent randomizeSlotCapacities(VSQEnchantmentComponent component, RandomSource random, int minCapacity, int maxCapacity) {
        return VSQEnchantmentSlots.randomizeSlotCapacities(component, random, minCapacity, maxCapacity);
    }

    public static List<VSQEnchantmentSlotEntry> appendSlotEntry(List<VSQEnchantmentSlotEntry> entries, VSQEnchantmentSlotEntry entry) {
        return VSQEnchantmentSlots.appendSlotEntry(entries, entry);
    }

    public static VSQEnchantmentComponent randomizeFreeSlots(VSQEnchantmentComponent component, Map<VSQEnchantmentSlotType, Integer> requestedFreeSlots) {
        return VSQEnchantmentSlots.randomizeFreeSlots(component, requestedFreeSlots);
    }

    public static TypedDataComponent<VSQEnchantmentComponent> componentFor(ItemStack stack) {
        return VSQEnchantmentSlots.componentFor(stack);
    }

    public static List<VSQEnchantmentSlotType> definedSlotTypes(VSQEnchantmentComponent component) {
        return VSQEnchantmentSlots.definedSlotTypes(component);
    }

    public static List<Component> buildTooltipLines(VSQEnchantmentComponent component, int selectedIndex, boolean expandSelected) {
        return VSQEnchantmentSlots.buildTooltipLines(component, selectedIndex, expandSelected);
    }
}
