package blob.vanillasquared.main.world.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class LootTableIdResolver {
    private static final Map<HolderLookup.Provider, IdentityHashMap<LootTable, Identifier>> LOOKUP_CACHE =
            new ConcurrentHashMap<>();

    private LootTableIdResolver() {
    }

    public static void clearCache() {
        LOOKUP_CACHE.clear();
    }

    public static Optional<Identifier> lookup(LootContext context, LootTable targetTable) {
        if (!(context.getResolver() instanceof HolderLookup.Provider provider)) {
            return Optional.empty();
        }

        IdentityHashMap<LootTable, Identifier> reverseLookup = LOOKUP_CACHE.computeIfAbsent(provider, p -> {
            IdentityHashMap<LootTable, Identifier> built = new IdentityHashMap<>();
            p.lookupOrThrow(Registries.LOOT_TABLE)
                    .listElements()
                    .forEach(holder -> built.put(holder.value(), holder.key().identifier()));
            return built;
        });

        return Optional.ofNullable(reverseLookup.get(targetTable));
    }
}
