package blob.vanillasquared.main.gui.enchantment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;

public class VSQEnchantmentScreen extends AbstractContainerScreen<EnchantmentMenu> {
    private static final Identifier GUI_TEXTURE =
        Identifier.fromNamespaceAndPath("vsq", "textures/gui/containers/enchantment_table.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public VSQEnchantmentScreen(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x0 = this.leftPos;
        int y0 = this.topPos;

        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            GUI_TEXTURE,
            x0,
            y0,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            TEX_W,
            TEX_H
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
