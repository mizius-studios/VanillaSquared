package blob.vanillasquared.main.world.inventory;

import blob.vanillasquared.main.network.payload.EnchantingRecipeBookSyncPayload;
import blob.vanillasquared.main.network.payload.EnchantingRecipeStatePayload;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingBlockRequirement;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingIngredient;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipe;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeInput;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VSQEnchantmentMenu extends RecipeBookMenu implements VSQEnchantmentMenuProperties {
    public static final BlockPos SYNTHETIC_OPEN_POS = new BlockPos(0, -2_048, 0);
    public static final int INPUT_SLOT = EnchantingSlotLayout.INPUT_SLOT;
    public static final int TABLE_SLOT_COUNT = EnchantingSlotLayout.TABLE_SLOT_COUNT;
    private static final Identifier ENCHANTABLE_TAG_ID = Identifier.fromNamespaceAndPath("vsq", "enchanting/enchantable");
    private static final Identifier MATERIAL_TAG_ID = Identifier.fromNamespaceAndPath("vsq", "enchanting/material");
    private static final TagKey<Item> ENCHANTABLE_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM, ENCHANTABLE_TAG_ID);
    private static final TagKey<Item> MATERIAL_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM, MATERIAL_TAG_ID);

    private final Container enchantSlots = new SimpleContainer(EnchantingSlotLayout.TABLE_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            VSQEnchantmentMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final Player player;
    private final boolean requireEnchantingTable;

    private int playerLevel = -1;
    private int nearbyBlockCount = -1;
    private int levelRequirement = -1;
    private int blockRequirement = -1;
    private boolean hasRequiredXp;
    private boolean hasRequiredBlocks;
    private List<Component> detectedBlockTooltipLines = List.of();
    private List<Component> bookTooltipLines = List.of();
    private int selectedDisplayId = -1;
    private final Map<Integer, RecipeHolder<EnchantingRecipe>> displayRecipes = new LinkedHashMap<>();

    public VSQEnchantmentMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, SYNTHETIC_OPEN_POS);
    }

    public VSQEnchantmentMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(VSQMenuTypes.ENCHANTING, containerId);
        this.requireEnchantingTable = !SYNTHETIC_OPEN_POS.equals(blockPos);
        this.access = this.requireEnchantingTable
                ? ContainerLevelAccess.create(playerInventory.player.level(), blockPos)
                : ContainerLevelAccess.NULL;
        this.player = playerInventory.player;
        this.vsq$rebuildRecipeBookIndex();
        this.addSlot(new Slot(this.enchantSlots, EnchantingSlotLayout.INPUT_SLOT, 26, 23));
        this.addSlot(new Slot(this.enchantSlots, EnchantingSlotLayout.MATERIAL_SLOT, 80, 36));
        this.addSlot(new Slot(this.enchantSlots, EnchantingSlotLayout.FIRST_CROSS_SLOT, 80, 18));
        this.addSlot(new Slot(this.enchantSlots, EnchantingSlotLayout.FIRST_CROSS_SLOT + 1, 62, 36));
        this.addSlot(new Slot(this.enchantSlots, EnchantingSlotLayout.FIRST_CROSS_SLOT + 2, 98, 36));
        this.addSlot(new Slot(this.enchantSlots, EnchantingSlotLayout.FIRST_CROSS_SLOT + 3, 80, 54));
        EnchantingMenuSharedLogic.addPlayerSlots(this::addSlot, playerInventory);
        if (this.player instanceof ServerPlayer serverPlayer) {
            this.vsq$refresh(serverPlayer);
        }
    }

    @Override
    public PostPlaceAction handlePlacement(boolean placeAll, boolean creative, RecipeHolder<?> recipe, net.minecraft.server.level.ServerLevel level, Inventory inventory) {
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return PostPlaceAction.NOTHING;
        }
        if (!(recipe.value() instanceof EnchantingRecipe)) {
            return PostPlaceAction.NOTHING;
        }
        Optional<Integer> displayId = this.displayRecipes.entrySet().stream()
                .filter(entry -> entry.getValue().id().equals(recipe.id()))
                .map(Map.Entry::getKey)
                .findFirst();
        if (displayId.isEmpty()) {
            return PostPlaceAction.NOTHING;
        }

        this.vsq$setSelectedDisplayId(displayId.get());
        return this.vsq$handleRecipeBookPlacement(serverPlayer, displayId.get()) ? PostPlaceAction.NOTHING : PostPlaceAction.PLACE_GHOST_RECIPE;
    }

    @Override
    public void fillCraftSlotsStackedContents(net.minecraft.world.entity.player.StackedItemContents stackedItemContents) {
        for (int slotIndex = 0; slotIndex < this.enchantSlots.getContainerSize(); slotIndex++) {
            stackedItemContents.accountStack(this.enchantSlots.getItem(slotIndex));
        }
        Inventory inventory = this.player.getInventory();
        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {
            stackedItemContents.accountStack(inventory.getItem(slotIndex));
        }
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (this.player instanceof ServerPlayer serverPlayer) {
            this.vsq$refresh(serverPlayer);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.requireEnchantingTable || stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.enchantSlots);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < EnchantingSlotLayout.TABLE_SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, EnchantingSlotLayout.PLAYER_INV_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ENCHANTABLE_TAG)) {
            if (!this.moveItemStackTo(stack, EnchantingSlotLayout.INPUT_SLOT, EnchantingSlotLayout.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(MATERIAL_TAG)) {
            if (!this.moveItemStackTo(stack, EnchantingSlotLayout.MATERIAL_SLOT, EnchantingSlotLayout.MATERIAL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, EnchantingSlotLayout.FIRST_CROSS_SLOT, EnchantingSlotLayout.FIRST_CROSS_SLOT + EnchantingSlotLayout.CROSS_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    @Override
    public int vsq$getPlayerLevel() {
        return this.playerLevel;
    }

    @Override
    public int vsq$getBlockAmount() {
        return this.nearbyBlockCount;
    }

    @Override
    public int vsq$getLevelRequirement() {
        return this.levelRequirement;
    }

    @Override
    public int vsq$getBlockRequirement() {
        return this.blockRequirement;
    }

    @Override
    public boolean vsq$hasRequiredXp() {
        return this.hasRequiredXp;
    }

    @Override
    public boolean vsq$hasRequiredBlocks() {
        return this.hasRequiredBlocks;
    }

    @Override
    public List<Component> vsq$getDetectedBlockTooltipLines() {
        return this.detectedBlockTooltipLines;
    }

    @Override
    public List<Component> vsq$getBookTooltipLines() {
        return this.bookTooltipLines;
    }

    @Override
    public void vsq$applyRecipeState(int containerId, List<Identifier> blockIds, List<Integer> counts, List<Integer> requiredBlockCounts, int levelRequirement, int blockRequirement, int playerLevel, Component recipeName, Component recipeDescription) {
        if (this.containerId != containerId) {
            return;
        }

        this.playerLevel = playerLevel;
        this.levelRequirement = levelRequirement;
        this.nearbyBlockCount = blockRequirement == -1 ? -1 : counts.stream().mapToInt(Integer::intValue).sum();
        this.blockRequirement = blockRequirement;
        this.hasRequiredXp = this.levelRequirement != -1 && this.playerLevel >= this.levelRequirement;
        this.hasRequiredBlocks = this.blockRequirement != -1;
        this.bookTooltipLines = this.levelRequirement == -1 || this.blockRequirement == -1 ? List.of() : List.of(recipeName.copy(), recipeDescription.copy());
        EnchantingMenuSharedLogic.TooltipBuildResult tooltipBuildResult = EnchantingMenuSharedLogic.buildDetectedBlockTooltipLines(blockIds, counts, requiredBlockCounts, this.blockRequirement != -1);
        this.detectedBlockTooltipLines = tooltipBuildResult.lines();
        this.hasRequiredBlocks = tooltipBuildResult.hasRequiredBlocks();

        if (this.blockRequirement == -1) {
            this.hasRequiredBlocks = false;
            this.bookTooltipLines = List.of();
            this.detectedBlockTooltipLines = List.of(Component.translatable("vsq.gui.container.enchantment_table.blocks.tooltip.none"));
        } else if (blockIds.isEmpty()) {
            this.hasRequiredBlocks = true;
            this.detectedBlockTooltipLines = List.of(Component.translatable("vsq.gui.container.enchantment_table.blocks.tooltip.none"));
        }
    }

    @Override
    public boolean vsq$tryCraftEnchantingRecipe(ServerPlayer player) {
        if (player.containerMenu != this) {
            return false;
        }

        EnchantingRecipeInput input = this.vsq$createRecipeInput();
        Map<Identifier, Integer> detectedBlocks = this.vsq$collectDetectedBlocks();
        Optional<RecipeHolder<EnchantingRecipe>> recipeHolder = this.vsq$getCraftingRecipe(input, detectedBlocks, player.registryAccess(), player.experienceLevel);
        if (recipeHolder.isEmpty()) {
            return false;
        }

        EnchantingRecipe recipe = recipeHolder.get().value();
        Optional<EnchantingRecipe.Match> match = recipe.findMatch(input);
        if (match.isEmpty()) {
            return false;
        }

        ItemStack result = recipe.assemble(input, player.registryAccess());
        if (result.isEmpty()) {
            return false;
        }

        this.getSlot(EnchantingSlotLayout.MATERIAL_SLOT).remove(recipe.material().count());
        for (int ingredientIndex = 0; ingredientIndex < recipe.ingredients().size(); ingredientIndex++) {
            int matchedSlotIndex = match.get().matchedCrossSlots().get(ingredientIndex);
            this.getSlot(EnchantingSlotLayout.FIRST_CROSS_SLOT + matchedSlotIndex).remove(recipe.ingredients().get(ingredientIndex).count());
        }
        if (recipe.consumedLevels() > 0) {
            player.giveExperienceLevels(-recipe.consumedLevels());
        }
        this.getSlot(EnchantingSlotLayout.INPUT_SLOT).set(result);
        this.broadcastChanges();
        this.vsq$refresh(player);
        return true;
    }

    public void vsq$setSelectedDisplayId(int selectedDisplayId) {
        this.selectedDisplayId = selectedDisplayId;
        if (this.player instanceof ServerPlayer serverPlayer) {
            this.vsq$refresh(serverPlayer);
        }
    }

    public boolean vsq$handleRecipeBookPlacement(ServerPlayer player, int displayId) {
        RecipeHolder<EnchantingRecipe> recipeHolder = this.displayRecipes.get(displayId);
        if (recipeHolder == null) {
            return false;
        }

        Optional<PlannedRecipePlacement> plannedPlacement = this.vsq$planRecipePlacement(recipeHolder.value(), player.registryAccess());
        if (plannedPlacement.isEmpty()) {
            return false;
        }

        if (!this.vsq$returnInputsToInventory()) {
            return false;
        }
        if (!this.vsq$applyPlannedPlacement(plannedPlacement.get())) {
            this.vsq$returnInputsToInventory();
            return false;
        }

        this.broadcastChanges();
        this.vsq$refresh(player);
        return true;
    }

    private boolean vsq$returnInputsToInventory() {
        for (int slotIndex = 0; slotIndex < EnchantingSlotLayout.TABLE_SLOT_COUNT; slotIndex++) {
            ItemStack stack = this.enchantSlots.getItem(slotIndex);
            if (stack.isEmpty()) {
                continue;
            }
            this.player.getInventory().placeItemBackInInventory(stack.copy(), false);
            this.enchantSlots.setItem(slotIndex, ItemStack.EMPTY);
        }
        return true;
    }

    private ItemStack vsq$extractExactItems(Item item, int count) {
        Inventory inventory = this.player.getInventory();
        ItemStack extracted = ItemStack.EMPTY;
        int remaining = count;
        for (int slotIndex = 0; slotIndex < inventory.getContainerSize(); slotIndex++) {
            ItemStack stack = inventory.getItem(slotIndex);
            if (!stack.is(item)) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            if (taken <= 0) {
                continue;
            }

            ItemStack split = stack.split(taken);
            if (extracted.isEmpty()) {
                extracted = split;
            } else {
                extracted.grow(split.getCount());
            }
            remaining -= taken;
            if (remaining == 0) {
                extracted.setCount(count);
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    private void vsq$refresh(ServerPlayer player) {
        this.playerLevel = player.experienceLevel;
        this.vsq$sendRecipeBookSync(player, true);
        Map<Identifier, Integer> detectedBlocks = this.vsq$collectDetectedBlocks();
        EnchantingRecipeInput input = this.vsq$createRecipeInput();
        Optional<RecipeHolder<EnchantingRecipe>> recipeHolder = this.vsq$getPreviewRecipe(input, player.registryAccess());
        if (recipeHolder.isEmpty()) {
            this.levelRequirement = -1;
            this.blockRequirement = -1;
            this.nearbyBlockCount = -1;
            this.vsq$sendDetectedBlockCounts(List.of(), -1, -1, player.experienceLevel, Component.empty(), Component.empty(), player);
            return;
        }

        EnchantingRecipe recipe = recipeHolder.get().value();
        List<EnchantingRecipe.BlockRequirementDisplay> blockDisplay = recipe.blockRequirementDisplay(detectedBlocks);
        this.levelRequirement = recipe.level();
        this.blockRequirement = recipe.blocks().stream().mapToInt(EnchantingBlockRequirement::count).sum();
        this.nearbyBlockCount = this.blockRequirement == -1 ? -1 : blockDisplay.stream().mapToInt(EnchantingRecipe.BlockRequirementDisplay::placedCount).sum();
        this.vsq$sendDetectedBlockCounts(blockDisplay, this.levelRequirement, this.blockRequirement, player.experienceLevel, recipe.name(), recipe.description(), player);
    }

    private Optional<RecipeHolder<EnchantingRecipe>> vsq$getPreviewRecipe(EnchantingRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        if (this.selectedDisplayId != -1) {
            RecipeHolder<EnchantingRecipe> selected = this.displayRecipes.get(this.selectedDisplayId);
            if (selected != null) {
                return Optional.of(selected);
            }
            this.selectedDisplayId = -1;
        }
        return EnchantingRecipeRegistry.findFirstStructuralMatch(input, registries);
    }

    private Optional<RecipeHolder<EnchantingRecipe>> vsq$getCraftingRecipe(EnchantingRecipeInput input, Map<Identifier, Integer> detectedBlocks, net.minecraft.core.HolderLookup.Provider registries, int playerLevel) {
        if (this.selectedDisplayId != -1) {
            RecipeHolder<EnchantingRecipe> selected = this.displayRecipes.get(this.selectedDisplayId);
            if (selected != null
                    && selected.value().findMatch(input).isPresent()
                    && selected.value().isCompatibleInput(input, registries)
                    && selected.value().wouldModifyInput(input, registries)
                    && selected.value().canPlayerCraft(playerLevel)
                    && selected.value().hasRequiredBlocks(detectedBlocks)) {
                return Optional.of(selected);
            }
            return Optional.empty();
        }

        return EnchantingRecipeRegistry.findFirstCraftableMatch(input, playerLevel, detectedBlocks, registries);
    }

    private Map<Identifier, Integer> vsq$collectDetectedBlocks() {
        List<Map<Identifier, Integer>> detectedBlocks = new ArrayList<>(1);
        this.access.execute((level, tablePos) -> detectedBlocks.add(EnchantingMenuSharedLogic.collectDetectedBlocks(level, tablePos)));
        return detectedBlocks.isEmpty() ? Map.of() : detectedBlocks.getFirst();
    }

    private void vsq$sendDetectedBlockCounts(List<EnchantingRecipe.BlockRequirementDisplay> blockDisplay, int levelRequirement, int blockRequirement, int playerLevel, Component recipeName, Component recipeDescription, ServerPlayer player) {
        EnchantingMenuSharedLogic.BlockDisplayLists blockDisplayLists = EnchantingMenuSharedLogic.flattenBlockDisplay(blockDisplay);
        ServerPlayNetworking.send(player, new EnchantingRecipeStatePayload(this.containerId, blockDisplayLists.blockIds(), blockDisplayLists.blockCounts(), blockDisplayLists.requiredBlockCounts(), playerLevel, levelRequirement, blockRequirement, recipeName, recipeDescription));
    }

    private EnchantingRecipeInput vsq$createRecipeInput() {
        return EnchantingMenuSharedLogic.createRecipeInput(this.enchantSlots::getItem);
    }

    private void vsq$rebuildRecipeBookIndex() {
        this.displayRecipes.clear();
        int displayId = 0;
        for (RecipeHolder<EnchantingRecipe> holder : EnchantingRecipeRegistry.recipes()) {
            this.displayRecipes.put(displayId++, holder);
        }
    }

    private void vsq$sendRecipeBookSync(ServerPlayer player, boolean replace) {
        this.vsq$rebuildRecipeBookIndex();
        List<EnchantingRecipeBookSyncPayload.RecipeView> recipeViews = new ArrayList<>(this.displayRecipes.size());
        for (RecipeHolder<EnchantingRecipe> holder : this.displayRecipes.values()) {
            Optional<PlannedRecipePlacement> plannedPlacement = this.vsq$planRecipePlacement(holder.value(), player.registryAccess());
            recipeViews.add(new EnchantingRecipeBookSyncPayload.RecipeView(holder, plannedPlacement.map(PlannedRecipePlacement::input), plannedPlacement.isPresent()));
        }
        ServerPlayNetworking.send(player, EnchantingRecipeBookSyncPayload.create(this.containerId, replace, recipeViews, player.registryAccess()));
    }

    private Optional<PlannedRecipePlacement> vsq$planRecipePlacement(EnchantingRecipe recipe, net.minecraft.core.HolderLookup.Provider registries) {
        List<SlotRequirement> requirements = new ArrayList<>(EnchantingSlotLayout.TABLE_SLOT_COUNT);
        requirements.add(new SlotRequirement(EnchantingSlotLayout.INPUT_SLOT, recipe.input()));
        requirements.add(new SlotRequirement(EnchantingSlotLayout.MATERIAL_SLOT, recipe.material()));
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            requirements.add(new SlotRequirement(EnchantingSlotLayout.FIRST_CROSS_SLOT + index, recipe.ingredients().get(index)));
        }

        LinkedHashMap<Item, Integer> availableCounts = new LinkedHashMap<>();
        this.vsq$accountAvailableStacks(availableCounts, this.player.getInventory()::getItem, this.player.getInventory().getContainerSize());
        this.vsq$accountAvailableStacks(availableCounts, this.enchantSlots::getItem, EnchantingSlotLayout.TABLE_SLOT_COUNT);

        ItemStack[] plannedSlots = new ItemStack[EnchantingSlotLayout.TABLE_SLOT_COUNT];
        Arrays.fill(plannedSlots, ItemStack.EMPTY);
        return this.vsq$planRequirements(requirements, 0, availableCounts, plannedSlots, recipe, registries)
                ? Optional.of(new PlannedRecipePlacement(
                        this.vsq$plannedInput(plannedSlots),
                        List.of(
                                plannedSlots[EnchantingSlotLayout.INPUT_SLOT].copy(),
                                plannedSlots[EnchantingSlotLayout.MATERIAL_SLOT].copy(),
                                plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT].copy(),
                                plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT + 1].copy(),
                                plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT + 2].copy(),
                                plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT + 3].copy()
                        )
                ))
                : Optional.empty();
    }

    private boolean vsq$planRequirements(List<SlotRequirement> requirements, int requirementIndex, LinkedHashMap<Item, Integer> availableCounts, ItemStack[] plannedSlots, EnchantingRecipe recipe, net.minecraft.core.HolderLookup.Provider registries) {
        if (requirementIndex >= requirements.size()) {
            EnchantingRecipeInput plannedInput = this.vsq$plannedInput(plannedSlots);
            return recipe.findMatch(plannedInput).isPresent()
                    && recipe.isCompatibleInput(plannedInput, registries)
                    && recipe.wouldModifyInput(plannedInput, registries);
        }

        SlotRequirement requirement = requirements.get(requirementIndex);
        for (Map.Entry<Item, Integer> entry : availableCounts.entrySet()) {
            if (entry.getValue() < requirement.ingredient().count()) {
                continue;
            }

            ItemStack candidate = new ItemStack(entry.getKey(), requirement.ingredient().count());
            if (!requirement.ingredient().matchesIgnoringCount(candidate)) {
                continue;
            }

            entry.setValue(entry.getValue() - requirement.ingredient().count());
            plannedSlots[requirement.slotIndex()] = candidate;
            if (this.vsq$planRequirements(requirements, requirementIndex + 1, availableCounts, plannedSlots, recipe, registries)) {
                return true;
            }
            plannedSlots[requirement.slotIndex()] = ItemStack.EMPTY;
            entry.setValue(entry.getValue() + requirement.ingredient().count());
        }

        return false;
    }

    private void vsq$accountAvailableStacks(LinkedHashMap<Item, Integer> availableCounts, java.util.function.IntFunction<ItemStack> slotReader, int slotCount) {
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            ItemStack stack = slotReader.apply(slotIndex);
            if (stack.isEmpty()) {
                continue;
            }
            availableCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
    }

    private EnchantingRecipeInput vsq$plannedInput(ItemStack[] plannedSlots) {
        return new EnchantingRecipeInput(
                plannedSlots[EnchantingSlotLayout.INPUT_SLOT].copy(),
                plannedSlots[EnchantingSlotLayout.MATERIAL_SLOT].copy(),
                List.of(
                        plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT].copy(),
                        plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT + 1].copy(),
                        plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT + 2].copy(),
                        plannedSlots[EnchantingSlotLayout.FIRST_CROSS_SLOT + 3].copy()
                )
        );
    }

    private boolean vsq$applyPlannedPlacement(PlannedRecipePlacement plannedPlacement) {
        for (int slotIndex = 0; slotIndex < plannedPlacement.slotStacks().size(); slotIndex++) {
            ItemStack plannedStack = plannedPlacement.slotStacks().get(slotIndex);
            if (plannedStack.isEmpty()) {
                this.enchantSlots.setItem(slotIndex, ItemStack.EMPTY);
                continue;
            }

            ItemStack extracted = this.vsq$extractExactItems(plannedStack.getItem(), plannedStack.getCount());
            if (extracted.isEmpty() || extracted.getCount() != plannedStack.getCount()) {
                return false;
            }
            this.enchantSlots.setItem(slotIndex, extracted);
        }
        return true;
    }

    public List<Slot> vsq$getEnchantingSlots() {
        return this.slots.subList(0, EnchantingSlotLayout.TABLE_SLOT_COUNT);
    }

    public Map<Integer, RecipeHolder<EnchantingRecipe>> vsq$getDisplayRecipes() {
        return Map.copyOf(this.displayRecipes);
    }

    private record SlotRequirement(int slotIndex, EnchantingIngredient ingredient) {
    }

    private record PlannedRecipePlacement(EnchantingRecipeInput input, List<ItemStack> slotStacks) {
    }
}
