package blob.vanillasquared.main.world.recipe.enchanting;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public final class EnchantingRecipeValue {
    public static final Codec<LevelBasedValue> CODEC = LevelBasedValue.CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, LevelBasedValue> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(LevelBasedValue.CODEC);

    private EnchantingRecipeValue() {
    }

    public static LevelBasedValue constant(int value) {
        return LevelBasedValue.constant(value);
    }

    public static int itemCount(LevelBasedValue value, int level) {
        return Mth.clamp(floor(value, level), 1, Item.ABSOLUTE_MAX_STACK_SIZE);
    }

    public static int blockCount(LevelBasedValue value, int level) {
        return Math.max(floor(value, level), 1);
    }

    public static int requiredLevel(LevelBasedValue value, int level) {
        return Math.max(floor(value, level), 0);
    }

    private static int floor(LevelBasedValue value, int level) {
        return Mth.floor(value.calculate(level));
    }
}
