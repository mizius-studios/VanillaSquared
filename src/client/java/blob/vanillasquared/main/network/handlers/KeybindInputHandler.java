package blob.vanillasquared.main.network.handlers;

import blob.vanillasquared.main.gui.settings.controls.VSQControls;
import blob.vanillasquared.main.network.payload.DebugPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class KeybindInputHandler {
    private static boolean lastSpecialEffectKeyState = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> handleSpecialEffectKey());
    }

    private static void handleSpecialEffectKey() {
        boolean isPressed = VSQControls.specialEffectKey.isDown();
        if (isPressed && !lastSpecialEffectKeyState) {
            ClientPlayNetworking.send(new DebugPayload());
        }
        lastSpecialEffectKeyState = isPressed;
    }
}