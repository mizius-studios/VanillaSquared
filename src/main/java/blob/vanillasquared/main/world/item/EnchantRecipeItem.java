package blob.vanillasquared.main.world.item;

import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeTags;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeBookNotifier;
import blob.vanillasquared.util.api.modules.components.VSQItemComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

public class EnchantRecipeItem extends Item {
    public EnchantRecipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceKey<Recipe<?>> recipeKey = VSQItemComponents.getEnchantRecipe(stack);
        if (recipeKey == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!EnchantingRecipeTags.isValidRecipe(recipeKey)) {
            return InteractionResult.FAIL;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (serverPlayer.getRecipeBook().contains(recipeKey)) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("item.vsq.enchant_recipe.already_known").withStyle(ChatFormatting.RED)
            ));
            return InteractionResult.FAIL;
        }

        EnchantingRecipeBookNotifier.unlock(serverPlayer, recipeKey);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}
