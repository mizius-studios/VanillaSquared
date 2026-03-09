package blob.vanillasquared.mixin.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Accessor("lastSlots")
    NonNullList<ItemStack> vsq$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<ItemStack> vsq$getRemoteSlots();
}
