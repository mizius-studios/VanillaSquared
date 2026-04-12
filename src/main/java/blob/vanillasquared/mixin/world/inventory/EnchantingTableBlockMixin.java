package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {
    @Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
    private void vsq$getCustomMenuProvider(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> cir) {
        Component title = level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity blockEntity
                ? blockEntity.getName()
                : Component.translatable("container.enchant");
        cir.setReturnValue(new VSQEnchantmentMenuProvider() {
            @Override
            public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new VSQEnchantmentMenu(containerId, inventory, pos);
            }
        });
    }
}
