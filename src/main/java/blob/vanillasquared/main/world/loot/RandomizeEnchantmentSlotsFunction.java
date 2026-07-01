package blob.vanillasquared.main.world.loot;

import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class RandomizeEnchantmentSlotsFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomizeEnchantmentSlotsFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance).and(instance.group(
            Codec.intRange(0, 64).optionalFieldOf("min_capacity", 1).forGetter(function -> function.minCapacity),
            Codec.intRange(0, 64).optionalFieldOf("max_capacity", 4).forGetter(function -> function.maxCapacity)
    )).apply(instance, RandomizeEnchantmentSlotsFunction::new));

    public static final RandomizeEnchantmentSlotsFunction DEFAULT_LOOT_RANDOMIZATION = new RandomizeEnchantmentSlotsFunction(List.of(), 1, 4);

    private final int minCapacity;
    private final int maxCapacity;

    public RandomizeEnchantmentSlotsFunction(List<LootItemCondition> predicates, int minCapacity, int maxCapacity) {
        super(predicates);
        this.minCapacity = Math.min(minCapacity, maxCapacity);
        this.maxCapacity = Math.max(minCapacity, maxCapacity);
    }

    @Override
    public MapCodec<RandomizeEnchantmentSlotsFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        VSQEnchantments.randomizeSlotCapacities(itemStack, context.getRandom(), this.minCapacity, this.maxCapacity);
        return itemStack;
    }
}
