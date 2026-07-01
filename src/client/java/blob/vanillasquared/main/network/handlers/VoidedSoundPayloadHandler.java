package blob.vanillasquared.main.network.handlers;

import blob.vanillasquared.main.network.payload.VoidedSoundPayload;
import blob.vanillasquared.main.sound.VoidedSoundController;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class VoidedSoundPayloadHandler {
    private VoidedSoundPayloadHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(VoidedSoundPayload.TYPE, (payload, context) ->
                context.client().execute(() -> VoidedSoundController.apply(payload.entityId(), payload.active(), payload.playIncrease(), payload.playConsume()))
        );
    }
}
