package blob.vanillasquared.main.world.item.enchantment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VSQEnchantmentProfileCodecTest {
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
    void profileDecodeFailsWhenEffectIdIsMissing() {
        DataResult<VSQEnchantmentProfile> result = decodeProfile("""
                {
                  "enchantment_slot": "damage",
                  "max_level": 1,
                  "effects": {
                    "minecraft:damage": [
                      {
                        "effect": {
                          "type": "minecraft:multiply",
                          "factor": {
                            "type": "minecraft:linear",
                            "base": 1.25,
                            "per_level_above_first": 0
                          }
                        }
                      }
                    ]
                  },
                  "slots": [
                    "mainhand"
                  ],
                  "max_cost": {
                    "base": 25,
                    "per_level_above_first": 8
                  },
                  "min_cost": {
                    "base": 5,
                    "per_level_above_first": 8
                  }
                }
                """);

        assertFailedWithMessage(result, "component=minecraft:damage, index=0: missing effect_id");
    }

    @Test
    void profileDecodeFailsWhenEffectIdIsBlank() {
        DataResult<VSQEnchantmentProfile> result = decodeProfile(profileJson("""
                {
                  "effect": {
                    "type": "minecraft:multiply",
                    "factor": {
                      "type": "minecraft:linear",
                      "base": 1.25,
                      "per_level_above_first": 0
                    }
                  },
                  "effect_id": "   "
                }
                """));

        assertFailedWithMessage(result, "component=minecraft:damage, index=0: blank effect_id");
    }

    @Test
    void profileDecodeFailsWhenEffectIdIsNotAString() {
        DataResult<VSQEnchantmentProfile> result = decodeProfile(profileJson("""
                {
                  "effect": {
                    "type": "minecraft:multiply",
                    "factor": {
                      "type": "minecraft:linear",
                      "base": 1.25,
                      "per_level_above_first": 0
                    }
                  },
                  "effect_id": 12
                }
                """));

        assertFailedWithMessage(result, "component=minecraft:damage, index=0: non-string effect_id");
    }

    @Test
    void profileDecodeSucceedsWhenEffectIdIsValid() {
        VSQEnchantmentProfile profile = getOrThrow(decodeProfile(profileJson("""
                {
                  "effect": {
                    "type": "minecraft:multiply",
                    "factor": {
                      "type": "minecraft:linear",
                      "base": 1.25,
                      "per_level_above_first": 0
                    }
                  },
                  "effect_id": "vsq_ruthless_damage"
                }
                """)));

        assertTrue(profile.specialEffectIndex().metadata("minecraft:damage", 0).isPresent());
        assertEquals(
                "vsq_ruthless_damage",
                profile.specialEffectIndex().metadata("minecraft:damage", 0).orElseThrow().effectId()
        );
    }

    private static DataResult<VSQEnchantmentProfile> decodeProfile(String json) {
        JsonObject profileJson = JsonParser.parseString(json).getAsJsonObject();
        return VSQEnchantmentProfile.CODEC.parse(registryOps, profileJson);
    }

    private static String profileJson(String effectEntryJson) {
        return """
                {
                  "enchantment_slot": "damage",
                  "max_level": 1,
                  "effects": {
                    "minecraft:damage": [
                      %s
                    ]
                  },
                  "slots": [
                    "mainhand"
                  ],
                  "max_cost": {
                    "base": 25,
                    "per_level_above_first": 8
                  },
                  "min_cost": {
                    "base": 5,
                    "per_level_above_first": 8
                  }
                }
                """.formatted(effectEntryJson.strip());
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

    private static void assertFailedWithMessage(DataResult<?> result, String messageFragment) {
        assertTrue(result.isError());
        String message = result.error().orElseThrow().message();
        assertTrue(message.contains(messageFragment), message);
        assertFalse(result.result().isPresent());
    }

    private static <T> T getOrThrow(DataResult<T> result) {
        return result.getOrThrow(message -> new AssertionError(message));
    }
}
