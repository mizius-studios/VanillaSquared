package blob.vanillasquared.main.world.loot;

import net.minecraft.resources.Identifier;

import java.util.Optional;

public interface LootContextBridge {
    Optional<Identifier> vsq$currentLootTableId();
}
