package blob.vanillasquared.main.world.item.enchantment;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentTooltipState;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VSQEnchantmentComponentTooltipTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
    }

    @Test
    void componentBuildsCollapsedAndExpandedTooltipWithEmptyAndFilledSlots() {
        VSQEnchantmentComponent component = componentWithTwoGroups();

        List<Component> collapsed = component.tooltipLines(0, false);
        assertEquals(6, collapsed.size());
        assertTranslationKey("vsq.tooltip.enchantment_slots.header", collapsed.get(1));
        assertTranslationKey("vsq.tooltip.enchantment_slots.slot.selected", collapsed.get(2));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD), collapsed.get(2).getStyle().getColor());
        assertTranslationKey("vsq.tooltip.enchantment_slots.slot", collapsed.get(3));

        List<Component> expanded = component.tooltipLines(0, true);
        assertEquals(8, expanded.size());
        assertTranslationKey("vsq.tooltip.enchantment_slots.empty", expanded.get(3).getSiblings().getFirst());
        assertTranslationKey("enchantment.minecraft.sharpness", expanded.get(4).getSiblings().getFirst());
        assertTranslationKey("vsq.tooltip.enchantment_slots.slot", expanded.get(5));
    }

    @Test
    void selectionWrapsAndRejectsMissingOrUndefinedComponents() {
        VSQEnchantmentComponent component = componentWithTwoGroups();
        VSQEnchantmentComponent empty = new VSQEnchantmentComponent(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()
        );

        VSQEnchantmentTooltipState.onTooltip(10, component);
        assertTrue(VSQEnchantmentTooltipState.cycleHovered(-1));
        assertEquals(1, VSQEnchantmentTooltipState.selectedIndex(component));
        assertTrue(VSQEnchantmentTooltipState.cycleHovered(1));
        assertEquals(0, VSQEnchantmentTooltipState.selectedIndex(component));

        VSQEnchantmentTooltipState.onTooltip(11, component);
        assertEquals(0, VSQEnchantmentTooltipState.selectedIndex(component));
        assertFalse(VSQEnchantmentTooltipState.cycle(null, 1));
        assertFalse(VSQEnchantmentTooltipState.cycle(empty, 1));
        VSQEnchantmentTooltipState.onTooltip(12, null);
        assertFalse(VSQEnchantmentTooltipState.cycleHovered(1));
    }

    private static VSQEnchantmentComponent componentWithTwoGroups() {
        return new VSQEnchantmentComponent(
                Optional.of(List.of(VSQEnchantmentSlotEntry.empty(), VSQEnchantmentSlotEntry.of(Holder.direct(sharpness().value()), 3))),
                Optional.of(List.of()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        );
    }

    private static Holder<Enchantment> sharpness() {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
    }

    private static void assertTranslationKey(String expected, Component component) {
        TranslatableContents contents = (TranslatableContents) component.getContents();
        assertEquals(expected, contents.getKey());
    }
}
