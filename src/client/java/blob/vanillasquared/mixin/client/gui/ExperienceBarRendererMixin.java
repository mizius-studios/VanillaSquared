package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.hud.SpecialEnchantmentCooldownClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceBarRenderer.class)
public class ExperienceBarRendererMixin {
    private static final Identifier SPECIAL_COOLDOWN_BACKGROUND = Identifier.fromNamespaceAndPath("vsq", "hud/special_enchantment_cooldown_background");
    private static final Identifier SPECIAL_COOLDOWN_PROGRESS = Identifier.fromNamespaceAndPath("vsq", "hud/special_enchantment_cooldown");

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void vsq$extractSpecialEnchantmentCooldown(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SpecialEnchantmentCooldownClientState.visibleCooldown(this.minecraft.player).ifPresent(cooldown -> {
            int left = (guiGraphics.guiWidth() - 182) / 2;
            int top = guiGraphics.guiHeight() - 32 + 3;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPECIAL_COOLDOWN_BACKGROUND, left, top, 182, 5);
            int progressWidth = cooldown.progressWidth();
            if (progressWidth > 0) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPECIAL_COOLDOWN_PROGRESS, 182, 5, 0, 0, left, top, progressWidth, 5);
            }
            vsq$drawCooldownText(guiGraphics, cooldown);
            ci.cancel();
        });
    }

    private void vsq$drawCooldownText(GuiGraphicsExtractor guiGraphics, SpecialEnchantmentCooldownClientState.VisibleCooldown cooldown) {
        Font font = this.minecraft.font;
        Component text = Component.literal(cooldown.displayText());
        int x = (guiGraphics.guiWidth() - font.width(text)) / 2;
        int y = guiGraphics.guiHeight() - 24 - 9 - 2;
        guiGraphics.text(font, text, x + 1, y, 0xFF000000, false);
        guiGraphics.text(font, text, x - 1, y, 0xFF000000, false);
        guiGraphics.text(font, text, x, y + 1, 0xFF000000, false);
        guiGraphics.text(font, text, x, y - 1, 0xFF000000, false);
        guiGraphics.text(font, text, x, y, cooldown.textColor(), false);
    }
}
