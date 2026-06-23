package blob.vanillasquared.mixin.client.gui;

import blob.vanillasquared.main.VanillaSquaredClient;
import blob.vanillasquared.main.gui.enchantment.EnchantingOverlayRecipeButton;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent$OverlayRecipeButton")
public abstract class OverlayRecipeButtonMixin extends AbstractWidget implements EnchantingOverlayRecipeButton {
    @Unique
    private static final RenderPipeline VSQ$GUI_PIPELINE = RenderPipelines.GUI_TEXTURED;
    @Unique
    private static final Identifier VSQ$ENABLED = vsq$sprite("enchanting_overlay");
    @Unique
    private static final Identifier VSQ$ENABLED_HIGHLIGHTED = vsq$sprite("enchanting_overlay_highlighted");
    @Unique
    private static final Identifier VSQ$DISABLED = vsq$sprite("enchanting_overlay_disabled");
    @Unique
    private static final Identifier VSQ$DISABLED_HIGHLIGHTED = vsq$sprite("enchanting_overlay_disabled_highlighted");

    @Shadow
    @Final
    private boolean isCraftable;
    @Shadow
    @Final
    private OverlayRecipeComponent this$0;

    @Unique
    private List<List<ItemStack>> vsq$enchantingIngredients;

    protected OverlayRecipeButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public void vsq$setEnchantingDisplay(RecipeDisplay display, ContextMap contextMap) {
        if (!(display instanceof ShapelessCraftingRecipeDisplay shapeless)
                || !(shapeless.craftingStation() instanceof SlotDisplay.ItemSlotDisplay station)
                || station.item().value() != Items.ENCHANTING_TABLE) {
            this.vsq$enchantingIngredients = null;
            return;
        }

        List<SlotDisplay> ingredients = shapeless.ingredients();
        List<List<ItemStack>> resolved = new ArrayList<>(5);
        for (int index : new int[]{1, 2, 3, 4, 5}) {
            resolved.add(index < ingredients.size()
                    ? ingredients.get(index).resolveForStacks(contextMap)
                    : List.of());
        }
        this.vsq$enchantingIngredients = List.copyOf(resolved);
    }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void vsq$renderEnchantingOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                             float partialTick, CallbackInfo ci) {
        if (this.vsq$enchantingIngredients == null) {
            return;
        }

        graphics.blitSprite(VSQ$GUI_PIPELINE, this.vsq$getSprite(), this.getX(), this.getY(), this.width, this.height);
        int selection = ((OverlayRecipeComponentAccessor) this.this$0).vsq$getSlotSelectTime().currentIndex();

        vsq$renderIngredient(graphics, this.vsq$enchantingIngredients.get(0), selection, 10, 10);
        vsq$renderIngredient(graphics, this.vsq$enchantingIngredients.get(1), selection, 10, 3);
        vsq$renderIngredient(graphics, this.vsq$enchantingIngredients.get(2), selection, 3, 10);
        vsq$renderIngredient(graphics, this.vsq$enchantingIngredients.get(3), selection, 17, 10);
        vsq$renderIngredient(graphics, this.vsq$enchantingIngredients.get(4), selection, 10, 17);
        ci.cancel();
    }

    @Unique
    private Identifier vsq$getSprite() {
        if (this.isCraftable) {
            return this.isHoveredOrFocused() ? VSQ$ENABLED_HIGHLIGHTED : VSQ$ENABLED;
        }
        return this.isHoveredOrFocused() ? VSQ$DISABLED_HIGHLIGHTED : VSQ$DISABLED;
    }

    @Unique
    private void vsq$renderIngredient(GuiGraphicsExtractor graphics, List<ItemStack> alternatives,
                                      int selection, int x, int y) {
        if (alternatives.isEmpty()) {
            return;
        }

        ItemStack stack = alternatives.get(Math.floorMod(selection, alternatives.size())).copy();
        stack.setCount(1);
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.getX() + 2.0F + x, this.getY() + 2.0F + y);
        graphics.pose().scale(0.375F, 0.375F);
        graphics.pose().translate(-8.0F, -8.0F);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

    @Unique
    private static Identifier vsq$sprite(String name) {
        return Identifier.fromNamespaceAndPath(VanillaSquaredClient.MOD_ID, "recipe_book/" + name);
    }
}
