package blob.vanillasquared.mixin.world.inventory;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {

    @Inject(method = "getOffers", at = @At("RETURN"), cancellable = true)
    private void vsq$filterAestheticTrades(CallbackInfoReturnable<MerchantOffers> cir) {
        MerchantOffers offers = cir.getReturnValue();
        if (offers != null && !offers.isEmpty()) {
            offers.removeIf(offer -> offer.getResult().is(Items.ENCHANTED_BOOK));
        }
    }
}
