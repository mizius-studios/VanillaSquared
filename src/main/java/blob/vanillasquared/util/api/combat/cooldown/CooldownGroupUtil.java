package blob.vanillasquared.util.api.combat.cooldown;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;

public final class CooldownGroupUtil {
    private CooldownGroupUtil() {
    }

    public static Identifier stackGroup(ItemStack stack) {
        UseCooldown useCooldown = stack.get(net.minecraft.core.component.DataComponents.USE_COOLDOWN);
        Identifier raw = useCooldown != null
                ? useCooldown.cooldownGroup().orElseGet(() -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
        return normalize(raw);
    }

    public static Identifier normalize(Identifier identifier) {
        if (identifier.getNamespace().equals(VanillaSquared.MOD_ID) && identifier.getPath().startsWith("cooldown/")) {
            return identifier;
        }
        String normalizedPath = "cooldown/" + identifier.getNamespace() + "/" + identifier.getPath();
        return Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, normalizedPath);
    }
}
