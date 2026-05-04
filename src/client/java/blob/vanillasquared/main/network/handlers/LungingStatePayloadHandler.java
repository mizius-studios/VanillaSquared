package blob.vanillasquared.main.network.handlers;

import blob.vanillasquared.main.gui.hud.LungingClientState;
import blob.vanillasquared.main.network.payload.LungingStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class LungingStatePayloadHandler {
    private LungingStatePayloadHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(LungingStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> LungingClientState.setActive(payload.active()))
        );
    }
}
