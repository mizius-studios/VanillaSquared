package blob.vanillasquared.mixin.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {
    @Shadow
    @Final
    @Mutable
    private Container enchantSlots;

    protected EnchantmentMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void vsq$rebuildSlotLayoutClient(int containerId, Inventory playerInventory, CallbackInfo ci) {
        this.vsq$rebuildSlotLayout(playerInventory);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void vsq$rebuildSlotLayoutServer(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.vsq$rebuildSlotLayout(playerInventory);
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

        this.addSlot(new Slot(this.enchantSlots, 6, 134, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.EXPERIENCE_BOTTLE);
            }
        });

        this.addSlot(new Slot(this.enchantSlots, 7, 152, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOKSHELF);
            }
        });

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
