package blob.vanillasquared.mixin.world.commands;

import blob.vanillasquared.main.world.recipe.enchanting.EnchantingRecipeRegistry;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Mixin(ResourceKeyArgument.class)
public abstract class ResourceKeyArgumentMixin {
    @Shadow @Final private ResourceKey<? extends Registry<?>> registryKey;

    @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true)
    private static void vsq$getCustomEnchantingRecipe(CommandContext<CommandSourceStack> context, String name, CallbackInfoReturnable<RecipeHolder<?>> cir) {
        Object argument = context.getArgument(name, ResourceKey.class);
        if (!(argument instanceof ResourceKey<?> rawKey)) {
            return;
        }

        rawKey.cast(Registries.RECIPE)
                .flatMap(EnchantingRecipeRegistry::byKey)
                .ifPresent(cir::setReturnValue);
    }

    @Inject(method = "listSuggestions", at = @At("HEAD"))
    private <S> void vsq$suggestCustomEnchantingRecipes(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
        if (!this.registryKey.equals(Registries.RECIPE)) {
            return;
        }

        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        EnchantingRecipeRegistry.recipeIds().forEach(id -> {
            String suggestion = id.toString();
            if (SharedSuggestionProvider.matchesSubStr(remaining, suggestion.toLowerCase(Locale.ROOT))) {
                builder.suggest(suggestion);
            }
        });
    }
}
