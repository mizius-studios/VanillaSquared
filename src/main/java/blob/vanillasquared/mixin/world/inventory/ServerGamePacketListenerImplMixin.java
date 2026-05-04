package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.network.payload.EnchantingRecipeBookSyncPayload;
import blob.vanillasquared.main.world.effect.LungingState;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipe;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
    private void vsq$freezeLungingInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        if (!LungingState.isLunging(this.player)) {
            return;
        }
        this.player.setLastClientInput(Input.EMPTY);
        this.player.setShiftKeyDown(false);
        this.player.resetLastActionTime();
        ci.cancel();
    }

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void vsq$handlePlaceRecipe(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (!(this.player.containerMenu instanceof VSQEnchantmentMenu menu)) {
            return;
        }
        if (menu.containerId != packet.containerId()) {
            ci.cancel();
            return;
        }
        menu.vsq$setSelectedDisplayId(packet.recipe().index());
        RecipeHolder<EnchantingRecipe> recipeHolder = menu.vsq$getDisplayRecipes().get(packet.recipe().index());
        VSQEnchantmentMenu.PlacementOutcome outcome = menu.vsq$handleRecipeBookPlacement(this.player, packet.recipe().index());
        if (!outcome.fullyPlaced() && recipeHolder != null) {
            this.player.connection.send(new ClientboundPlaceGhostRecipePacket(
                    menu.containerId,
                    EnchantingRecipeBookSyncPayload.createGhostDisplay(recipeHolder.value(), this.player.registryAccess(), outcome.missingInput())
            ));
        }
        ci.cancel();
    }
}
