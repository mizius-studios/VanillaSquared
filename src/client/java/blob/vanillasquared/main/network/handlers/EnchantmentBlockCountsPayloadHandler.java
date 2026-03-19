package blob.vanillasquared.main.network.handlers;

import blob.vanillasquared.main.network.payload.EnchantmentBlockCountsPayload;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProperties;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantmentBlockCountsPayloadHandler {
    private static final Map<Integer, CachedCounts> VSQ$PENDING_COUNTS = new ConcurrentHashMap<>();

    private EnchantmentBlockCountsPayloadHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EnchantmentBlockCountsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    VSQ$PENDING_COUNTS.put(payload.containerId(), new CachedCounts(payload.blockIds(), payload.blockCounts(), payload.requiredBlockIds(), payload.requiredBlockCounts(), payload.levelRequirement(), payload.playerLevel()));

                    if (context.client().player == null) {
                        return;
                    }

                    if (!(context.client().player.containerMenu instanceof VSQEnchantmentMenuProperties properties)) {
                        return;
                    }

                    properties.vsq$setDetectedBlockCounts(payload.containerId(), payload.blockIds(), payload.blockCounts(), payload.requiredBlockIds(), payload.requiredBlockCounts(), payload.levelRequirement(), payload.playerLevel());
                })
        );
    }

    public static void applyCached(int containerId, VSQEnchantmentMenuProperties properties) {
        CachedCounts cachedCounts = VSQ$PENDING_COUNTS.get(containerId);
        if (cachedCounts == null) {
            return;
        }

        properties.vsq$setDetectedBlockCounts(containerId, cachedCounts.blockIds(), cachedCounts.blockCounts(), cachedCounts.requiredBlockIds(), cachedCounts.requiredBlockCounts(), cachedCounts.levelRequirement(), cachedCounts.playerLevel());
    }

    private record CachedCounts(List<Identifier> blockIds, List<Integer> blockCounts, List<Identifier> requiredBlockIds, List<Integer> requiredBlockCounts, int levelRequirement, int playerLevel) {
        private CachedCounts {
            blockIds = List.copyOf(blockIds);
            blockCounts = List.copyOf(blockCounts);
            requiredBlockIds = List.copyOf(requiredBlockIds);
            requiredBlockCounts = List.copyOf(requiredBlockCounts);
        }
    }
}
