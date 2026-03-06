package blob.vanillasquared.mixin.client.world.entities;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public class PlayerMixin {

    @Inject(method = "useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void vsq$prioritizeRod(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {

        ItemStack mainHand = player.getMainHandItem();

        if (hand == InteractionHand.MAIN_HAND
                && mainHand.getItem() instanceof AxeItem
                && player.getOffhandItem().is(Items.SHIELD)) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        if (hand == InteractionHand.MAIN_HAND && mainHand.getComponents().has(DataComponents.BLOCKS_ATTACKS)) {
            ItemStack off = player.getOffhandItem();

            if (off.getItem() instanceof FishingRodItem) {
                cir.setReturnValue(InteractionResult.PASS);
            }
        }
    }
}
