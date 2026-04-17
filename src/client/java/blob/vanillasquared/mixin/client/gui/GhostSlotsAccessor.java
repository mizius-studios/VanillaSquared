package blob.vanillasquared.mixin.client.gui;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GhostSlots.class)
public interface GhostSlotsAccessor {
    @Invoker("setInput")
    void vsq$setInput(Slot slot, ContextMap contextMap, SlotDisplay slotDisplay);

    @Accessor("ingredients")
    Reference2ObjectMap<Slot, Object> vsq$getIngredients();
}
