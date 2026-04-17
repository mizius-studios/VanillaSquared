package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(RecipeButton.class)
public abstract class RecipeButtonMixin {
    @Inject(method = "getTooltipText", at = @At("HEAD"), cancellable = true)
    private void vsq$replaceEnchantingRecipeBookTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        if (!(Minecraft.getInstance().screen instanceof VSQEnchantmentScreen)) {
            return;
        }
        cir.setReturnValue(List.of(stack.getHoverName()));
    }
}
