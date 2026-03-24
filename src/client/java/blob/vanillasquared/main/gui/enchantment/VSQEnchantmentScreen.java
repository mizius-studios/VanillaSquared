package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.main.network.payload.EnchantingBookClickPayload;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProperties;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;

import java.util.List;

public class VSQEnchantmentScreen extends AbstractContainerScreen<EnchantmentMenu> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath("vsq", "textures/gui/containers/enchantment_table.png");
    private static final Identifier XP_ENABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/xp_requirement_enabled");
    private static final Identifier XP_DISABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/xp_requirement_disabled");
    private static final Identifier XP_HOVER_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/xp_requirement_hover");
    private static final Identifier BLOCKS_ENABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_enabled");
    private static final Identifier BLOCKS_DISABLED_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_disabled");
    private static final Identifier BLOCKS_HOVER_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/enchantment_table/block_requirement_hover");
    private static final Identifier LAPIS_LAZULI_SLOT_EMPTY_SPRITE = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
    private static final Identifier SWORD_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/sword");
    private static final Identifier AXE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/axe");
    private static final Identifier PICKAXE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/pickaxe");
    private static final Identifier SHOVEL_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/shovel");
    private static final Identifier HOE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/hoe");
    private static final Identifier SPEAR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/spear");
    private static final Identifier SHIELD_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/shield");
    private static final Identifier HELMET_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/helmet");
    private static final Identifier CHESTPLATE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/chestplate");
    private static final Identifier LEGGINGS_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/leggings");
    private static final Identifier BOOTS_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/boots");
    private static final Identifier FLINT_AND_STEEL_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/flint_and_steel");
    private static final Identifier SHEARS_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/shears");
    private static final Identifier BOW_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/bow");
    private static final Identifier CROSSBOW_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/crossbow");
    private static final Identifier TRIDENT_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/trident");
    private static final Identifier ELYTRA_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/elytra");
    private static final Identifier FISHING_ROD_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/fishing_rod");
    private static final Identifier MACE_SLOT_SPRITE = Identifier.fromNamespaceAndPath("vsq", "containers/slots/mace");
    private static final Identifier ENCHANTING_BOOK_LOCATION = Identifier.withDefaultNamespace("textures/entity/enchantment/enchanting_table_book.png");
    private static final List<Identifier> LAPIS_LAZULI_SLOT_EMPTY_SPRITES = List.of(LAPIS_LAZULI_SLOT_EMPTY_SPRITE);
    private static final List<Identifier> INPUT_SLOT_EMPTY_SPRITES = List.of(SWORD_SLOT_SPRITE, AXE_SLOT_SPRITE, PICKAXE_SLOT_SPRITE, SHOVEL_SLOT_SPRITE, HOE_SLOT_SPRITE, SPEAR_SLOT_SPRITE, SHIELD_SLOT_SPRITE, HELMET_SLOT_SPRITE, CHESTPLATE_SLOT_SPRITE, LEGGINGS_SLOT_SPRITE, BOOTS_SLOT_SPRITE, FLINT_AND_STEEL_SLOT_SPRITE, SHEARS_SLOT_SPRITE, BOW_SLOT_SPRITE, CROSSBOW_SLOT_SPRITE, TRIDENT_SLOT_SPRITE, ELYTRA_SLOT_SPRITE, FISHING_ROD_SLOT_SPRITE, MACE_SLOT_SPRITE);
    private BookModel bookModel;
    private final RandomSource random = RandomSource.create();
    private ItemStack last = ItemStack.EMPTY;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
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
    private final CyclingSlotBackground vsq$inputSlot = new CyclingSlotBackground(0);
    private final CyclingSlotBackground vsq$lapislazuli = new CyclingSlotBackground(1);

    public VSQEnchantmentScreen(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
        this.titleLabelX = 10;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    private void renderBook(GuiGraphicsExtractor guiGraphics, int x, int y) {
        float f = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float g = Mth.lerp(f, this.oOpen, this.open);
        float h = Mth.lerp(f, this.oFlip, this.flip);
        int bookX = x + 127;
        int bookY = y + 4;
        int bookWidth = bookX + 33;
        int bookHeight = bookY + 28;
        guiGraphics.book(this.bookModel, ENCHANTING_BOOK_LOCATION, 38.0F, g, h, bookX, bookY, bookWidth, bookHeight);
    }

    private boolean vsq$isBookHovered(double mouseX, double mouseY) {
        return this.isHovering(127, 4, 33, 28, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);

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

        this.renderBook(guiGraphics, x, y);

        this.vsq$inputSlot.extractRenderState(this.menu, guiGraphics, partialTick, x, y);
        this.vsq$lapislazuli.extractRenderState(this.menu, guiGraphics, partialTick, x, y);

        Identifier xpSprite;
        if (this.vsq$levelRequirement == -1 || this.vsq$playerLevel == -1 || !this.vsq$hasRequiredXp) {
            xpSprite = XP_DISABLED_SPRITE;
        } else if (this.vsq$xpHovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            xpSprite = XP_HOVER_SPRITE;
        } else {
            xpSprite = XP_ENABLED_SPRITE;
        }

        Identifier blocksSprite;
        if (this.vsq$blockRequirement == -1 || this.vsq$blockAmount == -1 || !this.vsq$hasRequiredBlocks) {
            blocksSprite = BLOCKS_DISABLED_SPRITE;
        } else if (this.vsq$blocksHovered) {
            blocksSprite = BLOCKS_HOVER_SPRITE;
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
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
            guiGraphics.text(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", this.vsq$levelRequirement), button0x + 15, button0y + 5, this.vsq$xpHovered ? TEXT_HOVER : TEXT_ENABLED, false);
        } else if (this.vsq$levelRequirement == -1) {
            guiGraphics.text(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp.none"), button0x + 15, button0y + 5, TEXT_DISABLED, false);
        } else {
            guiGraphics.text(this.font, Component.translatable("vsq.gui.container.enchantment_table.xp", this.vsq$levelRequirement), button0x + 15, button0y + 5, TEXT_DISABLED, false);
        }

        if (this.vsq$blockAmount != -1 && this.vsq$hasRequiredBlocks) {
            guiGraphics.text(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", this.vsq$blockRequirement), button1x + 15, button1y + 5, this.vsq$blocksHovered ? TEXT_HOVER : TEXT_ENABLED, false);
        } else if (this.vsq$blockRequirement == -1) {
            guiGraphics.text(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks.none"), button1x + 15, button1y + 5, TEXT_DISABLED, false);
        } else {
            guiGraphics.text(this.font, Component.translatable("vsq.gui.container.enchantment_table.blocks", this.vsq$blockRequirement), button1x + 15, button1y + 5, TEXT_DISABLED, false);
        }
    }
    private boolean vsq$isXpHovered(int mouseX, int mouseY) {
        return this.isHovering(120, 36, 51, 18, mouseX, mouseY);
    }

    private boolean vsq$isBlocksHovered(int mouseX, int mouseY) {
        return this.isHovering(120, 54, 51, 18, mouseX, mouseY);
    }
    protected void containerTick() {
        super.containerTick();
        this.vsq$syncFromMenu();
        this.tickBook();
        this.vsq$inputSlot.tick(INPUT_SLOT_EMPTY_SPRITES);
        this.vsq$lapislazuli.tick(LAPIS_LAZULI_SLOT_EMPTY_SPRITES);
    }

    private void tickBook() {
        ItemStack current = this.menu.getSlot(0).getItem();
        if (!ItemStack.matches(current, this.last)) {
            this.last = current;
            this.flipT += (float) (this.random.nextInt(4) - this.random.nextInt(4));
            while (this.flip <= this.flipT + 1.0F && this.flip >= this.flipT - 1.0F) {
                this.flipT += (float) (this.random.nextInt(4) - this.random.nextInt(4));
            }
        }

        this.oFlip = this.flip;
        this.oOpen = this.open;

        boolean hasOffer = false;
        for (int i = 0; i < 3; i++) {
            if (this.menu.costs[i] != 0) {
                hasOffer = true;
                break;
            }
        }

        if (hasOffer) {
            this.open += 0.2F;
        } else {
            this.open -= 0.2F;
        }

        this.open = Mth.clamp(this.open, 0.0F, 1.0F);
        float f = (this.flipT - this.flip) * 0.4F;
        f = Mth.clamp(f, -0.2F, 0.2F);
        this.flipA += (f - this.flipA) * 0.9F;
        this.flip += this.flipA;
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.vsq$syncFromMenu();
        this.vsq$xpHovered = this.vsq$isXpHovered(mouseX, mouseY);
        this.vsq$blocksHovered = this.vsq$isBlocksHovered(mouseX, mouseY);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        VSQEnchantmentMenuProperties properties = this.menu instanceof VSQEnchantmentMenuProperties vsqProperties ? vsqProperties : null;
        List<Component> buttonTooltip = null;

        if (this.vsq$xpHovered && this.vsq$levelRequirement != -1) {
            Component xpTooltip = Component.translatable("vsq.gui.container.enchantment_table.xp.tooltip", this.vsq$playerLevel, this.vsq$levelRequirement).withStyle(ChatFormatting.GRAY);
            if (!this.vsq$hasRequiredXp) {
                guiGraphics.requestCursor(CursorTypes.NOT_ALLOWED);
                xpTooltip = xpTooltip.copy().withStyle(ChatFormatting.RED);
            }
            buttonTooltip = List.of(xpTooltip);
        } else if (this.vsq$blocksHovered && this.vsq$blockRequirement != -1) {
            List<Component> blocksTooltip = properties != null ? properties.vsq$getDetectedBlockTooltipLines() : List.of();
            if (blocksTooltip.isEmpty()) {
                blocksTooltip = List.of(Component.translatable("vsq.gui.container.enchantment_table.blocks.tooltip.none"));
            }
            if (!this.vsq$hasRequiredBlocks) {
                guiGraphics.requestCursor(CursorTypes.NOT_ALLOWED);
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
        if (buttonTooltip != null) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, buttonTooltip, mouseX, mouseY);
        }

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.vsq$isBookHovered(event.x(), event.y())) {
            ClientPlayNetworking.send(new EnchantingBookClickPayload(this.menu.containerId));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }
}
