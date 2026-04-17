package blob.vanillasquared.mixin.world.inventory;

import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenu;
import blob.vanillasquared.main.world.inventory.VSQEnchantmentMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {
    private static final TagKey<Block> VSQ_ENCHANTMENT_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("vsq", "enchantment_blocks"));

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

    @Inject(method = "isValidBookShelf", at = @At("HEAD"), cancellable = true)
    private static void vsq$useCustomEnchantmentBlocks(Level level, BlockPos pos, BlockPos offset, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
                level.getBlockState(pos.offset(offset)).is(VSQ_ENCHANTMENT_BLOCKS)
                        && level.getBlockState(pos.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2)).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER)
        );
    }
}
