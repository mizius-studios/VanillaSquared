package blob.vanillasquared.mixin.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(CyclingSlotBackground.class)
public abstract class CyclingSlotBackgroundMixin {
    @Shadow
    private int slotIndex;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void vsq$skipTickWhenGhostPresent(List<?> icons, CallbackInfo ci) {
        if (this.vsq$hasGhostSlot()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void vsq$skipRenderWhenGhostPresent(AbstractContainerMenu menu, GuiGraphicsExtractor guiGraphics, float partialTick, int x, int y, CallbackInfo ci) {
        if (this.vsq$hasGhostSlot()) {
            ci.cancel();
        }
    }

    private boolean vsq$hasGhostSlot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AbstractRecipeBookScreen<?> recipeBookScreen)) {
            return false;
        }

        RecipeBookComponent<?> recipeBookComponent = ((AbstractRecipeBookScreenAccessor) recipeBookScreen).vsq$getRecipeBookComponent();
        GhostSlots ghostSlots = ((RecipeBookComponentAccessor) recipeBookComponent).vsq$getGhostSlots();
        AbstractContainerMenu menu = ((AbstractContainerScreenAccessor) recipeBookScreen).vsq$getMenu();
        if (this.slotIndex < 0 || this.slotIndex >= menu.slots.size()) {
            return false;
        }

        Slot slot = menu.getSlot(this.slotIndex);
        return ((GhostSlotsAccessor) ghostSlots).vsq$getIngredients().containsKey(slot);
    }
}
