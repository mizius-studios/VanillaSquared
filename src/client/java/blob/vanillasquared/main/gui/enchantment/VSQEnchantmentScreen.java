package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;

import java.util.List;

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
    private static final int TEXT_ENABLED = ARGB.opaque(0xEFE2C6);
    private static final int TEXT_DISABLED = ARGB.opaque(0x8A7F6A);
    private static final int TEXT_HOVER = ARGB.opaque(0xFFFFFF);
    private int vsq$playerLevel = -1;
    private int vsq$levelRequirement = -1;
    private int vsq$blockAmount = -1;
    private int vsq$blockRequirement = -1;
    private boolean vsq$hasRequiredXp;
    private boolean vsq$hasRequiredBlocks;
    private boolean vsq$xpHovered;
    private boolean vsq$blocksHovered;

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
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;


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

        Identifier xpSprite;
        if (this.vsq$levelRequirement == -1 || this.vsq$playerLevel == -1 || !this.vsq$hasRequiredXp) {
            xpSprite = XP_DISABLED_SPRITE;
        } else if (this.vsq$xpHovered) {
            xpSprite = XP_HOVER_SPRITE;
        } else {
            xpSprite = XP_ENABLED_SPRITE;
        }

        Identifier blocksSprite;
        if (this.vsq$blockRequirement == -1 || this.vsq$blockAmount == -1 || !this.vsq$hasRequiredBlocks) {
            blocksSprite = BLOCKS_DISABLED_SPRITE;
        } else if (this.vsq$blocksHovered) {
            blocksSprite = BLOCKS_HOVER_SPRITE;
        } else {
            blocksSprite = BLOCKS_ENABLED_SPRITE;
        }
        int button0x = x + 120;
        int button0y = y + 36;
        int button1x = x + 120;
        int button1y = y + 54;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, xpSprite, button0x - 1, button0y - 1, 51, 18);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, blocksSprite, button1x - 1, button1y - 1, 51, 18);
        if (this.vsq$playerLevel != -1 && this.vsq$hasRequiredXp) {
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", this.vsq$levelRequirement), button0x + 15, button0y + 5, this.vsq$xpHovered ? TEXT_HOVER : TEXT_ENABLED, false);
        } else if (this.vsq$levelRequirement == -1) {
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp.none"), button0x + 15, button0y + 5, TEXT_DISABLED, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", this.vsq$levelRequirement), button0x + 15, button0y + 5, TEXT_DISABLED, false);
        }

        if (this.vsq$blockAmount != -1 && this.vsq$hasRequiredBlocks) {
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", this.vsq$blockRequirement), button1x + 15, button1y + 5, this.vsq$blocksHovered ? TEXT_HOVER : TEXT_ENABLED, false);
        } else if (this.vsq$blockRequirement == -1) {
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks.none"), button1x + 15, button1y + 5, TEXT_DISABLED, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", this.vsq$blockRequirement), button1x + 15, button1y + 5, TEXT_DISABLED, false);
        }
    }
    private boolean vsq$isXpHovered(int mouseX, int mouseY) {
        return this.isHovering(120, 36, 51, 18, mouseX, mouseY);
    }

    private boolean vsq$isBlocksHovered(int mouseX, int mouseY) {
        return this.isHovering(120, 54, 51, 18, mouseX, mouseY);
    }

    private void vsq$syncFromMenu() {
        VSQEnchantmentMenuProperties properties = this.menu instanceof VSQEnchantmentMenuProperties vsqProperties ? vsqProperties : null;
        this.vsq$playerLevel = properties != null ? properties.vsq$getPlayerLevel() : -1;
        this.vsq$levelRequirement = properties != null ? properties.vsq$getLevelRequirement() : -1;
        this.vsq$blockAmount = properties != null ? properties.vsq$getBlockAmount() : -1;
        this.vsq$blockRequirement = properties != null ? properties.vsq$getBlockRequirement() : -1;
        this.vsq$hasRequiredXp = properties != null && properties.vsq$hasRequiredXp();
        this.vsq$hasRequiredBlocks = properties != null && properties.vsq$hasRequiredBlocks();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.vsq$syncFromMenu();
        VSQEnchantmentMenuProperties properties = this.menu instanceof VSQEnchantmentMenuProperties vsqProperties ? vsqProperties : null;
        this.vsq$xpHovered = this.vsq$isXpHovered(mouseX, mouseY);
        this.vsq$blocksHovered = this.vsq$isBlocksHovered(mouseX, mouseY);
        List<Component> buttonTooltip = null;

        if (this.vsq$xpHovered && this.vsq$levelRequirement != -1) {
            Component xpTooltip = Component.translatable("vsq.gui.container.enchantment_table.xp.tooltip", this.vsq$playerLevel, this.vsq$levelRequirement).withStyle(ChatFormatting.GRAY);
            if (!this.vsq$hasRequiredXp) {
                xpTooltip = xpTooltip.copy().withStyle(ChatFormatting.RED);
            }
            buttonTooltip = List.of(xpTooltip);
        } else if (this.vsq$blocksHovered && this.vsq$blockRequirement != -1) {
            List<Component> blocksTooltip = properties != null ? properties.vsq$getDetectedBlockTooltipLines() : List.of();
            if (blocksTooltip.isEmpty()) {
                blocksTooltip = List.of(Component.translatable("vsq.gui.container.enchantment_table.blocks.tooltip.none"));
            }
            if (!this.vsq$hasRequiredBlocks) {
                blocksTooltip = blocksTooltip.stream()
                    .map(line -> (Component) line.copy().withStyle(ChatFormatting.RED))
                    .toList();
            } else {
                blocksTooltip = blocksTooltip.stream()
                    .map(line -> (Component) line.copy().withStyle(ChatFormatting.GRAY))
                    .toList();
            }
            buttonTooltip = blocksTooltip;
        }

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (buttonTooltip != null) {
            List<ClientTooltipComponent> tooltipLines = buttonTooltip.stream()
                .flatMap(line -> this.font.split(line, 180).stream())
                .map(ClientTooltipComponent::create)
                .toList();
            guiGraphics.renderTooltip(this.font, tooltipLines, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

    }
}
