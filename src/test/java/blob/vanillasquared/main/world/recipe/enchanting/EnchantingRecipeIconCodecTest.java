package blob.vanillasquared.main.world.recipe.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantingRecipeIconCodecTest {
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        HolderLookup.Provider registries = VanillaRegistries.createLookup();
        registryOps = registries.createSerializationContext(JsonOps.INSTANCE);
    }

    @Test
    void fixedItemIdAndComponentsDecode() {
        EnchantingRecipeIcon icon = getOrThrow(EnchantingRecipeIcon.CODEC.parse(registryOps, JsonParser.parseString("""
                {
                  "id": "minecraft:enchanted_book",
                  "components": {
                    "minecraft:item_name": "Configured name",
                    "minecraft:lore": [
                      "First line",
                      "Second line"
                    ]
                  }
                }
                """)));

        assertEquals(Items.ENCHANTED_BOOK, icon.id().value());
        assertEquals("Configured name", icon.name().getString());
        assertEquals("First line\nSecond line", icon.description().getString());
        ItemLore lore = icon.components().get(DataComponentMap.EMPTY, DataComponents.LORE);
        assertEquals(2, lore.lines().size());
        assertNull(icon.components().get(DataComponentMap.EMPTY, DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        SlotDisplay.ItemStackSlotDisplay display = assertInstanceOf(SlotDisplay.ItemStackSlotDisplay.class, icon.display());
        assertEquals(icon.components(), display.stack().components());
    }

    @Test
    void componentsAreRequired() {
        assertFailed(EnchantingRecipeIcon.CODEC.parse(registryOps, JsonParser.parseString("""
                {
                  "id": "minecraft:enchanted_book"
                }
                """)));
    }

    @Test
    void itemTagsAreRejectedAsIconIds() {
        assertFailed(EnchantingRecipeIcon.CODEC.parse(registryOps, JsonParser.parseString("""
                {
                  "id": "#minecraft:bookshelf_books",
                  "components": {}
                }
                """)));
    }

    @Test
    void omittedGroupDefaultsToNoGroup() {
        EnchantingRecipe recipe = getOrThrow(decodeRecipe(minimalRecipeWith("""
                "icon": {
                  "id": "minecraft:enchanted_book",
                  "components": {}
                },
                """)));

        assertEquals("", recipe.group());
    }

    @Test
    void legacyNameAndDescriptionWithoutIconAreRejected() {
        assertFailed(decodeRecipe(minimalRecipeWith("""
                "name": "Sharpness",
                "description": "Legacy description",
                """)));
    }

    @Test
    void migratedBuiltInRecipeDecodes() throws IOException {
        try (var input = EnchantingRecipeIconCodecTest.class.getResourceAsStream("/data/vsq/recipe/sharpness.json")) {
            JsonElement json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            EnchantingRecipe recipe = getOrThrow(decodeRecipe(json.getAsJsonObject()));
            assertEquals("Sharpness", recipe.iconName().getString());
            assertFalse(recipe.iconDescription().getString().isBlank());
        }
    }

    private static JsonObject minimalRecipeWith(String fields) {
        return JsonParser.parseString("""
                {
                  "type": "vsq:enchanting",
                  "category": "weapons",
                  %s
                  "material": {
                    "item": "minecraft:lapis_lazuli",
                    "count": 1
                  },
                  "ingredients": [
                    { "item": "minecraft:amethyst_shard", "count": 1 },
                    { "item": "minecraft:quartz", "count": 1 },
                    { "item": "minecraft:echo_shard", "count": 1 },
                    { "item": "minecraft:diamond", "count": 1 }
                  ],
                  "level": 1,
                  "enchantment": "minecraft:sharpness"
                }
                """.formatted(fields)).getAsJsonObject();
    }

    private static DataResult<EnchantingRecipe> decodeRecipe(JsonObject json) {
        return EnchantingRecipe.CODEC.codec().parse(registryOps, json);
    }

    private static void assertFailed(DataResult<?> result) {
        assertTrue(result.isError());
        assertFalse(result.isSuccess());
    }

    private static <T> T getOrThrow(DataResult<T> result) {
        return result.getOrThrow(message -> new AssertionError(message));
    }
}
