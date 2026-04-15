package blob.vanillasquared.mixin.world.item;

import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/PatchedDataComponentMap;)V", at = @At("TAIL"))
    private void vsq$seedSlotComponent(Holder<Item> item, int count, PatchedDataComponentMap components, CallbackInfo ci) {
        VSQEnchantmentSlots.ensureSeeded((ItemStack) (Object) this);
        VSQEnchantmentSlots.syncDerivedEnchantments((ItemStack) (Object) this);
    }

    @Inject(method = "set", at = @At("TAIL"))
    private <T> void vsq$mirrorVanillaEnchantments(DataComponentType<T> type, @Nullable T value, CallbackInfoReturnable<T> cir) {
        if (VSQEnchantmentSlots.isDerivedSyncInProgress()) {
            return;
        }

        if (type == net.minecraft.core.component.DataComponents.ENCHANTMENTS || type == net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS) {
            VSQEnchantmentSlots.onVanillaEnchantmentsChanged((ItemStack) (Object) this, (ItemEnchantments) value);
        }
    }
}
