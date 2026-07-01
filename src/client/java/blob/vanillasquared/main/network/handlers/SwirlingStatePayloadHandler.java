package blob.vanillasquared.main.network.handlers;

import blob.vanillasquared.main.gui.hud.SwirlingClientState;
import blob.vanillasquared.main.network.payload.SwirlingStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.Entity;

public final class SwirlingStatePayloadHandler {
    private SwirlingStatePayloadHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SwirlingStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> SwirlingClientState.setActive(
                        payload.entityId(),
                        payload.active(),
                        payload.remainingTicks(),
                        payload.totalTicks(),
                        payload.warmupTicks(),
                        payload.paused(),
                        ageInTicks(context, payload.entityId())
                ))
        );
    }

    private static float ageInTicks(ClientPlayNetworking.Context context, int entityId) {
        if (context.client().level == null) {
            return 0.0F;
        }
        Entity entity = context.client().level.getEntity(entityId);
        return entity == null ? 0.0F : entity.tickCount;
    }
}
