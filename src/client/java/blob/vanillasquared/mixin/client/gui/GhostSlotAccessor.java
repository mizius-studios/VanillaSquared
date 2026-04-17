package blob.vanillasquared.mixin.client.gui;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.GhostSlots$GhostSlot")
public interface GhostSlotAccessor {
    @Invoker("getItem")
    ItemStack vsq$getItem(int index);

    @Accessor("isResultSlot")
    boolean vsq$isResultSlot();
}
