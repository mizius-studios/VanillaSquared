package blob.vanillasquared.main.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class EnchantmentMenuRedirectProvider implements VSQEnchantmentMenuProvider {
    private final Component title;
    private final BlockPos openingPos;

    public EnchantmentMenuRedirectProvider(Component title, BlockPos openingPos) {
        this.title = title;
        this.openingPos = openingPos;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.openingPos;
    }

    @Override
    public Component getDisplayName() {
        return this.title;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VSQEnchantmentMenu(containerId, inventory, this.openingPos);
    }
}
