package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentTooltipState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @Shadow private EditBox name;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void vsq$cycleTooltipSelectionWithKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        boolean verticalArrow = event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN;

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

        if (verticalArrow && this.name != null && this.name.isFocused() && this.name.isVisible()) {
            cir.setReturnValue(false);
        }
    }

    private static boolean vsq$isLeftAltHeld() {
        return Minecraft.getInstance().screen != null
                && GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
    }
}
