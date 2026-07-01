package blob.vanillasquared.mixin.world.item;

import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
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
        VSQEnchantments.ensureSeeded((ItemStack) (Object) this);
        VSQEnchantments.syncDerivedEnchantments((ItemStack) (Object) this);
    }

    @Inject(method = "set*", at = @At("TAIL"))
    private <T> void vsq$mirrorVanillaEnchantments(DataComponentType<T> type, @Nullable T value, CallbackInfoReturnable<T> cir) {
        if (VSQEnchantments.isDerivedSyncInProgress()) {
            return;
        }

        if (type == DataComponents.ENCHANTMENTS || type == DataComponents.STORED_ENCHANTMENTS) {
            VSQEnchantments.onVanillaEnchantmentsChanged((ItemStack) (Object) this, (ItemEnchantments) value);
        }
    }
}
