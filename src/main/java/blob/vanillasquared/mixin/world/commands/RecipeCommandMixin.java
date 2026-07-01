package blob.vanillasquared.mixin.world.commands;

import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeBookNotifier;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.commands.RecipeCommand;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import com.mojang.brigadier.CommandDispatcher;
import java.util.List;

@Mixin(RecipeCommand.class)
public abstract class RecipeCommandMixin {
    @Shadow @Final private static SimpleCommandExceptionType ERROR_GIVE_FAILED;
    @Shadow @Final private static SimpleCommandExceptionType ERROR_TAKE_FAILED;
    @Shadow private static int giveRecipes(CommandSourceStack source, Collection<ServerPlayer> players, Collection<RecipeHolder<?>> recipes) throws CommandSyntaxException { throw new AssertionError(); }
    @Shadow private static int takeRecipes(CommandSourceStack source, Collection<ServerPlayer> players, Collection<RecipeHolder<?>> recipes) throws CommandSyntaxException { throw new AssertionError(); }

    @Inject(method = "register", at = @At("TAIL"))
    private static void vsq$extendWildcardRecipeTargets(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        vsq$replaceWildcardExecutor(dispatcher, "give", context -> giveRecipes(
                context.getSource(),
                EntityArgument.getPlayers(context, "targets"),
                vsq$allRecipes(context.getSource())
        ));
        vsq$replaceWildcardExecutor(dispatcher, "take", context -> takeRecipes(
                context.getSource(),
                EntityArgument.getPlayers(context, "targets"),
                vsq$allRecipes(context.getSource())
        ));
    }

    @Unique
    private static Collection<RecipeHolder<?>> vsq$allRecipes(CommandSourceStack source) {
        List<RecipeHolder<?>> recipes = new ArrayList<>(source.getServer().getRecipeManager().getRecipes());
        recipes.addAll(EnchantingRecipeRegistry.recipeHolders());
        return List.copyOf(recipes);
    }

    @Unique
    private static void vsq$replaceWildcardExecutor(CommandDispatcher<CommandSourceStack> dispatcher, String action, Command<CommandSourceStack> command) {
        CommandNode<CommandSourceStack> recipeNode = dispatcher.getRoot().getChild("recipe");
        if (recipeNode == null) {
            return;
        }
        CommandNode<CommandSourceStack> actionNode = recipeNode.getChild(action);
        if (actionNode == null) {
            return;
        }
        CommandNode<CommandSourceStack> targetsNode = actionNode.getChild("targets");
        if (targetsNode == null) {
            return;
        }
        CommandNode<CommandSourceStack> wildcardNode = targetsNode.getChild("*");
        if (wildcardNode == null) {
            return;
        }
        ((CommandNodeAccessor<CommandSourceStack>) wildcardNode).vsq$setCommand(command);
    }

    @Inject(method = "giveRecipes", at = @At("HEAD"), cancellable = true)
    private static void vsq$giveCustomEnchantingRecipes(CommandSourceStack source, Collection<ServerPlayer> players, Collection<RecipeHolder<?>> recipes, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        if (recipes.stream().noneMatch(holder -> EnchantingRecipeRegistry.contains(holder.id()))) {
            return;
        }

        int changed = 0;
        List<RecipeHolder<?>> vanillaRecipes = recipes.stream()
                .filter(holder -> !EnchantingRecipeRegistry.contains(holder.id()))
                .toList();
        for (ServerPlayer player : players) {
            changed += player.awardRecipes(vanillaRecipes);
            for (RecipeHolder<?> recipe : recipes) {
                if (EnchantingRecipeRegistry.contains(recipe.id()) && !player.getRecipeBook().contains(recipe.id())) {
                    changed += EnchantingRecipeBookNotifier.unlock(player, recipe.id());
                }
            }
        }
        if (changed == 0) {
            throw ERROR_GIVE_FAILED.create();
        }
        int recipeCount = changed;
        int targetCount = players.size();
        source.sendSuccess(() -> Component.translatable(
                targetCount == 1 ? "commands.recipe.give.success.single" : "commands.recipe.give.success.multiple",
                recipeCount,
                targetCount == 1 ? players.iterator().next().getDisplayName() : targetCount
        ), true);
        cir.setReturnValue(changed);
    }

    @Inject(method = "takeRecipes", at = @At("HEAD"), cancellable = true)
    private static void vsq$takeCustomEnchantingRecipes(CommandSourceStack source, Collection<ServerPlayer> players, Collection<RecipeHolder<?>> recipes, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        if (recipes.stream().noneMatch(holder -> EnchantingRecipeRegistry.contains(holder.id()))) {
            return;
        }

        int changed = 0;
        List<RecipeHolder<?>> vanillaRecipes = recipes.stream()
                .filter(holder -> !EnchantingRecipeRegistry.contains(holder.id()))
                .toList();
        for (ServerPlayer player : players) {
            changed += player.resetRecipes(vanillaRecipes);
            for (RecipeHolder<?> recipe : recipes) {
                if (EnchantingRecipeRegistry.contains(recipe.id()) && player.getRecipeBook().contains(recipe.id())) {
                    changed += EnchantingRecipeBookNotifier.revoke(player, recipe.id());
                }
            }
        }
        if (changed == 0) {
            throw ERROR_TAKE_FAILED.create();
        }
        int recipeCount = changed;
        int targetCount = players.size();
        source.sendSuccess(() -> Component.translatable(
                targetCount == 1 ? "commands.recipe.take.success.single" : "commands.recipe.take.success.multiple",
                recipeCount,
                targetCount == 1 ? players.iterator().next().getDisplayName() : targetCount
        ), true);
        cir.setReturnValue(changed);
    }
}
