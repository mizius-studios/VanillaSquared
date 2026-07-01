package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentComponent;

/**
 * Client-only transient selection state for component-backed enchantment tooltips.
 */
public final class VSQEnchantmentTooltipState {
    private static int hoveredHash;
    private static int selectedIndex;
    private static long lastTooltipMillis;
    private static VSQEnchantmentComponent hoveredComponent;

    private VSQEnchantmentTooltipState() {
    }

    public static void onTooltip(int currentHash, VSQEnchantmentComponent component) {
        if (currentHash != hoveredHash) {
            hoveredHash = currentHash;
            selectedIndex = 0;
        }
        hoveredComponent = component;
        lastTooltipMillis = System.currentTimeMillis();
    }

    public static boolean isActive() {
        return System.currentTimeMillis() - lastTooltipMillis < 250L;
    }

    public static boolean cycleHovered(int delta) {
        if (!isActive() || hoveredComponent == null) {
            return false;
        }
        return cycle(hoveredComponent, delta);
    }

    public static int selectedIndex(VSQEnchantmentComponent component) {
        if (component == null) {
            selectedIndex = 0;
            return 0;
        }

        int slotTypeCount = component.definedSlotTypes().size();
        if (slotTypeCount == 0) {
            selectedIndex = 0;
            return 0;
        }
        if (selectedIndex >= slotTypeCount) {
            selectedIndex = 0;
        }
        return selectedIndex;
    }

    public static boolean cycle(VSQEnchantmentComponent component, int delta) {
        if (component == null) {
            selectedIndex = 0;
            return false;
        }

        int size = component.definedSlotTypes().size();
        if (size == 0) {
            selectedIndex = 0;
            return false;
        }

        selectedIndex = Math.floorMod(selectedIndex + delta, size);
        lastTooltipMillis = System.currentTimeMillis();
        return true;
    }
}
