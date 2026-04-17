package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
        List<EquipmentSlotGroup> slots,
        Enchantment.Cost maxCost,
        Enchantment.Cost minCost
) {
    public static final Codec<VSQEnchantmentProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VSQEnchantmentProfileRequirement.CODEC.optionalFieldOf("requirement").forGetter(VSQEnchantmentProfile::requirement),
            VSQEnchantmentSlotType.CODEC.fieldOf("enchantment_slot").forGetter(VSQEnchantmentProfile::enchantmentSlot),
            RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set", HolderSet.empty()).forGetter(VSQEnchantmentProfile::exclusiveSet),
            Codec.intRange(1, Enchantment.MAX_LEVEL).fieldOf("max_level").forGetter(VSQEnchantmentProfile::maxLevel),
            EnchantmentEffectComponents.CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY).forGetter(VSQEnchantmentProfile::effects),
            EquipmentSlotGroup.CODEC.listOf().fieldOf("slots").forGetter(VSQEnchantmentProfile::slots),
            Enchantment.Cost.CODEC.fieldOf("max_cost").forGetter(VSQEnchantmentProfile::maxCost),
            Enchantment.Cost.CODEC.fieldOf("min_cost").forGetter(VSQEnchantmentProfile::minCost)
    ).apply(instance, VSQEnchantmentProfile::new));

    public boolean matches(ItemStack stack) {
        return this.requirement.map(requirement -> requirement.matches(stack)).orElse(true);
    }
}
