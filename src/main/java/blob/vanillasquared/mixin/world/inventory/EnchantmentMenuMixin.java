package blob.vanillasquared.mixin.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static blob.vanillasquared.mixin.world.inventory.EnchantMenuMixinUtil.VSQ$DUMMYBLOCKREQUIREMENT;
import static blob.vanillasquared.mixin.world.inventory.EnchantMenuMixinUtil.VSQ$DUMMYLEVELREQUIREMENT;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {
    @Shadow
    @Final
    @Mutable
    private Container enchantSlots;

    @Unique
    private ContainerLevelAccess vsq$access = ContainerLevelAccess.NULL;

    @Unique
    private ServerPlayer vsq$serverPlayer;

    @Unique private int vsq$nearbyBlockCount;

    @Unique
    private Player vsq$player;

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
        this.vsq$debugNearbyBlocks(playerInventory, access);
        this.vsq$updateNearbyBlockCount();
    }

    @Inject(method = "slotsChanged(Lnet/minecraft/world/Container;)V", at = @At("TAIL"))
    private void vsq$refreshNearbyBlockCount(Container container, CallbackInfo ci) {
        this.vsq$updateNearbyBlockCount();
    }

    @Unique
    private void vsq$updateNearbyBlockCount() {
        if (this.vsq$serverPlayer == null) {
            return;
        }

        this.vsq$access.execute((Level level, BlockPos tablePos) -> {
            if (level.isClientSide()) {
                return;
            }

            this.vsq$nearbyBlockCount = this.vsq$countNearbyBlocks(level, tablePos);
        });
    }


    @Unique
    private int vsq$countNearbyBlocks(Level level, BlockPos tablePos) {
        int total = 0;
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
                    total++;
                }
            }
        }
        EnchantMenuMixinUtil.setBlockAmount(total);
        return total;
    }

    @Unique
    private void vsq$debugNearbyBlocks(Inventory playerInventory, ContainerLevelAccess access) {
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        access.execute((Level level, BlockPos tablePos) -> {
            if (level.isClientSide()) {
                return;
            }

            Map<String, Integer> counts = new TreeMap<>();

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

                        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                        if (key == null) {
                            continue;
                        }

                        String name = key.getPath().toUpperCase(Locale.ROOT);
                        counts.merge(name, 1, Integer::sum);
                    }
                }
            }

            if (counts.isEmpty()) {
                serverPlayer.sendSystemMessage(Component.literal("[vsq debug] Enchantment Table nearby blocks: none"));
                return;
            }

            StringBuilder sb = new StringBuilder("[vsq debug] Enchantment Table nearby blocks (r=2, y+0..2): ");
            boolean first = true;
            for (var entry : counts.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(entry.getKey()).append(" x").append(entry.getValue());
            }

            serverPlayer.sendSystemMessage(Component.literal(sb.toString()));
        });
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

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return EnchantmentMenuMixin.this.vsq$nearbyBlockCount;
            }

            @Override
            public void set(int value) {
                EnchantmentMenuMixin.this.vsq$nearbyBlockCount = value;
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return VSQ$DUMMYLEVELREQUIREMENT;
            }

            @Override
            public void set(int value) {
                // Do nothing
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return VSQ$DUMMYBLOCKREQUIREMENT;
            }

            @Override
            public void set(int value) {
                // Do nothing
            }
        });
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return VSQ$DUMMYBLOCKREQUIREMENT;
            }

            @Override
            public void set(int value) {
                // Do nothing
            }
        });

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
}
