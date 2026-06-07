package blob.vanillasquared.main.world.item.enchantment;

import com.google.gson.JsonElement;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record VSQEnchantmentProfile(
        Optional<VSQEnchantmentProfileRequirement> requirement,
        VSQEnchantmentSlotType enchantmentSlot,
        HolderSet<Enchantment> exclusiveSet,
        int maxLevel,
        DataComponentMap effects,
        Optional<SpecialEnchantmentProfileConfig> special,
        SpecialEffectMetadataIndex specialEffectIndex,
        List<EquipmentSlotGroup> slots,
        Enchantment.Cost maxCost,
        Enchantment.Cost minCost
) {
    private static final ThreadLocal<Optional<Dynamic<?>>> RAW_EFFECTS = ThreadLocal.withInitial(Optional::empty);

    private static final Codec<DataComponentMap> EFFECTS_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<DataComponentMap, T>> decode(DynamicOps<T> ops, T input) {
            JsonElement rawJson = ops.convertTo(JsonOps.INSTANCE, input);
            RAW_EFFECTS.set(Optional.of(new Dynamic<>(JsonOps.INSTANCE, rawJson)));
            return EnchantmentEffectComponents.CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(DataComponentMap input, DynamicOps<T> ops, T prefix) {
            return EnchantmentEffectComponents.CODEC.encode(input, ops, prefix);
        }
    };

    public static final Codec<VSQEnchantmentProfile> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<VSQEnchantmentProfile, T>> decode(DynamicOps<T> ops, T input) {
            try {
                return Raw.CODEC.decode(ops, input).flatMap(pair ->
                        pair.getFirst().decode().map(profile -> Pair.of(profile, pair.getSecond()))
                );
            } finally {
                RAW_EFFECTS.remove();
            }
        }

        @Override
        public <T> DataResult<T> encode(VSQEnchantmentProfile input, DynamicOps<T> ops, T prefix) {
            return Raw.CODEC.encode(Raw.encode(input), ops, prefix);
        }
    };

    private static DataResult<SpecialEffectMetadataIndex> decodeSpecialEffectIndex(
            Optional<Dynamic<?>> rawEffects,
            Optional<SpecialEffectMetadataIndex> encodedSpecialEffectIndex
    ) {
        return encodedSpecialEffectIndex
                .map(DataResult::success)
                .orElseGet(() -> SpecialEffectMetadataIndex.fromDynamic(rawEffects));
    }

    private record Raw(
            Optional<VSQEnchantmentProfileRequirement> requirement,
            VSQEnchantmentSlotType enchantmentSlot,
            HolderSet<Enchantment> exclusiveSet,
            int maxLevel,
            DataComponentMap effects,
            Optional<SpecialEnchantmentProfileConfig> special,
            Optional<SpecialEffectMetadataIndex> encodedSpecialEffectIndex,
            List<EquipmentSlotGroup> slots,
            Enchantment.Cost maxCost,
            Enchantment.Cost minCost
    ) {
        private static final Codec<Raw> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                VSQEnchantmentProfileRequirement.CODEC.optionalFieldOf("requirement").forGetter(Raw::requirement),
                VSQEnchantmentSlotType.CODEC.fieldOf("enchantment_slot").forGetter(Raw::enchantmentSlot),
                RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set", HolderSet.empty()).forGetter(Raw::exclusiveSet),
                Codec.intRange(1, Enchantment.MAX_LEVEL).fieldOf("max_level").forGetter(Raw::maxLevel),
                EFFECTS_CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY).forGetter(Raw::effects),
                SpecialEnchantmentProfileConfig.CODEC.optionalFieldOf("special").forGetter(Raw::special),
                SpecialEffectMetadataIndex.CODEC.optionalFieldOf("special_effect_index").forGetter(Raw::encodedSpecialEffectIndex),
                EquipmentSlotGroup.CODEC.listOf().fieldOf("slots").forGetter(Raw::slots),
                Enchantment.Cost.CODEC.fieldOf("max_cost").forGetter(Raw::maxCost),
                Enchantment.Cost.CODEC.fieldOf("min_cost").forGetter(Raw::minCost)
        ).apply(instance, Raw::new));

        private DataResult<VSQEnchantmentProfile> decode() {
            Optional<Dynamic<?>> rawEffects = RAW_EFFECTS.get();
            RAW_EFFECTS.remove();
            return decodeSpecialEffectIndex(rawEffects, this.encodedSpecialEffectIndex).map(specialEffectIndex ->
                    new VSQEnchantmentProfile(
                            this.requirement,
                            this.enchantmentSlot,
                            this.exclusiveSet,
                            this.maxLevel,
                            this.effects,
                            this.special,
                            specialEffectIndex,
                            this.slots,
                            this.maxCost,
                            this.minCost
                    )
            );
        }

        private static Raw encode(VSQEnchantmentProfile profile) {
            return new Raw(
                    profile.requirement(),
                    profile.enchantmentSlot(),
                    profile.exclusiveSet(),
                    profile.maxLevel(),
                    profile.effects(),
                    profile.special(),
                    Optional.of(profile.specialEffectIndex()),
                    profile.slots(),
                    profile.maxCost(),
                    profile.minCost()
            );
        }
    }

    public boolean matches(ItemStack stack) {
        return this.requirement.map(requirement -> requirement.matches(stack)).orElse(true);
    }

    public boolean matchesProjectileTakeover(ItemStack stack) {
        return this.requirement.map(requirement -> requirement.matchesProjectileTakeover(stack)).orElse(false);
    }

    public boolean matches(ItemStack stack, @Nullable ItemStack projectileTakeoverStack) {
        return this.requirement.map(requirement -> requirement.matches(stack, projectileTakeoverStack)).orElse(true);
    }
}
