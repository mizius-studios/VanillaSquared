package blob.vanillasquared.main.world.item.enchantment;

import blob.vanillasquared.util.api.references.RegistryReference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record VSQEnchantmentProfileRequirement(Type type, Optional<Identifier> item, Optional<Identifier> tag) {
    public static final Codec<VSQEnchantmentProfileRequirement> CODEC = Raw.CODEC.flatXmap(
            Raw::decodeRequirement,
            requirement -> DataResult.success(Raw.encodeRequirement(requirement))
    );

    public enum Type {
        ITEM("item"),
        PROJECTILE_TAKEOVER("projectile_takeover");

        private final String serializedName;

        Type(String serializedName) {
            this.serializedName = serializedName;
        }

        public static DataResult<Type> decode(String value) {
            if (value == null) {
                return DataResult.error(() -> "Enchantment profile requirement type cannot be null");
            }

            return switch (value.trim()) {
                case "item" -> DataResult.success(ITEM);
                case "ITEM" -> DataResult.error(() -> "Use lowercase \"item\" (renamed from \"ITEM\" in 2.11.0)");
                case "projectile_takeover", "PROJECTILE_TAKEOVER" -> DataResult.success(PROJECTILE_TAKEOVER);
                default -> DataResult.error(() -> "Unknown enchantment profile requirement type: " + value);
            };
        }

        public String serializedName() {
            return this.serializedName;
        }
    }

    private record Raw(String type, Optional<RegistryReference> item) {
        private static final Codec<Raw> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Raw::type),
                RegistryReference.CODEC.optionalFieldOf("item").forGetter(Raw::item)
        ).apply(instance, Raw::new));

        private DataResult<VSQEnchantmentProfileRequirement> decodeRequirement() {
            return Type.decode(this.type).flatMap(requirementType -> {
                if (this.item.isEmpty()) {
                    return DataResult.error(() -> "Enchantment profile requirement must define an item reference");
                }

                RegistryReference reference = this.item.get();
                return new VSQEnchantmentProfileRequirement(
                        requirementType,
                        reference.tag() ? Optional.empty() : Optional.of(reference.id()),
                        reference.tag() ? Optional.of(reference.id()) : Optional.empty()
                ).validate();
            });
        }

        private static Raw encodeRequirement(VSQEnchantmentProfileRequirement requirement) {
            Optional<RegistryReference> itemReference = requirement.item
                    .map(RegistryReference::value)
                    .or(() -> requirement.tag.map(RegistryReference::tag));
            return new Raw(requirement.type.serializedName(), itemReference);
        }
    }

    public DataResult<VSQEnchantmentProfileRequirement> validate() {
        if (this.item.isPresent() == this.tag.isPresent()) {
            return DataResult.error(() -> "Enchantment profile requirement must define exactly one item reference");
        }
        return DataResult.success(this);
    }

    public boolean matches(ItemStack stack) {
        return this.type == Type.ITEM && this.matchesReference(stack);
    }

    public boolean matchesProjectileTakeover(ItemStack stack) {
        return this.type == Type.PROJECTILE_TAKEOVER && this.matchesReference(stack);
    }

    public boolean matches(ItemStack stack, @Nullable ItemStack projectileTakeoverStack) {
        return switch (this.type) {
            case ITEM -> this.matchesReference(stack);
            case PROJECTILE_TAKEOVER -> projectileTakeoverStack != null && this.matchesReference(projectileTakeoverStack);
        };
    }

    private boolean matchesReference(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return this.item.map(identifier -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(identifier)).orElseGet(() -> this.tag.map(identifier -> stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, identifier))).orElse(false));
    }
}
