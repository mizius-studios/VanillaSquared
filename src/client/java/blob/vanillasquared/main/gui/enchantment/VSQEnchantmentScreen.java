package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.mixin.world.inventory.EnchantMenuMixinUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;

public class VSQEnchantmentScreen extends AbstractContainerScreen<EnchantmentMenu> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath("vsq", "textures/gui/containers/enchantment_table.png");
    private static final Identifier XP_ENABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/xp_requirement_enabled");
    private static final Identifier XP_DISABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/xp_requirement_disabled");
    private static final Identifier BLOCKS_ENABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_enabled");
    private static final Identifier BLOCKS_DISABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_disabled");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static int BUTTONID = -1;

    public VSQEnchantmentScreen(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    public static int getBUTTONID() {
        return BUTTONID;
    }

    public static void setBUTTONID(int BUTTONID) {
        VSQEnchantmentScreen.BUTTONID = BUTTONID;
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
        // int x0 = this.leftPos;
        // int y0 = this.topPos;
        int x = this.leftPos;
        int y = this.topPos;


        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            GUI_TEXTURE,
            x,
            y,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            TEX_W,
            TEX_H
        );
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_ENABLED_SPRITE, x + 119, y + 35, 51, 18);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLOCKS_ENABLED_SPRITE, x + 119, y + 53, 51, 18);
    }
    public boolean isMouseOver(double mouseX, double mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        double mouseXOffset = mouseX - x;
        double mouseYOffset = mouseY - y;
        if (mouseXOffset >= 119 && mouseXOffset <= 170 && mouseYOffset >= 35 && mouseYOffset <= 53) {
            BUTTONID = 0;
            return true;
        } else if (mouseXOffset >= 119 && mouseXOffset <= 170 && mouseYOffset >= 53 && mouseYOffset <= 71) {
            BUTTONID = 1;
            return true;
        }
        BUTTONID = -1;
        return false;
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        return this.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int Button0x = x + 119;
        int Button0y = y + 35;
        int Button1x = x + 119;
        int Button1y = y + 53;
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (EnchantMenuMixinUtil.getPlayerLevel() >= EnchantMenuMixinUtil.getLevelRequirement()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_ENABLED_SPRITE, Button0x, Button0y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", EnchantMenuMixinUtil.getLevelRequirement()), Button0x + 15, Button0y + 4, ARGB.opaque(0x70644F));
        } else {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_DISABLED_SPRITE, Button0x, Button0y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", EnchantMenuMixinUtil.getLevelRequirement()), Button0x + 15, Button0y + 4, ARGB.opaque(0x332E24));
        }
        if (EnchantMenuMixinUtil.getBlockAmount() >= EnchantMenuMixinUtil.getBlockRequirement()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLOCKS_ENABLED_SPRITE, Button1x, Button1y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", EnchantMenuMixinUtil.getBlockAmount()), Button1x + 15, Button1y + 4, ARGB.opaque(0x70644F));
        } else {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLOCKS_DISABLED_SPRITE, Button1x, Button1y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", EnchantMenuMixinUtil.getBlockAmount()), Button1x + 15, Button1y + 4, ARGB.opaque(0x332E24));
        }
    }
}
