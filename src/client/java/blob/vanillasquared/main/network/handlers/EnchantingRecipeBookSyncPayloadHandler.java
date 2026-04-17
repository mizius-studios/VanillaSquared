package blob.vanillasquared.main.network.handlers;

import blob.vanillasquared.main.network.payload.EnchantingRecipeBookSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class EnchantingRecipeBookSyncPayloadHandler {
    private static final Map<Integer, List<RecipeDisplayId>> CONTAINER_DISPLAY_IDS = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<RecipeDisplay, Integer>> CONTAINER_DISPLAY_LOOKUP = new ConcurrentHashMap<>();

    private EnchantingRecipeBookSyncPayloadHandler() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EnchantingRecipeBookSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> apply(context.client(), payload))
        );
    }

    public static void clearContainer(Minecraft minecraft, int containerId) {
        if (minecraft.player == null) {
            return;
        }
        ClientRecipeBook recipeBook = minecraft.player.getRecipeBook();
        List<RecipeDisplayId> existing = CONTAINER_DISPLAY_IDS.remove(containerId);
        CONTAINER_DISPLAY_LOOKUP.remove(containerId);
        if (existing == null || existing.isEmpty()) {
            return;
        }
        for (RecipeDisplayId displayId : existing) {
            recipeBook.remove(displayId);
        }
        recipeBook.rebuildCollections();
    }

    public static OptionalInt findDisplayId(int containerId, RecipeDisplay display) {
        Map<RecipeDisplay, Integer> lookup = CONTAINER_DISPLAY_LOOKUP.get(containerId);
        if (lookup == null) {
            return OptionalInt.empty();
        }
        Integer displayId = lookup.get(display);
        return displayId == null ? OptionalInt.empty() : OptionalInt.of(displayId);
    }

    private static void apply(Minecraft minecraft, EnchantingRecipeBookSyncPayload payload) {
        if (minecraft.player == null) {
            return;
        }

        ClientRecipeBook recipeBook = minecraft.player.getRecipeBook();
        if (payload.replace()) {
            clearContainer(minecraft, payload.containerId());
        }

        List<RecipeDisplayId> displayIds = new ArrayList<>(payload.entries().size());
        Map<RecipeDisplay, Integer> displayLookup = new ConcurrentHashMap<>();
        for (EnchantingRecipeBookSyncPayload.Entry entry : payload.entries()) {
            var displayEntry = entry.toDisplayEntry();
            recipeBook.add(displayEntry);
            displayIds.add(displayEntry.id());
            displayLookup.put(entry.display(), entry.displayId());
        }
        CONTAINER_DISPLAY_IDS.put(payload.containerId(), List.copyOf(displayIds));
        CONTAINER_DISPLAY_LOOKUP.put(payload.containerId(), Map.copyOf(displayLookup));
        recipeBook.rebuildCollections();
        if (minecraft.screen instanceof RecipeUpdateListener recipeUpdateListener
                && minecraft.player.containerMenu.containerId == payload.containerId()) {
            recipeUpdateListener.recipesUpdated();
        }
    }
}
