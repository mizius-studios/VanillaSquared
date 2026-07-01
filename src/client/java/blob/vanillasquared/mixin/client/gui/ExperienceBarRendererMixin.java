package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.hud.SpecialEnchantmentCooldownClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ExperienceBar.class)
public class ExperienceBarRendererMixin {
    @Unique
    private static final Identifier SPECIAL_COOLDOWN_BACKGROUND = Identifier.fromNamespaceAndPath("vsq", "hud/special_enchantment_cooldown_background");
    @Unique
    private static final Identifier SPECIAL_COOLDOWN_PROGRESS = Identifier.fromNamespaceAndPath("vsq", "hud/special_enchantment_cooldown");

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void vsq$extractSpecialEnchantmentCooldown(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Optional<SpecialEnchantmentCooldownClientState.VisibleCooldown> visibleCooldown =
                SpecialEnchantmentCooldownClientState.visibleCooldown(this.minecraft.player);
        visibleCooldown.ifPresent(cooldown -> {
            int left = (graphics.guiWidth() - 182) / 2;
            int top = graphics.guiHeight() - 32 + 3;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPECIAL_COOLDOWN_BACKGROUND, left, top, 182, 5);
            int progressWidth = cooldown.progressWidth();
            if (progressWidth > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPECIAL_COOLDOWN_PROGRESS, 182, 5, 0, 0, left, top, progressWidth, 5);
            }
            vsq$drawCooldownText(graphics, cooldown);
            ci.cancel();
        });
        if (vsq$shouldSuppressVanillaExperienceBar(
                this.minecraft.player != null && this.minecraft.player.isCreative(),
                visibleCooldown.isPresent()
        )) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean vsq$shouldSuppressVanillaExperienceBar(boolean creativeMode, boolean hasVisibleCooldown) {
        return creativeMode && !hasVisibleCooldown;
    }

    @Unique
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
