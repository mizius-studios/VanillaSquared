package blob.vanillasquared.main.network;

import blob.vanillasquared.main.network.payload.EnchantingBookClickPayload;
import blob.vanillasquared.main.network.payload.EnchantingRecipeBookSyncPayload;
import blob.vanillasquared.main.network.payload.EnchantingRecipeSelectionPayload;
import blob.vanillasquared.main.network.payload.EnchantingRecipeStatePayload;
import blob.vanillasquared.main.network.payload.LungingStatePayload;
import blob.vanillasquared.main.network.payload.SpecialEnchantmentCooldownPayload;
import blob.vanillasquared.main.network.payload.SpecialEnchantmentHotkeyPayload;
import blob.vanillasquared.main.network.payload.SwirlingStatePayload;
import blob.vanillasquared.main.network.payload.VoidedSoundPayload;
import blob.vanillasquared.main.world.effect.LungingState;
import blob.vanillasquared.main.world.effect.SwirlingState;
import blob.vanillasquared.main.world.effect.VoidedEffectState;
import blob.vanillasquared.main.world.item.enchantment.SpecialEnchantmentCooldowns;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashSet;
import java.util.Set;

public final class VSQNetworking {
    private VSQNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(EnchantingBookClickPayload.TYPE, EnchantingBookClickPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EnchantingRecipeSelectionPayload.TYPE, EnchantingRecipeSelectionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SpecialEnchantmentHotkeyPayload.TYPE, SpecialEnchantmentHotkeyPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnchantingRecipeStatePayload.TYPE, EnchantingRecipeStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnchantingRecipeBookSyncPayload.TYPE, EnchantingRecipeBookSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LungingStatePayload.TYPE, LungingStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SwirlingStatePayload.TYPE, SwirlingStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SpecialEnchantmentCooldownPayload.TYPE, SpecialEnchantmentCooldownPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VoidedSoundPayload.TYPE, VoidedSoundPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EnchantingBookClickPayload.TYPE, (payload, context) ->
                context.server().execute(() -> vsq$handleEnchantingBookClick(payload, context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(EnchantingRecipeSelectionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> vsq$handleEnchantingRecipeSelection(payload, context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(SpecialEnchantmentHotkeyPayload.TYPE, (payload, context) ->
                context.server().execute(() -> vsq$handleSpecialEnchantmentHotkey(context.player()))
        );
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof LivingEntity livingEntity) {
                vsq$sendCurrentVoidedStateToPlayer(livingEntity, player);
                SwirlingState.syncToTrackingPlayer(livingEntity, player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player instanceof LivingEntity livingEntity) {
                vsq$sendCurrentVoidedStateToPlayer(livingEntity, player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SpecialEnchantmentCooldowns.clear(handler.player);
            LungingState.clear(handler.player);
            SwirlingState.clear(handler.player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SpecialEnchantmentCooldowns.tickPlayer(player);
            }
        });
    }

    public static void sendVoidedSoundState(LivingEntity entity, boolean active, boolean playIncrease, boolean playConsume) {
        if (!(entity.level() instanceof ServerLevel)) {
            return;
        }

        VoidedSoundPayload payload = new VoidedSoundPayload(entity.getId(), active, playIncrease, playConsume);
        Set<ServerPlayer> recipients = new LinkedHashSet<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer player) {
            recipients.add(player);
        }

        for (ServerPlayer recipient : recipients) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void sendLungingState(ServerPlayer player, boolean active) {
        ServerPlayNetworking.send(player, new LungingStatePayload(active));
    }

    public static void sendSwirlingState(ServerPlayer player, boolean active, int remainingTicks, int totalTicks, int warmupTicks, boolean paused) {
        sendSwirlingState(player, player.getId(), active, remainingTicks, totalTicks, warmupTicks, paused);
    }

    public static void sendSwirlingState(ServerPlayer player, int entityId, boolean active, int remainingTicks, int totalTicks, int warmupTicks, boolean paused) {
        SwirlingStatePayload payload = new SwirlingStatePayload(entityId, active, remainingTicks, totalTicks, warmupTicks, paused);
        if (entityId == player.getId()) {
            ServerPlayNetworking.send(player, payload);
        }
        Entity entity = player.level().getEntity(entityId);
        if (entity == null) {
            return;
        }
        for (ServerPlayer recipient : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    private static void vsq$sendCurrentVoidedStateToPlayer(LivingEntity entity, ServerPlayer player) {
        if (!VoidedEffectState.isActive(entity)) {
            return;
        }
        ServerPlayNetworking.send(player, new VoidedSoundPayload(entity.getId(), true, false, false));
    }

    private static void vsq$handleEnchantingBookClick(EnchantingBookClickPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof VSQEnchantmentMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            return;
        }
        menu.vsq$tryCraftEnchantingRecipe(player);
    }

    private static void vsq$handleEnchantingRecipeSelection(EnchantingRecipeSelectionPayload payload, ServerPlayer player) {
        if (!(player.containerMenu instanceof VSQEnchantmentMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            return;
        }
        menu.vsq$setSelectedDisplayId(payload.displayId());
    }

    private static void vsq$handleSpecialEnchantmentHotkey(ServerPlayer player) {
        SpecialEnchantmentCooldowns.selectUsable(player).ifPresent(use -> SpecialEnchantmentCooldowns.processHotkey(player, use));
    }
}
