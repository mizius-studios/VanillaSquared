package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentTooltipState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow private EditBox searchBox;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void vsq$cycleTooltipSelectionWithKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (vsq$isLeftAltHeld()) {
            if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_UP) {
                if (VSQEnchantmentTooltipState.cycleHovered(-1)) {
                    cir.setReturnValue(true);
                    return;
                }
            } else if (event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_DOWN) {
                if (VSQEnchantmentTooltipState.cycleHovered(1)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        if (this.searchBox != null
                && this.searchBox.isFocused()
                && this.searchBox.isVisible()
                && (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void vsq$cycleTooltipSelectionWithScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!vsq$isLeftAltHeld() || verticalAmount == 0.0D) {
            return;
        }

        if (VSQEnchantmentTooltipState.cycleHovered(verticalAmount > 0.0D ? -1 : 1)) {
            cir.setReturnValue(true);
        }
    }

    private static boolean vsq$isLeftAltHeld() {
        return Minecraft.getInstance().screen != null
                && GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
    }
}
