package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void vsq$warnClientFallback(int containerId, Inventory playerInventory, CallbackInfo ci) {
        this.vsq$warnFallback(playerInventory);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void vsq$warnServerFallback(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.vsq$warnFallback(playerInventory);
    }

    private void vsq$warnFallback(Inventory playerInventory) {
        if (!playerInventory.player.level().isClientSide()) {
            VanillaSquared.LOGGER.warn("Vanilla EnchantmentMenu was constructed after VSQ redirect. This path should be rare and may indicate another mod bypassed ServerPlayer.openMenu redirection.");
        }
    }
}
