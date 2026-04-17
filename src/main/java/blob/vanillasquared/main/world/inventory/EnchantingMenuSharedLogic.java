package blob.vanillasquared.main.world.inventory;

import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipe;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EnchantingMenuSharedLogic {
    private EnchantingMenuSharedLogic() {
    }

    public static void addPlayerSlots(Consumer<Slot> slotAdder, Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotAdder.accept(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            slotAdder.accept(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
        }
    }

    public static EnchantingRecipeInput createRecipeInput(IntFunction<ItemStack> slotReader) {
        return new EnchantingRecipeInput(
                slotReader.apply(EnchantingSlotLayout.INPUT_SLOT).copy(),
                slotReader.apply(EnchantingSlotLayout.MATERIAL_SLOT).copy(),
                getCrossSlotItems(slotReader)
        );
    }

    public static List<ItemStack> getCrossSlotItems(IntFunction<ItemStack> slotReader) {
        List<ItemStack> crossSlotItems = new ArrayList<>(EnchantingSlotLayout.CROSS_SLOT_COUNT);
        for (int index = 0; index < EnchantingSlotLayout.CROSS_SLOT_COUNT; index++) {
            crossSlotItems.add(slotReader.apply(EnchantingSlotLayout.FIRST_CROSS_SLOT + index).copy());
        }
        return List.copyOf(crossSlotItems);
    }

    public static Map<Identifier, Integer> collectDetectedBlocks(Level level, BlockPos tablePos) {
        Map<Identifier, Integer> blockCounts = new TreeMap<>(Identifier::compareTo);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos pos = tablePos.offset(dx, dy, dz);
                    if (pos.equals(tablePos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }

                    Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (key != null) {
                        blockCounts.merge(key, 1, Integer::sum);
                    }
                }
            }
        }
        return blockCounts;
    }

    public static TooltipBuildResult buildDetectedBlockTooltipLines(List<Identifier> blockIds, List<Integer> blockCounts, List<Integer> requiredBlockCounts, boolean hasRequiredBlocksInitially) {
        List<Component> tooltipLines = new ArrayList<>(blockIds.size());
        boolean hasRequiredBlocks = hasRequiredBlocksInitially;
        for (int index = 0; index < blockIds.size(); index++) {
            int requiredCount = requiredBlockCounts.get(index);
            int detectedCount = blockCounts.get(index);
            Block block = BuiltInRegistries.BLOCK.getValue(blockIds.get(index));
            if (detectedCount < requiredCount) {
                hasRequiredBlocks = false;
            }

            MutableComponent line = Component.translatable(
                    "vsq.gui.container.enchantment_table.blocks.tooltip.entry",
                    detectedCount,
                    requiredCount,
                    block.getName()
            );
            tooltipLines.add(line);
        }
        return new TooltipBuildResult(Collections.unmodifiableList(tooltipLines), hasRequiredBlocks);
    }

    public static BlockDisplayLists flattenBlockDisplay(List<EnchantingRecipe.BlockRequirementDisplay> blockDisplay) {
        List<Identifier> blockIds = new ArrayList<>(blockDisplay.size());
        List<Integer> blockCounts = new ArrayList<>(blockDisplay.size());
        List<Integer> requiredBlockCounts = new ArrayList<>(blockDisplay.size());
        for (EnchantingRecipe.BlockRequirementDisplay entry : blockDisplay) {
            blockIds.add(entry.blockId());
            blockCounts.add(entry.placedCount());
            requiredBlockCounts.add(entry.requiredCount());
        }
        return new BlockDisplayLists(List.copyOf(blockIds), List.copyOf(blockCounts), List.copyOf(requiredBlockCounts));
    }

    public record TooltipBuildResult(List<Component> lines, boolean hasRequiredBlocks) {
    }

    public record BlockDisplayLists(List<Identifier> blockIds, List<Integer> blockCounts, List<Integer> requiredBlockCounts) {
    }
}
