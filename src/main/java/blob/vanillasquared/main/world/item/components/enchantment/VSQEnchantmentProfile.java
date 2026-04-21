package blob.vanillasquared.main.world.item.components.enchantment;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult;
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
        public <T> DataResult<Pair<DataComponentMap, T>> decode(com.mojang.serialization.DynamicOps<T> ops, T input) {
            JsonElement rawJson = ops.convertTo(JsonOps.INSTANCE, input);
            RAW_EFFECTS.set(Optional.of(new Dynamic<>(JsonOps.INSTANCE, rawJson)));
            return EnchantmentEffectComponents.CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(DataComponentMap input, com.mojang.serialization.DynamicOps<T> ops, T prefix) {
            return EnchantmentEffectComponents.CODEC.encode(input, ops, prefix);
        }
    };

    private static final Codec<VSQEnchantmentProfile> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VSQEnchantmentProfileRequirement.CODEC.optionalFieldOf("requirement").forGetter(VSQEnchantmentProfile::requirement),
            VSQEnchantmentSlotType.CODEC.fieldOf("enchantment_slot").forGetter(VSQEnchantmentProfile::enchantmentSlot),
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set", HolderSet.empty()).forGetter(VSQEnchantmentProfile::exclusiveSet),
            Codec.intRange(1, Enchantment.MAX_LEVEL).fieldOf("max_level").forGetter(VSQEnchantmentProfile::maxLevel),
            EFFECTS_CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY).forGetter(VSQEnchantmentProfile::effects),
            SpecialEnchantmentProfileConfig.CODEC.optionalFieldOf("special").forGetter(VSQEnchantmentProfile::special),
            SpecialEffectMetadataIndex.CODEC.optionalFieldOf("special_effect_index").forGetter(profile -> Optional.of(profile.specialEffectIndex())),
            EquipmentSlotGroup.CODEC.listOf().fieldOf("slots").forGetter(VSQEnchantmentProfile::slots),
            Enchantment.Cost.CODEC.fieldOf("max_cost").forGetter(VSQEnchantmentProfile::maxCost),
            Enchantment.Cost.CODEC.fieldOf("min_cost").forGetter(VSQEnchantmentProfile::minCost)
    ).apply(instance, VSQEnchantmentProfile::create));

    public static final Codec<VSQEnchantmentProfile> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<VSQEnchantmentProfile, T>> decode(com.mojang.serialization.DynamicOps<T> ops, T input) {
            try {
                return BASE_CODEC.decode(ops, input);
            } finally {
                RAW_EFFECTS.remove();
            }
        }

        @Override
        public <T> DataResult<T> encode(VSQEnchantmentProfile input, com.mojang.serialization.DynamicOps<T> ops, T prefix) {
            return BASE_CODEC.encode(input, ops, prefix);
        }
    };

    private static VSQEnchantmentProfile create(
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
        Optional<Dynamic<?>> rawEffects = RAW_EFFECTS.get();
        RAW_EFFECTS.remove();
        return new VSQEnchantmentProfile(
                requirement,
                enchantmentSlot,
                exclusiveSet,
                maxLevel,
                effects,
                special,
                decodeSpecialEffectIndex(rawEffects, encodedSpecialEffectIndex),
                slots,
                maxCost,
                minCost
        );
    }

    private static SpecialEffectMetadataIndex decodeSpecialEffectIndex(
            Optional<Dynamic<?>> rawEffects,
            Optional<SpecialEffectMetadataIndex> encodedSpecialEffectIndex
    ) {
        return encodedSpecialEffectIndex.orElseGet(() -> SpecialEffectMetadataIndex.fromDynamic(rawEffects));
    }

    public boolean matches(ItemStack stack) {
        return this.requirement.map(requirement -> requirement.matches(stack)).orElse(true);
    }
}
