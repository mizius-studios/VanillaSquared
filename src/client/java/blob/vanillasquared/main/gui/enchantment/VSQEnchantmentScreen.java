package blob.vanillasquared.main.gui.enchantment;

import blob.vanillasquared.main.network.handlers.EnchantingRecipeBookSyncPayloadHandler;
import blob.vanillasquared.main.network.handlers.EnchantingRecipeStatePayloadHandler;
import blob.vanillasquared.main.network.payload.EnchantingBookClickPayload;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProperties;
import blob.vanillasquared.util.api.VSQUtil;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class VSQEnchantmentScreen extends AbstractRecipeBookScreen<VSQEnchantmentMenu> {
    private static final int BUTTON_X = 120;
    private static final int XP_BUTTON_Y = 36;
    private static final int BLOCKS_BUTTON_Y = 54;
    private static final int BUTTON_WIDTH = 51;
    private static final int BUTTON_HEIGHT = 18;
    private static final int REQUIREMENT_HOVER_RIGHT_INSET = 2;
    private static final int REQUIREMENT_HOVER_BOTTOM_INSET = 2;
    private static final int RECIPE_BOOK_BUTTON_X = 24;
    private static final int RECIPE_BOOK_BUTTON_Y = 50;
    private static final int BOOK_X = 127;
    private static final int BOOK_Y = 4;
    private static final int BOOK_WIDTH = 33;
    private static final int BOOK_HEIGHT = 28;
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
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int TEXT_ENABLED = ARGB.opaque(0xEFE2C6);
    private static final int TEXT_DISABLED = ARGB.opaque(0x8A7F6A);
    private static final int TEXT_HOVER = ARGB.opaque(0xFFFFFF);

    private final VSQEnchantmentRecipeBookComponent vsq$recipeBookComponent;
    private BookModel bookModel;
    private final RandomSource random = RandomSource.create();
    private ItemStack last = ItemStack.EMPTY;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    private int vsq$playerLevel = -1;
    private int vsq$levelRequirement = -1;
    private int vsq$blockAmount = -1;
    private int vsq$blockRequirement = -1;
    private boolean vsq$hasRequiredXp;
    private boolean vsq$hasRequiredBlocks;
    private List<Component> vsq$bookTooltipLines = List.of();
    private final CyclingSlotBackground vsq$inputSlot = new CyclingSlotBackground(0);
    private final CyclingSlotBackground vsq$lapislazuli = new CyclingSlotBackground(1);
    private final VSQUtil.VSQ$Component vsq$util = new VSQUtil.VSQ$Component();

    public VSQEnchantmentScreen(VSQEnchantmentMenu menu, Inventory inventory, Component title) {
        this(menu, new VSQEnchantmentRecipeBookComponent(menu), inventory, title);
    }

    private VSQEnchantmentScreen(VSQEnchantmentMenu menu, VSQEnchantmentRecipeBookComponent recipeBookComponent, Inventory inventory, Component title) {
        super(menu, recipeBookComponent, inventory, title);
        this.vsq$recipeBookComponent = recipeBookComponent;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
        EnchantingRecipeStatePayloadHandler.applyCached(this.menu.containerId, this.menu);
        this.titleLabelX = 10;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected @NonNull ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + RECIPE_BOOK_BUTTON_X, this.topPos + RECIPE_BOOK_BUTTON_Y);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.vsq$syncFromMenu();
        if (this.menu.vsq$consumeSelectionCleared()) {
            this.vsq$recipeBookComponent.vsq$clearSelection();
        }
        this.tickBook();
        this.vsq$inputSlot.tick(INPUT_SLOT_EMPTY_SPRITES);
        this.vsq$lapislazuli.tick(LAPIS_LAZULI_SLOT_EMPTY_SPRITES);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        this.renderBook(guiGraphics, this.leftPos, this.topPos);
        this.vsq$inputSlot.extractRenderState(this.menu, guiGraphics, partialTick, this.leftPos, this.topPos);
        this.vsq$lapislazuli.extractRenderState(this.menu, guiGraphics, partialTick, this.leftPos, this.topPos);

        boolean xpHovered = this.vsq$isXpHovered(mouseX, mouseY);
        boolean blocksHovered = this.vsq$isBlocksHovered(mouseX, mouseY);
        Identifier xpSprite = this.vsq$getRequirementSprite(
                this.vsq$levelRequirement != -1 && this.vsq$playerLevel != -1,
                this.vsq$hasRequiredXp,
                xpHovered,
                XP_DISABLED_SPRITE,
                XP_ENABLED_SPRITE,
                XP_HOVER_SPRITE,
                guiGraphics
        );
        Identifier blocksSprite = this.vsq$getRequirementSprite(
                this.vsq$blockRequirement != -1 && this.vsq$blockAmount != -1,
                this.vsq$hasRequiredBlocks,
                blocksHovered,
                BLOCKS_DISABLED_SPRITE,
                BLOCKS_ENABLED_SPRITE,
                BLOCKS_HOVER_SPRITE,
                guiGraphics
        );
        int buttonX = this.leftPos + BUTTON_X;
        int xpButtonY = this.topPos + XP_BUTTON_Y;
        int blocksButtonY = this.topPos + BLOCKS_BUTTON_Y;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, xpSprite, buttonX - 1, xpButtonY - 1, BUTTON_WIDTH, BUTTON_HEIGHT);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, blocksSprite, buttonX - 1, blocksButtonY - 1, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.vsq$renderRequirementText(guiGraphics, buttonX, xpButtonY, xpHovered, this.vsq$playerLevel != -1 && this.vsq$hasRequiredXp, this.vsq$levelRequirement == -1, Component.translatable("vsq.gui.container.enchantment_table.xp", this.vsq$levelRequirement), Component.translatable("vsq.gui.container.enchantment_table.xp.none"));
        this.vsq$renderRequirementText(guiGraphics, buttonX, blocksButtonY, blocksHovered, this.vsq$blockAmount != -1 && this.vsq$hasRequiredBlocks, this.vsq$blockRequirement == -1, Component.translatable("vsq.gui.container.enchantment_table.blocks", this.vsq$blockRequirement), Component.translatable("vsq.gui.container.enchantment_table.blocks.none"));
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.vsq$syncFromMenu();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        List<Component> buttonTooltip = null;
        boolean bookHovered = this.vsq$isBookHovered(mouseX, mouseY);
        if (this.vsq$isXpHovered(mouseX, mouseY) && this.vsq$levelRequirement != -1) {
            Component xpTooltip = Component.translatable("vsq.gui.container.enchantment_table.xp.tooltip", this.vsq$playerLevel, this.vsq$levelRequirement).withStyle(ChatFormatting.GRAY);
            if (!this.vsq$hasRequiredXp) {
                guiGraphics.requestCursor(CursorTypes.NOT_ALLOWED);
                xpTooltip = xpTooltip.copy().withStyle(ChatFormatting.RED);
            }
            buttonTooltip = List.of(xpTooltip);
        } else if (bookHovered && this.vsq$hasDisplayableRecipe() && !this.vsq$bookTooltipLines.isEmpty()) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            buttonTooltip = vsq$util.expandTooltipLines(this.vsq$bookTooltipLines);
        } else if (bookHovered && !this.vsq$hasDisplayableRecipe()) {
            guiGraphics.requestCursor(CursorTypes.NOT_ALLOWED);
        } else if (this.vsq$isBlocksHovered(mouseX, mouseY) && this.vsq$blockRequirement != -1) {
            List<Component> blocksTooltip = this.menu.vsq$getDetectedBlockTooltipLines();
            if (blocksTooltip.isEmpty()) {
                blocksTooltip = List.of(Component.translatable("vsq.gui.container.enchantment_table.blocks.tooltip.none"));
            }
            if (!this.vsq$hasRequiredBlocks) {
                guiGraphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
            buttonTooltip = this.vsq$util.styleTooltipLines(blocksTooltip, this.vsq$hasRequiredBlocks ? ChatFormatting.GRAY : ChatFormatting.RED);
        }
        if (buttonTooltip != null) {
            guiGraphics.setComponentTooltipForNextFrame(this.font, buttonTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.vsq$isBookHovered(event.x(), event.y()) && this.vsq$hasDisplayableRecipe()) {
            ClientPlayNetworking.send(new EnchantingBookClickPayload(this.menu.containerId));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, @NonNull ContainerInput containerInput) {
        super.slotClicked(slot, slotId, mouseButton, containerInput);
        if (this.menu.vsq$getEnchantingSlots().contains(slot)) {
            this.vsq$recipeBookComponent.vsq$clearSelection();
        }
    }

    @Override
    protected void onRecipeBookButtonClick() {
        super.onRecipeBookButtonClick();
    }

    @Override
    public void removed() {
        this.vsq$recipeBookComponent.vsq$clearSelection();
        EnchantingRecipeBookSyncPayloadHandler.clearContainer(this.minecraft, this.menu.containerId);
        EnchantingRecipeStatePayloadHandler.clearContainer(this.menu.containerId);
        super.removed();
    }

    private void renderBook(GuiGraphicsExtractor guiGraphics, int x, int y) {
        float partial = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float open = Mth.lerp(partial, this.oOpen, this.open);
        float flip = Mth.lerp(partial, this.oFlip, this.flip);
        int bookX = x + BOOK_X;
        int bookY = y + BOOK_Y;
        guiGraphics.book(this.bookModel, ENCHANTING_BOOK_LOCATION, 38.0F, open, flip, bookX, bookY, bookX + BOOK_WIDTH, bookY + BOOK_HEIGHT);
    }

    private boolean vsq$isXpHovered(int mouseX, int mouseY) {
        return this.isHovering(BUTTON_X, XP_BUTTON_Y, BUTTON_WIDTH - REQUIREMENT_HOVER_RIGHT_INSET, BUTTON_HEIGHT - REQUIREMENT_HOVER_BOTTOM_INSET, mouseX, mouseY);
    }

    private boolean vsq$isBlocksHovered(int mouseX, int mouseY) {
        return this.isHovering(BUTTON_X, BLOCKS_BUTTON_Y, BUTTON_WIDTH - REQUIREMENT_HOVER_RIGHT_INSET, BUTTON_HEIGHT - REQUIREMENT_HOVER_BOTTOM_INSET, mouseX, mouseY);
    }

    private boolean vsq$isBookHovered(double mouseX, double mouseY) {
        return this.isHovering(BOOK_X, BOOK_Y, BOOK_WIDTH, BOOK_HEIGHT, mouseX, mouseY);
    }

    private boolean vsq$hasDisplayableRecipe() {
        return this.vsq$levelRequirement != -1 && this.vsq$blockRequirement != -1;
    }

    private void vsq$updateBookOpenState(boolean shouldOpen) {
        if (shouldOpen) {
            this.open += 0.2F;
        } else {
            this.open -= 0.2F;
        }
    }

    private void tickBook() {
        ItemStack current = this.menu.getSlot(VSQEnchantmentMenu.INPUT_SLOT).getItem();
        if (!ItemStack.matches(current, this.last)) {
            this.last = current;
            for (int retries = 0; retries < 10; retries++) {
                this.flipT += (float) (this.random.nextInt(4) - this.random.nextInt(4));
                if (this.flip < this.flipT - 1.0F || this.flip > this.flipT + 1.0F) {
                    break;
                }
            }
        }

        this.oFlip = this.flip;
        this.oOpen = this.open;
        this.vsq$updateBookOpenState(this.vsq$hasDisplayableRecipe());
        this.open = Mth.clamp(this.open, 0.0F, 1.0F);
        float delta = (this.flipT - this.flip) * 0.4F;
        delta = Mth.clamp(delta, -0.2F, 0.2F);
        this.flipA += (delta - this.flipA) * 0.9F;
        this.flip += this.flipA;
    }

    private void vsq$syncFromMenu() {
        VSQEnchantmentMenuProperties properties = this.menu;
        this.vsq$playerLevel = properties.vsq$getPlayerLevel();
        this.vsq$levelRequirement = properties.vsq$getLevelRequirement();
        this.vsq$blockAmount = properties.vsq$getBlockAmount();
        this.vsq$blockRequirement = properties.vsq$getBlockRequirement();
        this.vsq$hasRequiredXp = properties.vsq$hasRequiredXp();
        this.vsq$hasRequiredBlocks = properties.vsq$hasRequiredBlocks();
        this.vsq$bookTooltipLines = properties.vsq$getBookTooltipLines();
    }

    private Identifier vsq$getRequirementSprite(boolean hasData, boolean meetsRequirement, boolean hovered, Identifier disabledSprite, Identifier enabledSprite, Identifier hoverSprite, GuiGraphicsExtractor guiGraphics) {
        if (!hasData || !meetsRequirement) {
            if (hovered) {
                guiGraphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
            return disabledSprite;
        }
        if (hovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            return hoverSprite;
        }
        return enabledSprite;
    }

    private void vsq$renderRequirementText(GuiGraphicsExtractor guiGraphics, int buttonX, int buttonY, boolean hovered, boolean enabled, boolean showNoneText, Component valueText, Component noneText) {
        Component text = showNoneText ? noneText : valueText;
        int color = enabled ? (hovered ? TEXT_HOVER : TEXT_ENABLED) : TEXT_DISABLED;
        guiGraphics.text(this.font, text, buttonX + 15, buttonY + 5, color, false);
    }
}
