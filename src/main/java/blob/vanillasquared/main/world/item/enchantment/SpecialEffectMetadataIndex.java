package blob.vanillasquared.main.world.item.enchantment;

import blob.vanillasquared.main.VanillaSquared;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record SpecialEffectMetadataIndex(Map<String, List<SpecialEffectMetadata>> byComponent) {
    private static final Set<String> SUPPORTED_SPECIAL_KEYS = SpecialEffectSettings.MAP_CODEC.keys(JsonOps.INSTANCE)
            .map(JsonElement::getAsString)
            .collect(Collectors.toUnmodifiableSet());

    public static final SpecialEffectMetadataIndex EMPTY = new SpecialEffectMetadataIndex(Map.of());
    public static final Codec<SpecialEffectMetadataIndex> CODEC = Codec.unboundedMap(
            Codec.STRING,
            SpecialEffectMetadata.CODEC.listOf()
    ).xmap(SpecialEffectMetadataIndex::new, SpecialEffectMetadataIndex::byComponent);

    public Optional<SpecialEffectMetadata> metadata(String componentKey, int index) {
        List<SpecialEffectMetadata> entries = this.byComponent.get(componentKey);
        if (entries == null || index < 0 || index >= entries.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(index));
    }

    public List<SpecialEffectMetadata> all() {
        List<SpecialEffectMetadata> combined = new ArrayList<>();
        this.byComponent.values().forEach(combined::addAll);
        return List.copyOf(combined);
    }

    public static SpecialEffectMetadataIndex fromDynamic(Optional<Dynamic<?>> dynamic) {
        if (dynamic.isEmpty()) {
            return EMPTY;
        }

        JsonElement effectsJson = dynamic.get().convert(JsonOps.INSTANCE).getValue();
        if (!(effectsJson instanceof JsonObject effectsObject)) {
            return EMPTY;
        }

        Map<String, List<SpecialEffectMetadata>> byComponent = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : effectsObject.entrySet()) {
            String componentKey = entry.getKey().trim();
            if (componentKey.isEmpty()) {
                continue;
            }
            List<SpecialEffectMetadata> list = parseEntries(componentKey, entry.getValue());
            if (!list.isEmpty()) {
                byComponent.merge(componentKey, List.copyOf(list), (existing, added) -> {
                    ArrayList<SpecialEffectMetadata> merged = new ArrayList<>(existing.size() + added.size());
                    merged.addAll(existing);
                    merged.addAll(added);
                    return List.copyOf(merged);
                });
            }
        }
        return new SpecialEffectMetadataIndex(Collections.unmodifiableMap(byComponent));
    }

    private static List<SpecialEffectMetadata> parseEntries(String componentKey, JsonElement arrayElement) {
        if (!(arrayElement instanceof JsonArray entries)) {
            return List.of();
        }

        List<SpecialEffectMetadata> parsed = new ArrayList<>(entries.size());
        for (JsonElement entryElement : entries) {
            if (!(entryElement instanceof JsonObject entry)) {
                parsed.add(new SpecialEffectMetadata("", Optional.empty()));
                continue;
            }

            JsonElement idElement = entry.get("id");
            if (!(idElement instanceof JsonPrimitive primitive) || !primitive.isString()) {
                parsed.add(new SpecialEffectMetadata("", Optional.empty()));
                continue;
            }
            String id = primitive.getAsString().trim();
            if (id.isEmpty()) {
                parsed.add(new SpecialEffectMetadata("", Optional.empty()));
                continue;
            }
            Optional<SpecialEffectSettings> special = parseSpecial(componentKey, id, entry);
            parsed.add(new SpecialEffectMetadata(id, special));
        }
        return parsed.isEmpty() ? List.of() : List.copyOf(parsed);
    }

    private static Optional<SpecialEffectSettings> parseSpecial(String componentKey, String effectId, JsonObject entry) {
        if (!entry.has("special")) {
            return Optional.empty();
        }

        JsonElement json = entry.get("special");
        if (!(json instanceof JsonObject object)) {
            warnInvalidSpecial(componentKey, effectId, "expected object");
            return Optional.empty();
        }

        Set<String> unknownKeys = new HashSet<>(object.keySet());
        unknownKeys.removeAll(SUPPORTED_SPECIAL_KEYS);
        if (!unknownKeys.isEmpty()) {
            warnInvalidSpecial(componentKey, effectId,
                    "unsupported keys " + unknownKeys + "; supported keys: " + SUPPORTED_SPECIAL_KEYS);
            return Optional.empty();
        }

        return SpecialEffectSettings.CODEC.parse(JsonOps.INSTANCE, object).resultOrPartial(
                error -> warnInvalidSpecial(componentKey, effectId, error)
        );
    }

    private static void warnInvalidSpecial(String componentKey, String effectId, String reason) {
        VanillaSquared.LOGGER.warn(
                "Invalid special effect metadata for component={}, effect_id={}: {}",
                componentKey,
                effectId,
                reason
        );
    }
}
