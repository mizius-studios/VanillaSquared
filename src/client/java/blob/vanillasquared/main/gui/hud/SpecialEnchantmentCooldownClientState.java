package blob.vanillasquared.main.gui.hud;

import blob.vanillasquared.main.network.payload.SpecialEnchantmentCooldownPayload;
import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SpecialEnchantmentCooldownClientState {
    private static final long SWAP_GRACE_TICKS = 3L;
    private static final Map<Identifier, HudEntry> ENTRIES = new HashMap<>();
    private static Optional<VisibleCooldown> lastVisibleCooldown = Optional.empty();
    private static Optional<Identifier> lastVisibleEnchantmentId = Optional.empty();
    private static long swapGraceExpiryTick = -1L;
    private static long clientTick = 0L;

    private SpecialEnchantmentCooldownClientState() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(SpecialEnchantmentCooldownClientState::tick);
    }

    public static void apply(Identifier enchantmentId, long barRemaining, long barTotal, int displayValue, int displayKind, boolean frozen, boolean ticksDown) {
        if (barRemaining <= 0L || barTotal <= 0L || displayKind == SpecialEnchantmentCooldownPayload.DISPLAY_NONE) {
            ENTRIES.remove(enchantmentId);
            return;
        }
        ENTRIES.put(enchantmentId, new HudEntry(barRemaining, barTotal, displayValue, displayKind, frozen, ticksDown));
    }

    public static void clear() {
        ENTRIES.clear();
        clearBridgeState();
    }

    public static Optional<VisibleCooldown> visibleCooldown(LocalPlayer player) {
        if (player == null) {
            return Optional.empty();
        }

        ItemStack mainhandStack = player.getMainHandItem();
        ItemStack offhandStack = player.getOffhandItem();
        Optional<Identifier> mainhand = findSpecialEnchantmentId(mainhandStack, EquipmentSlot.MAINHAND);
        Optional<Identifier> offhand = findSpecialEnchantmentId(offhandStack, EquipmentSlot.OFFHAND);
        return visibleCooldown(new HeldSpecialState(mainhand, offhand, !mainhandStack.isEmpty(), !offhandStack.isEmpty()));
    }

    public static boolean hasVisibleCooldown(LocalPlayer player) {
        return visibleCooldown(player).isPresent();
    }

    public static boolean shouldReserveContextualBar() {
        if (clientTick > swapGraceExpiryTick) {
            clearBridgeState();
        }
        Minecraft client = Minecraft.getInstance();
        return client != null && visibleCooldown(client.player).isPresent();
    }

    static boolean shouldReserveContextualBar(Optional<Identifier> mainhand, Optional<Identifier> offhand) {
        if (clientTick > swapGraceExpiryTick) {
            clearBridgeState();
        }
        return visibleCooldown(new HeldSpecialState(mainhand, offhand, mainhand.isPresent(), offhand.isPresent())).isPresent();
    }

    static Optional<VisibleCooldown> visibleCooldown(Optional<Identifier> mainhand, Optional<Identifier> offhand) {
        return visibleCooldown(new HeldSpecialState(mainhand, offhand, mainhand.isPresent(), offhand.isPresent()));
    }

    static Optional<VisibleCooldown> visibleCooldown(Optional<Identifier> mainhand, boolean mainhandHasItem, Optional<Identifier> offhand, boolean offhandHasItem) {
        return visibleCooldown(new HeldSpecialState(mainhand, offhand, mainhandHasItem, offhandHasItem));
    }

    private static Optional<VisibleCooldown> visibleCooldown(HeldSpecialState heldState) {
        Optional<VisibleCooldown> currentMainhand = heldState.mainhand().flatMap(SpecialEnchantmentCooldownClientState::visibleCooldown);
        if (currentMainhand.isPresent()) {
            cacheVisibleCooldown(heldState.mainhand().orElseThrow(), currentMainhand.get());
            return currentMainhand;
        }

        Optional<VisibleCooldown> currentOffhand = heldState.offhand().flatMap(SpecialEnchantmentCooldownClientState::visibleCooldown);
        if (currentOffhand.isPresent()) {
            cacheVisibleCooldown(heldState.offhand().orElseThrow(), currentOffhand.get());
            return currentOffhand;
        }

        return bridgedVisibleCooldown(heldState);
    }

    private static Optional<VisibleCooldown> visibleCooldown(Identifier enchantmentId) {
        HudEntry entry = ENTRIES.get(enchantmentId);
        if (entry == null || entry.barRemaining() <= 0L || entry.barTotal() <= 0L) {
            return Optional.empty();
        }
        return Optional.of(new VisibleCooldown(entry.barRemaining(), entry.barTotal(), entry.displayValue(), entry.displayKind(), entry.frozen()));
    }

    private static Optional<Identifier> findSpecialEnchantmentId(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        ItemEnchantments enchantments = VSQEnchantments.aggregate(stack);
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            Optional<VSQEnchantmentProfile> profile = VSQEnchantments.selectedProfile(stack, enchantment);
            if (profile.isEmpty() || profile.get().special().isEmpty() || !matchesSlot(stack, enchantment, slot)) {
                continue;
            }

            Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
            if (key.isPresent()) {
                return Optional.of(key.get().identifier());
            }
        }
        return Optional.empty();
    }

    private static boolean matchesSlot(ItemStack stack, Holder<Enchantment> enchantment, EquipmentSlot slot) {
        return VSQEnchantments.selectedProfile(stack, enchantment)
                .map(profile -> profile.slots().stream().anyMatch(group -> group.test(slot)))
                .orElseGet(() -> enchantment.value().matchingSlot(slot));
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            clearBridgeState();
            return;
        }
        if (client.isPaused()) {
            return;
        }
        clientTick++;
        advanceCooldowns();
        if (client.player == null || clientTick > swapGraceExpiryTick || lastVisibleCooldown.isEmpty()) {
            clearBridgeState();
        }
    }

    private static void advanceCooldowns() {
        ENTRIES.entrySet().removeIf(entry -> entry.getValue().ticksDown() && entry.getValue().barRemaining <= 1L);
        ENTRIES.replaceAll((_, entry) -> entry.ticksDown() ? entry.tick() : entry);
    }

    static void advanceCooldownsForTest() {
        advanceCooldowns();
    }

    static void advanceTickForTest(boolean hasPlayer) {
        clientTick++;
        if (!hasPlayer) {
            clearBridgeState();
            return;
        }
        if (clientTick > swapGraceExpiryTick || lastVisibleCooldown.isEmpty()) {
            clearBridgeState();
        }
    }

    private static Optional<VisibleCooldown> bridgedVisibleCooldown(HeldSpecialState heldState) {
        if (clientTick > swapGraceExpiryTick) {
            clearBridgeState();
            return Optional.empty();
        }

        Optional<Identifier> cachedEnchantmentId = lastVisibleEnchantmentId;
        if (cachedEnchantmentId.isEmpty()) {
            return Optional.empty();
        }

        boolean switchingAwayFromCachedSpecial = isSwitchingAwayFromCachedSpecial(cachedEnchantmentId.get(), heldState);
        if (!switchingAwayFromCachedSpecial) {
            clearBridgeState();
            return Optional.empty();
        }

        Optional<VisibleCooldown> current = visibleCooldown(cachedEnchantmentId.get());
        if (current.isEmpty()) {
            if (lastVisibleCooldown.isPresent()) {
                return lastVisibleCooldown;
            }
            clearBridgeState();
            return Optional.empty();
        }

        VisibleCooldown cooldown = current.get();
        lastVisibleCooldown = Optional.of(cooldown);
        return lastVisibleCooldown;
    }

    private static boolean isSwitchingAwayFromCachedSpecial(Identifier cachedEnchantmentId, HeldSpecialState heldState) {
        if (heldState.mainhand().filter(id -> !id.equals(cachedEnchantmentId)).isPresent()
                || heldState.offhand().filter(id -> !id.equals(cachedEnchantmentId)).isPresent()) {
            return true;
        }

        return heldState.mainhand().isEmpty()
                && heldState.offhand().isEmpty()
                && !heldState.mainhandHasItem()
                && !heldState.offhandHasItem();
    }

    private static void cacheVisibleCooldown(Identifier enchantmentId, VisibleCooldown cooldown) {
        lastVisibleCooldown = Optional.of(cooldown);
        lastVisibleEnchantmentId = Optional.of(enchantmentId);
        swapGraceExpiryTick = clientTick + SWAP_GRACE_TICKS;
    }

    private static void clearBridgeState() {
        lastVisibleCooldown = Optional.empty();
        lastVisibleEnchantmentId = Optional.empty();
        swapGraceExpiryTick = -1L;
    }

    private record HeldSpecialState(Optional<Identifier> mainhand, Optional<Identifier> offhand, boolean mainhandHasItem, boolean offhandHasItem) {
    }

    private record HudEntry(long barRemaining, long barTotal, int displayValue, int displayKind, boolean frozen, boolean ticksDown) {
        private HudEntry tick() {
            long remaining = Math.max(0L, this.barRemaining - 1L);
            int display = this.displayKind == SpecialEnchantmentCooldownPayload.DISPLAY_COOLDOWN
                    ? (int) Math.max(1L, (remaining + 19L) / 20L)
                    : this.displayValue;
            return new HudEntry(remaining, this.barTotal, display, this.displayKind, false, true);
        }
    }

    public record VisibleCooldown(long barRemaining, long barTotal, int displayValue, int displayKind, boolean frozen) {
        public int progressWidth() {
            if (this.barTotal <= 0L) {
                return 0;
            }
            double remaining = (double) this.barRemaining / (double) this.barTotal;
            return Math.max(0, Math.min(182, (int) Math.round(remaining * 182.0D)));
        }

        public String displayText() {
            if (this.displayKind == SpecialEnchantmentCooldownPayload.DISPLAY_COOLDOWN) {
                return this.displayValue + "s";
            }
            return Integer.toString(this.displayValue);
        }

        public int textColor() {
            if (this.frozen) {
                return 0xFFFF5555;
            }
            if (this.displayKind == SpecialEnchantmentCooldownPayload.DISPLAY_LIMIT) {
                return 0xFFB45CFF;
            }
            return 0xFF6EE7FF;
        }
    }
}
