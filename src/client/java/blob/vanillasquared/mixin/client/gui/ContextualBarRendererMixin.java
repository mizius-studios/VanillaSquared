package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.hud.SpecialEnchantmentCooldownClientState;
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
    private static void vsq$hideExperienceLevelForSpecialCooldown(GuiGraphicsExtractor graphics, Font font, int experienceLevel, CallbackInfo ci) {
        if (SpecialEnchantmentCooldownClientState.shouldReserveContextualBar()) {
            ci.cancel();
        }
    }
}
