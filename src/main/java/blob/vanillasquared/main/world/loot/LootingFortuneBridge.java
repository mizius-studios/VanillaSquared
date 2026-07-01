package blob.vanillasquared.main.world.loot;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;

public final class LootingFortuneBridge {
    private LootingFortuneBridge() {
    }

    public static Holder<Enchantment> remapFortuneToLooting(Holder<Enchantment> enchantment, LootContext context) {
        if (!enchantment.is(Enchantments.FORTUNE)) {
            return enchantment;
        }

        return context.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.LOOTING)
                .map(Holder.class::cast)
                .orElse(enchantment);
    }
}
