package blob.vanillasquared.mixin.client.gui;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.SlotSelectTime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GhostSlots.class)
public abstract class GhostSlotsMixin {
    @Shadow
    @Final
    private Reference2ObjectMap<Slot, Object> ingredients;

    @Shadow
    @Final
    private SlotSelectTime slotSelectTime;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void vsq$renderInputDecorations(GuiGraphicsExtractor graphics, Minecraft minecraft, boolean isResultSlotBig, CallbackInfo ci) {
        int currentIndex = this.slotSelectTime.currentIndex();
        this.ingredients.forEach((slot, ghostSlot) -> {
            GhostSlotAccessor accessor = (GhostSlotAccessor) ghostSlot;
            if (accessor.vsq$isResultSlot()) {
                return;
            }

            ItemStack stack = accessor.vsq$getItem(currentIndex);
            if (stack.isEmpty()) {
                return;
            }

            graphics.itemDecorations(minecraft.font, stack, slot.x, slot.y);
        });
    }
}
