package blob.vanillasquared.main.world.recipe.enchanting;

import blob.vanillasquared.main.network.payload.EnchantingRecipeBookSyncPayload;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class EnchantingRecipeBookNotifier {
    private EnchantingRecipeBookNotifier() {
    }

    public static int unlock(ServerPlayer player, ResourceKey<Recipe<?>> recipeKey) {
        return EnchantingRecipeRegistry.byKey(recipeKey)
                .map(holder -> {
                    EnchantingRecipe recipe = holder.value();
                    player.getRecipeBook().add(recipeKey);
                    player.connection.send(new ClientboundRecipeBookAddPacket(
                            List.of(new ClientboundRecipeBookAddPacket.Entry(
                                    createDisplayEntry(recipeKey, recipe, player),
                                    true,
                                    true
                            )),
                            false
                    ));
                    return 1;
                })
                .orElse(0);
    }

    public static int revoke(ServerPlayer player, ResourceKey<Recipe<?>> recipeKey) {
        if (EnchantingRecipeRegistry.byKey(recipeKey).isEmpty()) {
            return 0;
        }

        player.getRecipeBook().remove(recipeKey);
        player.connection.send(new ClientboundRecipeBookRemovePacket(List.of(displayId(recipeKey))));
        return 1;
    }

    private static RecipeDisplayId displayId(ResourceKey<Recipe<?>> recipeKey) {
        return EnchantingRecipeRegistry.displayId(recipeKey)
                .stream()
                .mapToObj(RecipeDisplayId::new)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enchanting recipe: " + recipeKey.identifier()));
    }

    private static OptionalInt group(EnchantingRecipe recipe) {
        return recipe.group().isBlank()
                ? OptionalInt.empty()
                : EnchantingRecipeRegistry.groupId(recipe.group());
    }

    private static RecipeDisplayEntry createDisplayEntry(ResourceKey<Recipe<?>> recipeKey, EnchantingRecipe recipe, ServerPlayer player) {
        return new RecipeDisplayEntry(
                displayId(recipeKey),
                EnchantingRecipeBookSyncPayload.createDisplay(recipe, player.registryAccess()),
                group(recipe),
                RecipeBookCategories.CRAFTING_MISC,
                Optional.empty()
        );
    }
}
