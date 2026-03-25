package blob.vanillasquared.main.world.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public interface VSQEnchantmentMenuProperties {
    int vsq$getPlayerLevel();
    int vsq$getBlockAmount();
    int vsq$getLevelRequirement();
    int vsq$getBlockRequirement();
    boolean vsq$hasRequiredXp();
    boolean vsq$hasRequiredBlocks();
    List<Component> vsq$getDetectedBlockTooltipLines();
    void vsq$setDetectedBlockCounts(int containerId, List<Identifier> blockIds, List<Integer> counts, List<Integer> requiredBlockCounts, int levelRequirement, int blockRequirement, int playerLevel);
    boolean vsq$tryCraftEnchantingRecipe(ServerPlayer player);
}
