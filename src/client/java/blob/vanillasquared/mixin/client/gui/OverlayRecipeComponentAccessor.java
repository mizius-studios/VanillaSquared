package blob.vanillasquared.mixin.client.gui;

import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.SlotSelectTime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OverlayRecipeComponent.class)
public interface OverlayRecipeComponentAccessor {
    @Accessor("slotSelectTime")
    SlotSelectTime vsq$getSlotSelectTime();
}
