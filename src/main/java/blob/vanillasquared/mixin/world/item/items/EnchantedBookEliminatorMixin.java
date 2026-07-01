package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class EnchantedBookEliminatorMixin {

    @Final
    @Shadow
    private Holder<Item> item;

    @Inject(method = "set*", at = @At("HEAD"), cancellable = true)
    private <T> void vsq$neutralizeBookEnchantment(DataComponentType<? super T> type, T value,
            CallbackInfoReturnable<T> cir) {
        if (type == DataComponents.STORED_ENCHANTMENTS && value != null) {
            ItemStack stack = (ItemStack) (Object) this;
            if (stack.is(Items.ENCHANTED_BOOK)) {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void vsq$stripDeserializedBookEnchantments(CallbackInfo ci) {
        if (this.item == null) return;
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.is(Items.ENCHANTED_BOOK) && stack.has(DataComponents.STORED_ENCHANTMENTS)) {
            VSQEnchantments.restoreVanillaEnchantmentDefaults(stack);
        }
    }
}
