package blob.vanillasquared.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow
    @Final
    private static Identifier ARMOR_EMPTY_SPRITE;

    @Shadow
    @Final
    private static Identifier ARMOR_HALF_SPRITE;

    @Shadow
    @Final
    private static Identifier ARMOR_FULL_SPRITE;

    /**
     * @author blob
     * @reason Render armor values above 20 in additional HUD rows.
     */
    @Overwrite
    private static void renderArmor(
        GuiGraphics guiGraphics,
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
}
