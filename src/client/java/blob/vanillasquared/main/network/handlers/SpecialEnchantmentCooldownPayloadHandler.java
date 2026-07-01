package blob.vanillasquared.main.network.handlers;

import blob.vanillasquared.main.gui.hud.SpecialEnchantmentCooldownClientState;
import blob.vanillasquared.main.network.payload.SpecialEnchantmentCooldownPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class SpecialEnchantmentCooldownPayloadHandler {
    private SpecialEnchantmentCooldownPayloadHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SpecialEnchantmentCooldownPayload.TYPE, (payload, context) ->
                context.client().execute(() -> SpecialEnchantmentCooldownClientState.apply(
                        payload.enchantmentId(),
                        payload.barRemaining(),
                        payload.barTotal(),
                        payload.displayValue(),
                        payload.displayKind(),
                        payload.frozen(),
                        payload.ticksDown()
                ))
        );
    }
}
