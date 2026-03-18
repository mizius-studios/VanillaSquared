package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProperties;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
    private static final Identifier XP_HOVER_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/xp_requirement_hover");
    private static final Identifier BLOCKS_ENABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_enabled");
    private static final Identifier BLOCKS_DISABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_disabled");
    private static final Identifier BLOCKS_HOVER_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_hover");
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
    public int MouseOver(double mouseX, double mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        double mouseXOffset = mouseX - x;
        double mouseYOffset = mouseY - y;
        if (mouseXOffset >= 119 && mouseXOffset <= 170 && mouseYOffset >= 35 && mouseYOffset <= 53) {
            return 0;
        } else if (mouseXOffset >= 119 && mouseXOffset <= 170 && mouseYOffset >= 53 && mouseYOffset <= 71) {
            return 1;
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int Button0x = x + 119;
        int Button0y = y + 35;
        int Button1x = x + 119;
        int Button1y = y + 53;
        VSQEnchantmentMenuProperties properties = this.menu instanceof VSQEnchantmentMenuProperties vsqProperties ? vsqProperties : null;
        int playerLevel = properties != null ? properties.vsq$getPlayerLevel() : 0;
        int levelRequirement = properties != null ? properties.vsq$getLevelRequirement() : 0;
        int blockAmount = properties != null ? properties.vsq$getBlockAmount() : 0;
        int blockRequirement = properties != null ? properties.vsq$getBlockRequirement() : 0;

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (playerLevel >= levelRequirement && playerLevel != -1) {
            if (this.MouseOver(mouseX, mouseY) == 0) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_HOVER_SPRITE, Button0x, Button0y, 51, 18);
                guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", levelRequirement), Button0x + 15, Button0y + 5, ARGB.opaque(0xEAAFE0), false);
            } else {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_ENABLED_SPRITE, Button0x, Button0y, 51, 18);
                guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", levelRequirement), Button0x + 15, Button0y + 5, ARGB.opaque(0x70644F), false);
            }
        } else if (playerLevel != -1) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_DISABLED_SPRITE, Button0x, Button0y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp.none", levelRequirement), Button0x + 15, Button0y + 5, ARGB.opaque(0x332E24), false);
        } else {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_DISABLED_SPRITE, Button0x, Button0y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", levelRequirement), Button0x + 15, Button0y + 5, ARGB.opaque(0x332E24), false);
        }
        if (blockAmount >= blockRequirement && blockAmount != -1) {
            if (this.MouseOver(mouseX, mouseY) == 1) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLOCKS_HOVER_SPRITE, Button1x, Button1y, 51, 18);
                guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", blockAmount), Button1x + 15, Button1y + 5, ARGB.opaque(0xEAAFE0), false);
            } else {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLOCKS_ENABLED_SPRITE, Button1x, Button1y, 51, 18);
                guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", blockAmount), Button1x + 15, Button1y + 5, ARGB.opaque(0x70644F), false);
            }
        } else if (blockAmount != -1) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLOCKS_DISABLED_SPRITE, Button1x, Button1y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks.none", blockAmount), Button1x + 15, Button1y + 5, ARGB.opaque(0x332E24), false);
        } else {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLOCKS_DISABLED_SPRITE, Button1x, Button1y, 51, 18);
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", blockAmount), Button1x + 15, Button1y + 5, ARGB.opaque(0x332E24), false);
        }
    }
}
