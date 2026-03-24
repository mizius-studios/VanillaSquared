package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.network.payload.EnchantmentBlockCountsPayload;
import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProperties;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipe;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeInput;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import blob.vanillasquared.main.world.recipe.enchanting.VSQRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu implements VSQEnchantmentMenuProperties {
    @Unique
    private static final int VSQ$PROPERTY_PLAYER_LEVEL = 0;
    @Unique
    private static final int VSQ$PROPERTY_BLOCK_COUNT = 1;
    @Unique
    private static final int VSQ$PROPERTY_LEVEL_REQUIREMENT = 2;
    @Unique
    private static final int VSQ$PROPERTY_BLOCK_REQUIREMENT = 3;
    @Unique
    private static final int VSQ$DUMMY_LEVEL_REQUIREMENT = 69;
    @Unique
    private static final Map<Identifier, Integer> VSQ$DUMMY_DEBUG_BLOCKS = Map.of(
            BuiltInRegistries.BLOCK.getKey(Blocks.BOOKSHELF), 5,
            BuiltInRegistries.BLOCK.getKey(Blocks.CHISELED_BOOKSHELF), 2,
            BuiltInRegistries.BLOCK.getKey(Blocks.LECTERN), 3
    );

    @Shadow
    @Final
    @Mutable
    private Container enchantSlots;

    @Unique
    private ContainerLevelAccess vsq$access = ContainerLevelAccess.NULL;

    @Unique
    private ServerPlayer vsq$serverPlayer;

    @Unique
    private int vsq$playerLevel;
    @Unique
    private int vsq$nearbyBlockCount;
    @Unique
    private int vsq$levelRequirement = -1;
    @Unique
    private int vsq$blockRequirement = -1;
    @Unique
    private boolean vsq$hasRequiredXp;
    @Unique
    private boolean vsq$hasRequiredBlocks;
    @Unique
    private List<Component> vsq$detectedBlockTooltipLines = List.of();

    @Unique
    private Player vsq$player;

    @Unique
    private final ContainerData vsq$properties = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case VSQ$PROPERTY_PLAYER_LEVEL -> {
                    if (EnchantmentMenuMixin.this.vsq$player != null && !EnchantmentMenuMixin.this.vsq$player.level().isClientSide()) {
                        EnchantmentMenuMixin.this.vsq$playerLevel = EnchantmentMenuMixin.this.vsq$player.experienceLevel;
                    }
                    yield EnchantmentMenuMixin.this.vsq$playerLevel;
                }
                case VSQ$PROPERTY_BLOCK_COUNT -> EnchantmentMenuMixin.this.vsq$nearbyBlockCount;
                case VSQ$PROPERTY_LEVEL_REQUIREMENT -> EnchantmentMenuMixin.this.vsq$levelRequirement;
                case VSQ$PROPERTY_BLOCK_REQUIREMENT -> EnchantmentMenuMixin.this.vsq$blockRequirement;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case VSQ$PROPERTY_PLAYER_LEVEL -> EnchantmentMenuMixin.this.vsq$playerLevel = value;
                case VSQ$PROPERTY_BLOCK_COUNT -> EnchantmentMenuMixin.this.vsq$nearbyBlockCount = value;
                case VSQ$PROPERTY_LEVEL_REQUIREMENT -> EnchantmentMenuMixin.this.vsq$levelRequirement = value;
                case VSQ$PROPERTY_BLOCK_REQUIREMENT -> EnchantmentMenuMixin.this.vsq$blockRequirement = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    protected EnchantmentMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void vsq$rebuildSlotLayoutClient(int containerId, Inventory playerInventory, CallbackInfo ci) {
        this.vsq$player = playerInventory.player;
        this.vsq$rebuildSlotLayout(playerInventory);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void vsq$rebuildSlotLayoutServer(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.vsq$player = playerInventory.player;
        this.vsq$rebuildSlotLayout(playerInventory);
        this.vsq$access = access;
        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            this.vsq$serverPlayer = serverPlayer;
        }
        this.vsq$refresh();
    }

    @Inject(method = "slotsChanged(Lnet/minecraft/world/Container;)V", at = @At("TAIL"))
    private void vsq$refreshNearbyBlockCount(Container container, CallbackInfo ci) {
        this.vsq$refresh();
    }

    @Unique
    private void vsq$refresh() {
        if (this.vsq$serverPlayer == null) {
            return;
        }

        this.vsq$access.execute((Level level, BlockPos tablePos) -> {
            if (level.isClientSide()) {
                return;
            }

            Map<Identifier, Integer> detectedBlocks = this.vsq$collectDetectedBlocks(level, tablePos);
            Map<Identifier, Integer> requiredBlocks = VSQ$DUMMY_DEBUG_BLOCKS;
            this.vsq$nearbyBlockCount = detectedBlocks.values().stream().mapToInt(Integer::intValue).sum();
            this.vsq$blockRequirement = requiredBlocks.values().stream().mapToInt(Integer::intValue).sum();
            this.vsq$levelRequirement = VSQ$DUMMY_LEVEL_REQUIREMENT;
            this.vsq$sendDetectedBlockCounts(detectedBlocks, requiredBlocks, this.vsq$levelRequirement, this.vsq$player.experienceLevel);
        });
    }


    @Unique
    private Map<Identifier, Integer> vsq$collectDetectedBlocks(Level level, BlockPos tablePos) {
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

                    Block block = state.getBlock();
                    Identifier key = BuiltInRegistries.BLOCK.getKey(block);
                    if (!this.vsq$matchesDummyDebugBlock(key)) {
                        continue;
                    }
                    if (key == null) {
                        continue;
                    }

                    blockCounts.merge(key, 1, Integer::sum);
                }
            }
        }
        return blockCounts;
    }

    @Unique
    private void vsq$sendDetectedBlockCounts(Map<Identifier, Integer> detectedBlocks, Map<Identifier, Integer> requiredBlocks, int levelRequirement, int playerLevel) {
        if (this.vsq$serverPlayer == null) {
            return;
        }

        List<Identifier> blockIds = new ArrayList<>(detectedBlocks.size());
        List<Integer> blockCounts = new ArrayList<>(detectedBlocks.size());
        List<Identifier> requiredBlockIds = new ArrayList<>(requiredBlocks.size());
        List<Integer> requiredBlockCounts = new ArrayList<>(requiredBlocks.size());
        for (Map.Entry<Identifier, Integer> entry : detectedBlocks.entrySet()) {
            blockIds.add(entry.getKey());
            blockCounts.add(entry.getValue());
        }
        for (Map.Entry<Identifier, Integer> entry : requiredBlocks.entrySet()) {
            requiredBlockIds.add(entry.getKey());
            requiredBlockCounts.add(entry.getValue());
        }

        ServerPlayNetworking.send(this.vsq$serverPlayer, new EnchantmentBlockCountsPayload(this.containerId, blockIds, blockCounts, requiredBlockIds, requiredBlockCounts, playerLevel, levelRequirement));
    }

    @Unique
    private boolean vsq$matchesDummyDebugBlock(Identifier block) {
        for (Identifier candidate : VSQ$DUMMY_DEBUG_BLOCKS.keySet()) {
            if (candidate == block) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void vsq$rebuildSlotLayout(Inventory playerInventory) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;

        this.enchantSlots = new SimpleContainer(8) {
            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentMenuMixin.this.slotsChanged(this);
            }
        };

        this.slots.clear();
        accessor.vsq$getLastSlots().clear();
        accessor.vsq$getRemoteSlots().clear();

        this.addDataSlots(this.vsq$properties);

        this.addSlot(new Slot(this.enchantSlots, 0, 26, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.isEnchantable();
            }
        });

        this.addSlot(new Slot(this.enchantSlots, 1, 80, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }
        });

        this.addSlot(new Slot(this.enchantSlots, 2, 80, 18));
        this.addSlot(new Slot(this.enchantSlots, 3, 62, 36));
        this.addSlot(new Slot(this.enchantSlots, 4, 98, 36));
        this.addSlot(new Slot(this.enchantSlots, 5, 80, 54));

        this.vsq$addPlayerSlots(playerInventory);
    }

    @Unique
    private void vsq$addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            this.addSlot(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
        }
    }

    @Override
    public int vsq$getPlayerLevel() {
        return this.vsq$properties.get(VSQ$PROPERTY_PLAYER_LEVEL);
    }

    @Override
    public int vsq$getBlockAmount() {
        return this.vsq$properties.get(VSQ$PROPERTY_BLOCK_COUNT);
    }

    @Override
    public int vsq$getLevelRequirement() {
        return this.vsq$properties.get(VSQ$PROPERTY_LEVEL_REQUIREMENT);
    }

    @Override
    public int vsq$getBlockRequirement() {
        return this.vsq$properties.get(VSQ$PROPERTY_BLOCK_REQUIREMENT);
    }

    @Override
    public boolean vsq$hasRequiredXp() {
        return this.vsq$hasRequiredXp;
    }

    @Override
    public boolean vsq$hasRequiredBlocks() {
        return this.vsq$hasRequiredBlocks;
    }

    @Override
    public List<Component> vsq$getDetectedBlockTooltipLines() {
        return this.vsq$detectedBlockTooltipLines;
    }

    @Override
    public void vsq$setDetectedBlockCounts(int containerId, List<Identifier> blockIds, List<Integer> blockCounts, List<Identifier> requiredBlockIds, List<Integer> requiredBlockCounts, int levelRequirement, int playerLevel) {
        if (this.containerId != containerId) {
            return;
        }

        if (blockIds.size() != blockCounts.size()) {
            this.vsq$detectedBlockTooltipLines = List.of();
            return;
        }

        if (requiredBlockIds.size() != requiredBlockCounts.size()) {
            this.vsq$detectedBlockTooltipLines = List.of();
            return;
        }

        this.vsq$playerLevel = playerLevel;
        this.vsq$levelRequirement = levelRequirement;
        this.vsq$nearbyBlockCount = blockCounts.stream().mapToInt(Integer::intValue).sum();
        this.vsq$blockRequirement = requiredBlockCounts.stream().mapToInt(Integer::intValue).sum();
        this.vsq$hasRequiredXp = this.vsq$levelRequirement != -1 && this.vsq$playerLevel >= this.vsq$levelRequirement;
        this.vsq$hasRequiredBlocks = true;

        List<Component> tooltipLines = new ArrayList<>(requiredBlockIds.size());
        for (int j = 0; j < requiredBlockIds.size() && !requiredBlockIds.isEmpty(); j++) {
            int countRequired = requiredBlockCounts.get(j);
            Identifier requiredBlockId = requiredBlockIds.get(j);
            Block block = BuiltInRegistries.BLOCK.getValue(requiredBlockId);

            int detectedCount = 0;
            boolean foundRequiredBlock = false;
            for (int i = 0; i < blockIds.size() && !blockIds.isEmpty(); i++) {
                Identifier blockId = blockIds.get(i);
                if (blockId.equals(requiredBlockId)) {
                    detectedCount = blockCounts.get(i);
                    foundRequiredBlock = true;
                    break;
                }
            }

            if (!foundRequiredBlock) {
                detectedCount = 0;
            }
            if (detectedCount < countRequired) {
                this.vsq$hasRequiredBlocks = false;
            }

            MutableComponent line = Component.translatable(
                    "vsq.gui.container.enchantment_table.blocks.tooltip.entry",
                    detectedCount,
                    countRequired,
                    block.getName()
            );
            tooltipLines.add(line);
            this.vsq$detectedBlockTooltipLines = Collections.unmodifiableList(tooltipLines);
        }
        if (requiredBlockIds.isEmpty()) {
            this.vsq$hasRequiredBlocks = false;
            this.vsq$detectedBlockTooltipLines = List.of(Component.translatable("vsq.gui.container.enchantment_table.blocks.tooltip.none"));
        }
    }

    @Override
    public boolean vsq$tryCraftEnchantingRecipe(ServerPlayer player) {
        if (player.containerMenu != this) {
            return false;
        }

        EnchantingRecipeInput recipeInput = this.vsq$createRecipeInput();
        List<RecipeHolder<EnchantingRecipe>> enchantingRecipes = List.copyOf(EnchantingRecipeRegistry.recipes());
        Optional<RecipeHolder<EnchantingRecipe>> recipeHolder = EnchantingRecipeRegistry.findFirstMatch(recipeInput, player.level());
        if (recipeHolder.isEmpty()) {
            VanillaSquared.LOGGER.info(
                    "No Enchanting recipe matched. loadedEnchantingRecipes={}, input={}, material={}, cross=[{},{},{},{}]",
                    enchantingRecipes.size(),
                    this.getSlot(0).getItem(),
                    this.getSlot(1).getItem(),
                    this.getSlot(2).getItem(),
                    this.getSlot(3).getItem(),
                    this.getSlot(4).getItem(),
                    this.getSlot(5).getItem()
            );
            return false;
        }

        RecipeHolder<EnchantingRecipe> holder = recipeHolder.get();
        EnchantingRecipe recipe = holder.value();
        ItemStack result = recipe.assemble(recipeInput);
        if (result.isEmpty()) {
            VanillaSquared.LOGGER.warn("Enchanting recipe {} assembled to an empty stack", holder.id());
            return false;
        }

        this.getSlot(1).remove(1);
        for (int slotIndex = 2; slotIndex <= 5; slotIndex++) {
            this.getSlot(slotIndex).remove(1);
        }
        this.getSlot(0).set(result.copyWithCount(1));
        this.slotsChanged(this.enchantSlots);
        this.broadcastChanges();
        VanillaSquared.LOGGER.info("Applied Enchanting recipe {} -> {}", holder.id(), result);
        return true;
    }

    @Unique
    private EnchantingRecipeInput vsq$createRecipeInput() {
        return new EnchantingRecipeInput(
                this.getSlot(0).getItem().copy(),
                this.getSlot(1).getItem().copy(),
                List.of(
                        this.getSlot(2).getItem().copy(),
                        this.getSlot(3).getItem().copy(),
                        this.getSlot(4).getItem().copy(),
                        this.getSlot(5).getItem().copy()
                )
        );
    }
}
