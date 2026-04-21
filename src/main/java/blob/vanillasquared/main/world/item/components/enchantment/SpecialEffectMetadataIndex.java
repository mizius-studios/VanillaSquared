package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SpecialEffectMetadataIndex(Map<String, List<SpecialEffectMetadata>> byComponent) {
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
            List<SpecialEffectMetadata> list = parseEntries(entry.getValue());
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

    private static List<SpecialEffectMetadata> parseEntries(JsonElement arrayElement) {
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
            Optional<SpecialEffectSettings> special = parseSpecial(entry);
            parsed.add(new SpecialEffectMetadata(id, special));
        }
        return parsed.isEmpty() ? List.of() : List.copyOf(parsed);
    }

    private static Optional<SpecialEffectSettings> parseSpecial(JsonObject entry) {
        if (!entry.has("special")) {
            return Optional.empty();
        }

        JsonElement json = entry.get("special");
        if (!(json instanceof JsonObject object)) {
            return Optional.empty();
        }
        return SpecialEffectSettings.CODEC.parse(JsonOps.INSTANCE, object).result();
    }
}
