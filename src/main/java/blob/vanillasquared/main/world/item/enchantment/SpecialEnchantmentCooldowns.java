package blob.vanillasquared.main.world.item.enchantment;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.main.network.payload.SpecialEnchantmentCooldownPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SpecialEnchantmentCooldowns {
    private static final Map<UUID, Map<Identifier, ActivationState>> STATES = new HashMap<>();

    private SpecialEnchantmentCooldowns() {
    }

    public static Optional<SpecialEnchantmentUse> selectUsable(ServerPlayer player) {
        logInvalidSlots(player);

        Optional<SpecialEnchantmentUse> mainhand = findSpecial(player.getMainHandItem(), EquipmentSlot.MAINHAND);
        if (mainhand.isPresent()) {
            return mainhand;
        }
        return findSpecial(player.getOffhandItem(), EquipmentSlot.OFFHAND);
    }

    public static void processHotkey(ServerPlayer player, SpecialEnchantmentUse use) {
        ActivationState state = state(player, use.enchantmentId()).orElseGet(() -> {
            ActivationState created = createState(use);
            putState(player, use.enchantmentId(), created);
            return created;
        });

        state.activatedByHotkey = true;
        startCooldownIfEligible(player.level().getGameTime(), use.profile(), state);

        if (shouldClearState(use.profile(), state)) {
            removeState(player, use.enchantmentId());
        }
        sync(player, use);
    }

    public static void tickPlayer(ServerPlayer player) {
        Map<Identifier, ActivationState> playerStates = STATES.get(player.getUUID());
        if (playerStates == null || playerStates.isEmpty()) {
            return;
        }

        Set<Identifier> enchantmentIds = Set.copyOf(playerStates.keySet());
        for (Identifier enchantmentId : enchantmentIds) {
            ActivationState state = playerStates.get(enchantmentId);
            if (state == null) {
                continue;
            }

            if (isFinished(player, state)) {
                playerStates.remove(enchantmentId);
                continue;
            }

            Optional<SpecialEnchantmentUse> maybeUse = findUseInHands(player, enchantmentId);
            if (maybeUse.isEmpty()) {
                if (!state.displayedNone) {
                    state.displayedNone = true;
                    ServerPlayNetworking.send(
                            player,
                            new SpecialEnchantmentCooldownPayload(
                                    enchantmentId,
                                    0L,
                                    0L,
                                    0,
                                    SpecialEnchantmentCooldownPayload.DISPLAY_NONE,
                                    false,
                                    false
                            )
                    );
                }
                continue;
            }
            state.displayedNone = false;
            SpecialEnchantmentUse use = maybeUse.get();
            sync(player, use);
        }

        if (playerStates.isEmpty()) {
            STATES.remove(player.getUUID());
        }
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static boolean shouldRunSpecialEffect(
            ServerLevel level,
            ItemStack stack,
            Enchantment enchantment,
            DataComponentType<?> componentType,
            int effectIndex,
            @Nullable Entity contextEntity
    ) {
        Optional<ServerPlayer> maybePlayer = findPlayerContext(contextEntity);
        if (maybePlayer.isEmpty()) {
            return true;
        }
        ServerPlayer player = maybePlayer.get();

        Optional<VSQEnchantmentProfile> profile = VSQEnchantmentSlots.selectedProfile(stack, enchantment);
        if (profile.isEmpty() || profile.get().special().isEmpty()) {
            return true;
        }

        String componentKey = componentKey(componentType);
        Optional<SpecialEffectMetadata> metadata = profile.get().specialEffectIndex().metadata(componentKey, effectIndex);
        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Identifier enchantmentId = enchantmentRegistry
                .getResourceKey(enchantment)
                .map(ResourceKey::identifier)
                .orElse(null);
        if (enchantmentId == null) {
            return true;
        }
        ActivationState state = state(player, enchantmentId).orElse(null);
        if (state == null || !isActivationWindowOpen(state, profile.get())) {
            return false;
        }

        Holder<Enchantment> enchantmentHolder = enchantmentRegistry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, enchantmentId));
        int enchantmentLevel = VSQEnchantmentSlots.aggregate(stack).getLevel(enchantmentHolder);
        EquipmentSlot slot = player.getOffhandItem() == stack ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        SpecialEnchantmentUse use = new SpecialEnchantmentUse(
                enchantmentId,
                enchantmentHolder,
                enchantmentLevel,
                stack,
                slot,
                profile.get(),
                profile.get().special().orElseThrow()
        );

        boolean allowed = consumeLimitIfNeeded(use, state, metadata.orElse(null));
        startCooldownIfEligible(level.getGameTime(), profile.get(), state);
        if (shouldClearState(profile.get(), state)) {
            removeState(player, enchantmentId);
        }
        syncForEnchantment(player, enchantmentId);
        return allowed;
    }

    private static boolean consumeLimitIfNeeded(
            SpecialEnchantmentUse use,
            ActivationState state,
            @Nullable SpecialEffectMetadata metadata
    ) {
        if (metadata == null || metadata.special().isEmpty()) {
            return true;
        }

        String id = metadata.id();
        SpecialEffectSettings settings = metadata.special().orElseThrow();

        int limit = resolvedLimit(use, settings);
        if (limit == 0) {
            return false;
        }
        if (limit < 0) {
            return true;
        }

        int remaining = state.remainingLimits.getOrDefault(id, limit);
        if (remaining <= 0) {
            return false;
        }

        remaining--;
        state.remainingLimits.put(id, remaining);
        return true;
    }

    private static int resolvedLimit(SpecialEnchantmentUse use, SpecialEffectSettings settings) {
        return Mth.floor(settings.limit().calculate(use.level()));
    }

    private static Optional<ServerPlayer> findPlayerContext(@Nullable Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        if (entity instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    private static Optional<ActivationState> state(ServerPlayer player, Identifier enchantmentId) {
        Map<Identifier, ActivationState> playerStates = STATES.get(player.getUUID());
        if (playerStates == null) {
            return Optional.empty();
        }
        ActivationState state = playerStates.get(enchantmentId);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(state);
    }

    private static void putState(ServerPlayer player, Identifier enchantmentId, ActivationState state) {
        STATES.computeIfAbsent(player.getUUID(), uuid -> new HashMap<>()).put(enchantmentId, state);
    }

    private static void removeState(ServerPlayer player, Identifier enchantmentId) {
        Map<Identifier, ActivationState> playerStates = STATES.get(player.getUUID());
        if (playerStates == null) {
            return;
        }
        playerStates.remove(enchantmentId);
        if (playerStates.isEmpty()) {
            STATES.remove(player.getUUID());
        }
    }

    private static ActivationState createState(SpecialEnchantmentUse use) {
        ActivationState state = new ActivationState(use.special().cooldownTicks());
        for (SpecialEffectMetadata metadata : use.profile().specialEffectIndex().all()) {
            metadata.special().ifPresent(settings -> {
                int limit = resolvedLimit(use, settings);
                if (limit > 0) {
                    state.remainingLimits.put(metadata.id(), limit);
                    state.totalLimits.put(metadata.id(), limit);
                }
            });
        }
        return state;
    }

    private static void startCooldownIfEligible(long gameTime, VSQEnchantmentProfile profile, ActivationState state) {
        if (state.cooldownStarted || isPreCooldownPhaseActive(profile, state)) {
            return;
        }
        SpecialEnchantmentProfileConfig special = profile.special().orElse(null);
        if (special == null) {
            return;
        }
        long totalTicks = special.cooldownTicks();
        if (totalTicks <= 0L) {
            state.cooldownStarted = true;
            state.cooldownEndTick = gameTime;
            return;
        }
        state.cooldownStarted = true;
        state.cooldownEndTick = gameTime + totalTicks;
    }

    private static boolean isActivationWindowOpen(ActivationState state, VSQEnchantmentProfile profile) {
        if (!state.activatedByHotkey) {
            return false;
        }
        return state.cooldownStarted || isPreCooldownPhaseActive(profile, state);
    }

    private static boolean isPreCooldownPhaseActive(VSQEnchantmentProfile profile, ActivationState state) {
        if (state.cooldownStarted) {
            return false;
        }
        SpecialEnchantmentProfileConfig config = profile.special().orElse(null);
        if (config == null) {
            return false;
        }
        return isCooldownAfterLimitBlocked(config, state);
    }

    private static boolean isCooldownAfterLimitBlocked(SpecialEnchantmentProfileConfig config, ActivationState state) {
        return config.cooldownAfterLimit()
                .map(id -> state.remainingLimits.getOrDefault(id, 0) > 0)
                .orElse(false);
    }

    private static boolean shouldClearState(VSQEnchantmentProfile profile, ActivationState state) {
        return !state.cooldownStarted && !isPreCooldownPhaseActive(profile, state);
    }

    private static boolean isFinished(ServerPlayer player, ActivationState state) {
        if (!state.cooldownStarted) {
            return false;
        }
        return state.cooldownEndTick - player.level().getGameTime() <= 0L;
    }

    private static void sync(ServerPlayer player, SpecialEnchantmentUse use) {
        ActivationState state = state(player, use.enchantmentId()).orElse(null);
        if (state == null) {
            ServerPlayNetworking.send(player, new SpecialEnchantmentCooldownPayload(use.enchantmentId(), 0L, 0L, 0, SpecialEnchantmentCooldownPayload.DISPLAY_NONE, false, false));
            return;
        }
        ServerPlayNetworking.send(player, snapshot(player, use, state));
    }

    private static void syncForEnchantment(ServerPlayer player, Identifier enchantmentId) {
        findUseInHands(player, enchantmentId).ifPresentOrElse(
                use -> sync(player, use),
                () -> ServerPlayNetworking.send(
                        player,
                        new SpecialEnchantmentCooldownPayload(enchantmentId, 0L, 0L, 0, SpecialEnchantmentCooldownPayload.DISPLAY_NONE, false, false)
                )
        );
    }

    private static SpecialEnchantmentCooldownPayload snapshot(ServerPlayer player, SpecialEnchantmentUse use, ActivationState state) {
        SpecialEnchantmentProfileConfig config = use.special();
        boolean blockedByLimit = isCooldownAfterLimitBlocked(config, state);
        Optional<String> displayRef = config.displayLimit().filter(id -> state.totalLimits.getOrDefault(id, 0) > 0);
        int displayRemaining = displayRef.map(id -> state.remainingLimits.getOrDefault(id, 0)).orElse(0);
        int displayTotal = displayRef.map(id -> state.totalLimits.getOrDefault(id, 0)).orElse(0);

        if (state.cooldownStarted) {
            long remaining = Math.max(0L, state.cooldownEndTick - player.level().getGameTime());
            if (remaining <= 0L) {
                return new SpecialEnchantmentCooldownPayload(use.enchantmentId(), 0L, 0L, 0, SpecialEnchantmentCooldownPayload.DISPLAY_NONE, false, false);
            }
            if (displayRemaining > 0) {
                return new SpecialEnchantmentCooldownPayload(use.enchantmentId(), remaining, state.cooldownTotalTicks, displayRemaining, SpecialEnchantmentCooldownPayload.DISPLAY_LIMIT, false, true);
            }
            return new SpecialEnchantmentCooldownPayload(use.enchantmentId(), remaining, state.cooldownTotalTicks, ticksToSeconds(remaining), SpecialEnchantmentCooldownPayload.DISPLAY_COOLDOWN, false, true);
        }

        if (blockedByLimit && displayRemaining > 0 && config.cooldownAfterLimit().equals(displayRef)) {
            return new SpecialEnchantmentCooldownPayload(use.enchantmentId(), displayRemaining, Math.max(1, displayTotal), displayRemaining, SpecialEnchantmentCooldownPayload.DISPLAY_LIMIT, false, false);
        }
        if (blockedByLimit && displayRemaining > 0) {
            return new SpecialEnchantmentCooldownPayload(use.enchantmentId(), 1L, 1L, displayRemaining, SpecialEnchantmentCooldownPayload.DISPLAY_LIMIT, true, false);
        }
        if (blockedByLimit) {
            return new SpecialEnchantmentCooldownPayload(use.enchantmentId(), 1L, 1L, ticksToSeconds(state.cooldownTotalTicks), SpecialEnchantmentCooldownPayload.DISPLAY_COOLDOWN, true, false);
        }
        return new SpecialEnchantmentCooldownPayload(use.enchantmentId(), 0L, 0L, 0, SpecialEnchantmentCooldownPayload.DISPLAY_NONE, false, false);
    }

    private static int ticksToSeconds(long ticks) {
        return (int) Math.max(1L, (ticks + 19L) / 20L);
    }

    private static Optional<SpecialEnchantmentUse> findSpecial(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        ItemEnchantments enchantments = VSQEnchantmentSlots.aggregate(stack);
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            Optional<VSQEnchantmentProfile> profile = VSQEnchantmentSlots.selectedProfile(stack, enchantment);
            if (profile.isEmpty() || profile.get().special().isEmpty() || !matchesSlot(stack, enchantment, slot)) {
                continue;
            }

            Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
            if (key.isPresent()) {
                return Optional.of(new SpecialEnchantmentUse(
                        key.get().identifier(),
                        enchantment,
                        enchantments.getLevel(enchantment),
                        stack,
                        slot,
                        profile.get(),
                        profile.get().special().orElseThrow()
                ));
            }
        }
        return Optional.empty();
    }

    private static Optional<SpecialEnchantmentUse> findUseInHands(ServerPlayer player, Identifier enchantmentId) {
        Optional<SpecialEnchantmentUse> mainhand = findSpecial(player.getMainHandItem(), EquipmentSlot.MAINHAND)
                .filter(use -> use.enchantmentId().equals(enchantmentId));
        if (mainhand.isPresent()) {
            return mainhand;
        }
        return findSpecial(player.getOffhandItem(), EquipmentSlot.OFFHAND)
                .filter(use -> use.enchantmentId().equals(enchantmentId));
    }

    private static String componentKey(DataComponentType<?> componentType) {
        Identifier id = BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.getKey(componentType);
        return id == null ? "" : id.toString();
    }

    private static void logInvalidSlots(ServerPlayer player) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                continue;
            }
            logInvalidSlot(player, player.getItemBySlot(slot), slot.getName());
        }

        if (player.getVehicle() instanceof AbstractHorse horse) {
            logInvalidSlot(player, horse.getItemBySlot(EquipmentSlot.BODY), "mount body");
            logInvalidSlot(player, horse.getItemBySlot(EquipmentSlot.SADDLE), "mount saddle");
        }
    }

    private static void logInvalidSlot(ServerPlayer player, ItemStack stack, String slotName) {
        findSpecialIgnoringSlot(stack).ifPresent(use -> VanillaSquared.LOGGER.error(
                "Player {} has special enchantment effect {} in {}; special enchantment effects only work in mainhand/offhand",
                player.getName().getString(),
                use.enchantmentId(),
                slotName
        ));
    }

    private static Optional<SpecialEnchantmentUse> findSpecialIgnoringSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        ItemEnchantments enchantments = VSQEnchantmentSlots.aggregate(stack);
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            Optional<VSQEnchantmentProfile> profile = VSQEnchantmentSlots.selectedProfile(stack, enchantment);
            if (profile.isEmpty() || profile.get().special().isEmpty()) {
                continue;
            }
            Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
            if (key.isPresent()) {
                return Optional.of(new SpecialEnchantmentUse(
                        key.get().identifier(),
                        enchantment,
                        enchantments.getLevel(enchantment),
                        stack,
                        EquipmentSlot.MAINHAND,
                        profile.get(),
                        profile.get().special().orElseThrow()
                ));
            }
        }
        return Optional.empty();
    }

    private static boolean matchesSlot(ItemStack stack, Holder<Enchantment> enchantment, EquipmentSlot slot) {
        return VSQEnchantmentSlots.selectedProfile(stack, enchantment)
                .map(profile -> profile.slots().stream().anyMatch(group -> group.test(slot)))
                .orElseGet(() -> enchantment.value().matchingSlot(slot));
    }

    private static final class ActivationState {
        private final long cooldownTotalTicks;
        private final Map<String, Integer> remainingLimits = new HashMap<>();
        private final Map<String, Integer> totalLimits = new HashMap<>();
        private boolean activatedByHotkey;
        private boolean cooldownStarted;
        private boolean displayedNone;
        private long cooldownEndTick;

        private ActivationState(long cooldownTotalTicks) {
            this.cooldownTotalTicks = cooldownTotalTicks;
        }
    }

    public record SpecialEnchantmentUse(
            Identifier enchantmentId,
            Holder<Enchantment> enchantment,
            int level,
            ItemStack stack,
            EquipmentSlot slot,
            VSQEnchantmentProfile profile,
            SpecialEnchantmentProfileConfig special
    ) {
    }
}
