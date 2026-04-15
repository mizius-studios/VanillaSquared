package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.util.api.modules.components.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class VSQEnchantmentTooltipState {
    private static int hoveredHash;
    private static int selectedIndex;
    private static long lastTooltipMillis;
    private static ItemStack hoveredStack = ItemStack.EMPTY;

    private VSQEnchantmentTooltipState() {
    }

    public static void onTooltip(ItemStack stack) {
        int currentHash = ItemStack.hashItemAndComponents(stack);
        if (currentHash != hoveredHash) {
            hoveredHash = currentHash;
            selectedIndex = 0;
        }
        hoveredStack = stack.copy();
        lastTooltipMillis = System.currentTimeMillis();
    }

    public static boolean isActive() {
        return System.currentTimeMillis() - lastTooltipMillis < 250L;
    }

    public static ItemStack hoveredStack() {
        return isActive() ? hoveredStack : ItemStack.EMPTY;
    }

    public static boolean cycleHovered(int delta) {
        ItemStack stack = hoveredStack();
        if (stack.isEmpty()) {
            return false;
        }

        VSQEnchantmentComponent component = stack.get(DataComponents.VSQ_ENCHANTMENT);
        if (component == null) {
            return false;
        }

        return cycle(component, delta);
    }

    public static int selectedIndex(VSQEnchantmentComponent component) {
        List<?> slotTypes = VSQEnchantmentSlots.definedSlotTypes(component);
        if (slotTypes.isEmpty()) {
            selectedIndex = 0;
            return 0;
        }
        if (selectedIndex >= slotTypes.size()) {
            selectedIndex = 0;
        }
        return selectedIndex;
    }

    public static boolean cycle(VSQEnchantmentComponent component, int delta) {
        List<?> slotTypes = VSQEnchantmentSlots.definedSlotTypes(component);
        if (slotTypes.isEmpty()) {
            selectedIndex = 0;
            return false;
        }

        int size = slotTypes.size();
        selectedIndex = Math.floorMod(selectedIndex + delta, size);
        lastTooltipMillis = System.currentTimeMillis();
        return true;
    }
}
