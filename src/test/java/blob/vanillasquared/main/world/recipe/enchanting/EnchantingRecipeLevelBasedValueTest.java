package blob.vanillasquared.main.world.recipe.enchanting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantingRecipeLevelBasedValueTest {
    private static HolderLookup.Provider registries;
    private static RegistryOps<JsonElement> registryOps;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
        registryOps = createSerializationContext(registries);
    }

    @Test
    void numericCountDecodesAsConstant() {
        EnchantingRecipe recipe = getOrThrow(decodeRecipe(recipeJson("""
                "count": 4
                """, """
                "level": 3
                """)));

        assertEquals(4, recipe.material().count(1));
        assertEquals(4, recipe.material().count(5));
    }

    @Test
    void levelBasedValueObjectDecodesAndResolvesPerLevel() {
        EnchantingRecipe recipe = getOrThrow(decodeRecipe(recipeJson("""
                "count": {
                  "type": "minecraft:linear",
                  "base": 2,
                  "per_level_above_first": 3
                }
                """, """
                "level": {
                  "type": "minecraft:linear",
                  "base": 5,
                  "per_level_above_first": 2
                }
                """)));

        assertEquals(2, recipe.material().count(1));
        assertEquals(5, recipe.material().count(2));
        assertEquals(7, EnchantingRecipeValue.requiredLevel(recipe.level(), 2));
    }

    @Test
    void missingLevelFailsDecoding() {
        DataResult<EnchantingRecipe> result = decodeRecipe(recipeJson("""
                "count": 1
                """, ""));

        assertFailed(result);
    }

    @Test
    void levelMultiplierIsRejected() {
        DataResult<EnchantingRecipe> result = decodeRecipe(recipeJson("""
                "count": 1
                """, """
                "level": 3,
                "level_multiplier": 1
                """));

        assertFailed(result);
        assertTrue(result.error().orElseThrow().message().contains("level_multiplier"));
    }

    @Test
    void materialAndCrossIngredientCountsResolveFromTargetNextLevel() {
        EnchantingRecipe recipe = getOrThrow(decodeRecipe(recipeJson("""
                "count": {
                  "type": "minecraft:linear",
                  "base": 1,
                  "per_level_above_first": 1
                }
                """, """
                "level": 3
                """)));
        assertEquals(1, recipe.material().count(1));
        assertEquals(2, recipe.material().count(2));
        assertEquals(1, recipe.ingredients().getFirst().count(1));
        assertEquals(2, recipe.ingredients().getFirst().count(2));
    }

    @Test
    void blockRequirementsResolveFromTargetNextLevelAndDisplayResolvedCount() {
        EnchantingRecipe recipe = getOrThrow(decodeRecipe(recipeJson("""
                "count": 1
                """, """
                "blocks": [
                  {
                    "block": "minecraft:bookshelf",
                    "count": {
                      "type": "minecraft:linear",
                      "base": 2,
                      "per_level_above_first": 2
                    }
                  }
                ],
                "level": 3
                """)));
        EnchantingBlockRequirement requirement = recipe.blocks().getFirst();

        assertFalse(requirement.matches(Map.of(Identifier.withDefaultNamespace("bookshelf"), 3), 2));
        assertTrue(requirement.matches(Map.of(Identifier.withDefaultNamespace("bookshelf"), 4), 2));
        assertEquals(4, requirement.count(2));
    }

    @Test
    void plainNumericCountsStayConstantAcrossLevels() {
        EnchantingRecipe recipe = getOrThrow(decodeRecipe(recipeJson("""
                "count": 3
                """, """
                "blocks": [
                  {
                    "block": "minecraft:bookshelf",
                    "count": 5
                  }
                ],
                "level": 3
                """)));

        assertEquals(3, recipe.material().count(1));
        assertEquals(3, recipe.material().count(2));
        assertEquals(5, recipe.blocks().getFirst().count(2));
    }

    private static DataResult<EnchantingRecipe> decodeRecipe(String json) {
        JsonObject recipeJson = JsonParser.parseString(json).getAsJsonObject();
        return EnchantingRecipe.CODEC.codec().parse(registryOps, recipeJson);
    }

    private static String recipeJson(String materialCountEntry, String extraFields) {
        String fields = extraFields.isBlank() ? "" : extraFields.strip() + ",";
        return """
                {
                  "type": "vsq:enchanting",
                  "category": "weapons",
                  "description": "Sharpness recipe description",
                  "icon": {
                    "id": "minecraft:enchanted_book",
                    "components": {
                      "minecraft:item_name": "Sharpness",
                      "minecraft:lore": [
                        "Sharpness test"
                      ]
                    }
                  },
                  "material": {
                    "item": "minecraft:lapis_lazuli",
                    %s
                  },
                  "ingredients": [
                    {
                      "item": "minecraft:amethyst_shard",
                      %s
                    },
                    {
                      "item": "minecraft:quartz",
                      %s
                    },
                    {
                      "item": "minecraft:echo_shard",
                      %s
                    },
                    {
                      "item": "minecraft:diamond",
                      %s
                    }
                  ],
                  %s
                  "enchantment": "minecraft:sharpness"
                }
                """.formatted(
                materialCountEntry.strip(),
                materialCountEntry.strip(),
                materialCountEntry.strip(),
                materialCountEntry.strip(),
                materialCountEntry.strip(),
                fields
        );
    }

    @SuppressWarnings("unchecked")
    private static RegistryOps<JsonElement> createSerializationContext(HolderLookup.Provider registries) {
        try {
            var method = registries.getClass().getMethod("createSerializationContext", com.mojang.serialization.DynamicOps.class);
            method.setAccessible(true);
            return (RegistryOps<JsonElement>) method.invoke(registries, JsonOps.INSTANCE);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create registry serialization context", exception);
        }
    }

    private static void assertFailed(DataResult<?> result) {
        assertTrue(result.isError());
        assertFalse(result.result().isPresent());
    }

    private static <T> T getOrThrow(DataResult<T> result) {
        return result.getOrThrow(message -> new AssertionError(message));
    }
}
