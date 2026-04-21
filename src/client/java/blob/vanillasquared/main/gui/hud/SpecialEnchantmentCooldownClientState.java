package blob.vanillasquared.main.gui.hud;

import blob.vanillasquared.main.network.payload.SpecialEnchantmentCooldownPayload;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
    private static final Map<Identifier, HudEntry> ENTRIES = new HashMap<>();

    private SpecialEnchantmentCooldownClientState() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
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
    }

    public static Optional<VisibleCooldown> visibleCooldown(LocalPlayer player) {
        if (player == null) {
            return Optional.empty();
        }

        Optional<Identifier> mainhand = findSpecialEnchantmentId(player.getMainHandItem(), EquipmentSlot.MAINHAND);
        if (mainhand.isPresent()) {
            return visibleCooldown(mainhand.get());
        }

        Optional<Identifier> offhand = findSpecialEnchantmentId(player.getOffhandItem(), EquipmentSlot.OFFHAND);
        return offhand.flatMap(SpecialEnchantmentCooldownClientState::visibleCooldown);
    }

    public static boolean hasVisibleCooldown(LocalPlayer player) {
        return visibleCooldown(player).isPresent();
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

        ItemEnchantments enchantments = VSQEnchantmentSlots.aggregate(stack);
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            Optional<VSQEnchantmentProfile> profile = VSQEnchantmentSlots.selectedProfile(stack, enchantment);
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
        return VSQEnchantmentSlots.selectedProfile(stack, enchantment)
                .map(profile -> profile.slots().stream().anyMatch(group -> group.test(slot)))
                .orElseGet(() -> enchantment.value().matchingSlot(slot));
    }

    private static void tick() {
        ENTRIES.entrySet().removeIf(entry -> entry.getValue().ticksDown() && entry.getValue().barRemaining <= 1L);
        ENTRIES.replaceAll((id, entry) -> entry.ticksDown() ? entry.tick() : entry);
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
