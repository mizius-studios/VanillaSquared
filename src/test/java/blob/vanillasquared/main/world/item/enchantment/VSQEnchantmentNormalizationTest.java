package blob.vanillasquared.main.world.item.enchantment;

import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VSQEnchantmentNormalizationTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void restoringDefaultsRemovesBookEnchantmentRemovalPatch() {
        ItemStack stack = stackOf(Items.BOOK);
        ItemStack fresh = stackOf(Items.BOOK);

        stack.remove(DataComponents.ENCHANTMENTS);

        assertFalse(ItemStack.isSameItemSameComponents(stack, fresh));

        VSQEnchantments.restoreVanillaEnchantmentDefaults(stack);

        assertTrue(ItemStack.isSameItemSameComponents(stack, fresh));
    }

    @Test
    void restoringDefaultsRemovesEnchantedBookStoredEnchantmentRemovalPatch() {
        ItemStack stack = stackOf(Items.ENCHANTED_BOOK);
        ItemStack fresh = stackOf(Items.ENCHANTED_BOOK);

        stack.remove(DataComponents.STORED_ENCHANTMENTS);

        assertFalse(ItemStack.isSameItemSameComponents(stack, fresh));

        VSQEnchantments.restoreVanillaEnchantmentDefaults(stack);

        assertTrue(ItemStack.isSameItemSameComponents(stack, fresh));
    }

    @Test
    void restoringDefaultsRemovesUnexpectedEmptyStoredEnchantmentsOnPlainBooks() {
        ItemStack stack = stackOf(Items.BOOK);
        ItemStack fresh = stackOf(Items.BOOK);

        stack.set(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

        assertFalse(ItemStack.isSameItemSameComponents(stack, fresh));

        VSQEnchantments.restoreVanillaEnchantmentDefaults(stack);

        assertTrue(ItemStack.isSameItemSameComponents(stack, fresh));
    }

    private static ItemStack stackOf(Item item) {
        DataComponentMap prototype = DataComponentMap.EMPTY;
        if (item == Items.BOOK) {
            prototype = DataComponentMap.builder()
                    .set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                    .build();
        } else if (item == Items.ENCHANTED_BOOK) {
            prototype = DataComponentMap.builder()
                    .set(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
                    .build();
        }
        Holder<Item> holder = Holder.direct(item, prototype);
        return new ItemStack(holder);
    }
}
