package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.world.inventory.EnchantmentMenuRedirectProvider;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProvider;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerOpenMenuMixin extends Player {

    protected ServerPlayerOpenMenuMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At("HEAD"), cancellable = true)
    private void vsq$redirectVanillaEnchantmentMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (provider == null || provider instanceof EnchantmentMenuRedirectProvider) {
            return;
        }
        if (!(provider instanceof VSQEnchantmentMenuProvider vsqProvider)) {
            return;
        }
        BlockPos openingPos = vsqProvider.getScreenOpeningData((ServerPlayer) (Object) this);
        if (VSQEnchantmentMenu.SYNTHETIC_OPEN_POS.equals(openingPos)) {
            return;
        }

        Component title = provider.getDisplayName();
        VanillaSquared.LOGGER.debug("Redirecting vanilla EnchantmentMenu open to VSQEnchantmentMenu via {}", provider.getClass().getName());
        cir.setReturnValue(((ServerPlayer) (Object) this).openMenu(new EnchantmentMenuRedirectProvider(title, openingPos)));
    }
}
