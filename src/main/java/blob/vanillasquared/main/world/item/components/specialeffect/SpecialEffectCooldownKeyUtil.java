package blob.vanillasquared.main.world.item.components.specialeffect;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;

import java.util.Optional;

public final class SpecialEffectCooldownKeyUtil {
    private SpecialEffectCooldownKeyUtil() {
    }

    public static Identifier cooldownGroup(ItemStack stack, SpecialEffectComponent component) {
        String token = component.cooldownGroup().isBlank()
                ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
                : component.cooldownGroup();
        return Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "cooldown/special_effect/" + sanitizeIdentifierToken(token));
    }

    public static boolean isOnCooldown(ItemCooldowns cooldowns, Identifier group) {
        ItemStack probe = new ItemStack(Items.STONE);
        probe.set(DataComponents.USE_COOLDOWN, new UseCooldown(0.0F, Optional.of(group)));
        return cooldowns.isOnCooldown(probe);
    }

    public static float getCooldownPercent(ItemCooldowns cooldowns, Identifier group, float partialTick) {
        ItemStack probe = new ItemStack(Items.STONE);
        probe.set(DataComponents.USE_COOLDOWN, new UseCooldown(0.0F, Optional.of(group)));
        return cooldowns.getCooldownPercent(probe, partialTick);
    }

    private static String sanitizeIdentifierToken(String token) {
        String lower = token.toLowerCase();
        String sanitized = lower.replaceAll("[^a-z0-9/._-]", "_");
        if (sanitized.isBlank()) {
            sanitized = "id_" + Integer.toHexString(token.hashCode());
        }
        return sanitized;
    }
}
