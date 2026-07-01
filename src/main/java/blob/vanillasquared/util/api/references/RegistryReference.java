package blob.vanillasquared.util.api.references;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public record RegistryReference(Identifier id, boolean tag) {
    public static final Codec<RegistryReference> CODEC = Codec.STRING.comapFlatMap(
            RegistryReference::parse,
            RegistryReference::asString
    );

    public RegistryReference {
        if (id == null) {
            throw new IllegalArgumentException("Registry reference id cannot be null");
        }
    }

    public static RegistryReference value(Identifier id) {
        return new RegistryReference(id, false);
    }

    public static RegistryReference tag(Identifier id) {
        return new RegistryReference(id, true);
    }

    public static DataResult<RegistryReference> parse(String value) {
        if (value == null || value.isBlank()) {
            return DataResult.error(() -> "Registry reference cannot be empty");
        }

        boolean tag = value.startsWith("#");
        String idValue = tag ? value.substring(1) : value;
        if (idValue.isBlank()) {
            return DataResult.error(() -> "Registry reference cannot be empty");
        }

        Identifier id = Identifier.tryParse(idValue);
        if (id == null) {
            return DataResult.error(() -> "Invalid registry reference: " + value);
        }
        return DataResult.success(new RegistryReference(id, tag));
    }

    public static Codec<Identifier> tagIdCodec(String description) {
        return CODEC.comapFlatMap(
                reference -> {
                    if (!reference.tag()) {
                        return DataResult.error(() -> description + " must be a tag reference starting with #");
                    }
                    return DataResult.success(reference.id());
                },
                RegistryReference::tag
        );
    }

    public String asString() {
        return this.tag ? "#" + this.id : this.id.toString();
    }

    public <T> TagKey<T> tagKey(ResourceKey<? extends Registry<T>> registry) {
        if (!this.tag) {
            throw new IllegalStateException("Registry reference is not a tag: " + this.asString());
        }
        return TagKey.create(registry, this.id);
    }
}
