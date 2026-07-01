package blob.vanillasquared.mixin.world.item;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.item.CreativeModeTab$ItemDisplayBuilder")
public abstract class CreativeInventoryMixin {

    @Inject(method = "accept(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/CreativeModeTab$TabVisibility;)V", at = @At("HEAD"), cancellable = true)
    private void vsq$filterEnchantedBooks(ItemStack stack, CreativeModeTab.TabVisibility tabVisibility, CallbackInfo ci) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            ci.cancel();
        }
    }
}
