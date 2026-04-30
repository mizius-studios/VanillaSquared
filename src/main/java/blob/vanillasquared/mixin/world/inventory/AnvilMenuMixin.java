package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.util.api.modules.components.VSQDataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends AbstractContainerMenu {
    @Final
    @Shadow
    private DataSlot cost;

    protected AnvilMenuMixin(net.minecraft.world.inventory.MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void vsq$stopEnchantedBookCrafting(CallbackInfo ci) {
        if (this.slots.size() < 3) return;

        Slot secondaryInput = this.slots.get(1);
        Slot resultSlot = this.slots.get(2);

        if (secondaryInput.getItem().is(Items.ENCHANTED_BOOK)) {
            resultSlot.set(ItemStack.EMPTY);
            ci.cancel();
        }
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void vsq$normalizeSlottedAnvilResult(CallbackInfo ci) {
        if (this.slots.size() < 3) return;

        Slot inputSlot = this.slots.get(0);
        Slot resultSlot = this.slots.get(2);
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty() || !EnchantmentHelper.canStoreEnchantments(result)) {
            return;
        }

        VSQEnchantmentSlots.ensureSeeded(result);
        VSQEnchantmentComponent component = result.get(VSQDataComponents.ENCHANTMENT);
        if (component == null) {
            return;
        }

        var migrated = VSQEnchantmentSlots.tryPopulateFromVanilla(component, result, EnchantmentHelper.getEnchantmentsForCrafting(result));
        if (migrated.isEmpty()) {
            resultSlot.set(ItemStack.EMPTY);
            this.cost.set(0);
            return;
        }

        result.set(VSQDataComponents.ENCHANTMENT, migrated.get());
        VSQEnchantmentSlots.syncDerivedEnchantments(result);
        if (!inputSlot.getItem().isEmpty()) {
            resultSlot.set(result);
        }
    }
}
