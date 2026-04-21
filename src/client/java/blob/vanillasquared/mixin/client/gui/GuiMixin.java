package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.gui.hud.SpecialEnchantmentCooldownClientState;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(value = Gui.class, priority = 500)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private static Identifier ARMOR_EMPTY_SPRITE;

    @Shadow
    @Final
    private static Identifier ARMOR_HALF_SPRITE;

    @Shadow
    @Final
    private static Identifier ARMOR_FULL_SPRITE;

    @Inject(method = "nextContextualInfoState", at = @At("RETURN"), cancellable = true)
    private void vsq$prioritizeSpecialEnchantmentCooldown(CallbackInfoReturnable<Object> cir) {
        Object current = cir.getReturnValue();
        if (!vsq$isContextualInfo(current, "EMPTY")) {
            return;
        }
        if (SpecialEnchantmentCooldownClientState.hasVisibleCooldown(this.minecraft.player)) {
            Object experience = vsq$contextualInfo(current, "EXPERIENCE");
            if (experience != null) {
                cir.setReturnValue(experience);
            }
        }
    }

    /**
     * @author blob
     * @reason Render armor values above 20 in additional HUD rows.
     */
    @Overwrite
    private static void extractArmor(
        GuiGraphicsExtractor guiGraphics,
        Player player,
        int top,
        int lines,
        int lineHeight,
        int left
    ) {
        int armorValue = player.getArmorValue();
        if (armorValue <= 0) {
            return;
        }

        int rows = (armorValue + 19) / 20;
        int firstRowY = top - (lines - 1) * lineHeight - 10;

        // Start compressing armor rows only after the 4th row.
        int compactAfterRows = 4;
        // Vanilla-like dynamic stacking: spacing shrinks as row count grows, clamped to a readable minimum.
        int rowSpacing =
            rows > compactAfterRows
                ? Math.max(3, 10 - (rows - compactAfterRows))
                : 10;

        // Render from top row to bottom row so lower rows appear in front when rows overlap.
        for (int row = rows - 1; row >= 0; row--) {
            int y = firstRowY - row * rowSpacing;

            for (int col = 0; col < 10; col++) {
                int slot = row * 10 + col;
                int x = left + col * 8;
                int armorPoint = slot * 2 + 1;

                if (armorPoint < armorValue) {
                    guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        ARMOR_FULL_SPRITE,
                        x,
                        y,
                        9,
                        9
                    );
                } else if (armorPoint == armorValue) {
                    guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        ARMOR_HALF_SPRITE,
                        x,
                        y,
                        9,
                        9
                    );
                } else {
                    guiGraphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        ARMOR_EMPTY_SPRITE,
                        x,
                        y,
                        9,
                        9
                    );
                }
            }
        }
    }

    @Unique
    private static boolean vsq$isContextualInfo(Object value, String name) {
        return value instanceof Enum<?> contextualInfo && contextualInfo.name().equals(name);
    }

    @Unique
    private static Object vsq$contextualInfo(Object value, String name) {
        if (!(value instanceof Enum<?> contextualInfo)) {
            return null;
        }
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object result = Enum.valueOf((Class<? extends Enum>) contextualInfo.getDeclaringClass(), name);
            return result;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
