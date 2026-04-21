package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.hud.SpecialEnchantmentCooldownClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContextualBarRenderer.class)
public interface ContextualBarRendererMixin {
    @Inject(method = "extractExperienceLevel", at = @At("HEAD"), cancellable = true)
    private static void vsq$hideExperienceLevelForSpecialCooldown(GuiGraphicsExtractor guiGraphics, Font font, int level, CallbackInfo ci) {
        if (SpecialEnchantmentCooldownClientState.hasVisibleCooldown(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }
}
