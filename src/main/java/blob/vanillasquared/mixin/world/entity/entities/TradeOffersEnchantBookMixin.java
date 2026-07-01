package blob.vanillasquared.mixin.world.entity.entities;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MerchantOffer.class)
public abstract class TradeOffersEnchantBookMixin {

    @Shadow @Final private ItemStack result;
    @Shadow private int uses;
    @Shadow @Final private int maxUses;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void vsq$cancelEnchantedBookOffers(CallbackInfo ci) {
        if (this.result.is(Items.ENCHANTED_BOOK)) {
            this.uses = this.maxUses;
        }
    }
}
