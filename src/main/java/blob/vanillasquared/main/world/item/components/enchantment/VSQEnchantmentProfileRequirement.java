package blob.vanillasquared.main.world.item.components.enchantment;

import blob.vanillasquared.util.api.references.RegistryReference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record VSQEnchantmentProfileRequirement(Optional<Identifier> item, Optional<Identifier> tag) {
    public static final Codec<VSQEnchantmentProfileRequirement> CODEC = Raw.CODEC.flatXmap(
            raw -> {
                if (!"ITEM".equals(raw.type())) {
                    return DataResult.error(() -> "Unknown enchantment profile requirement type: " + raw.type());
                }
                return raw.decodeRequirement();
            },
            requirement -> DataResult.success(Raw.encodeRequirement(requirement))
    );

    private record Raw(String type, Optional<RegistryReference> item) {
        private static final Codec<Raw> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Raw::type),
                RegistryReference.CODEC.optionalFieldOf("item").forGetter(Raw::item)
        ).apply(instance, Raw::new));

        private DataResult<VSQEnchantmentProfileRequirement> decodeRequirement() {
            if (this.item.isEmpty()) {
                return DataResult.error(() -> "Enchantment profile requirement must define an item reference");
            }

            RegistryReference reference = this.item.get();
            return new VSQEnchantmentProfileRequirement(
                    reference.tag() ? Optional.empty() : Optional.of(reference.id()),
                    reference.tag() ? Optional.of(reference.id()) : Optional.empty()
            ).validate();
        }

        private static Raw encodeRequirement(VSQEnchantmentProfileRequirement requirement) {
            Optional<RegistryReference> itemReference = requirement.item
                    .map(RegistryReference::value)
                    .or(() -> requirement.tag.map(RegistryReference::tag));
            return new Raw("ITEM", itemReference);
        }
    }

    public DataResult<VSQEnchantmentProfileRequirement> validate() {
        if (this.item.isPresent() == this.tag.isPresent()) {
            return DataResult.error(() -> "Enchantment profile requirement must define exactly one item reference");
        }
        return DataResult.success(this);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.item.isPresent()) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(this.item.get());
        }
        return this.tag.map(identifier -> stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, identifier))).orElse(false);
    }
}
