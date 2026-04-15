package blob.vanillasquared.main.world.item.components.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public enum VSQEnchantmentSlotType {
    SPECIAL,
    DAMAGE,
    SECONDARY,
    DEFENSE,
    UTIL,
    CURSE;

    public static final Codec<VSQEnchantmentSlotType> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(byName(name));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(exception::getMessage);
                }
            },
            VSQEnchantmentSlotType::serializedName
    );

    public static VSQEnchantmentSlotType byName(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "special" -> SPECIAL;
            case "damage" -> DAMAGE;
            case "secondary" -> SECONDARY;
            case "defense" -> DEFENSE;
            case "util" -> UTIL;
            case "curse" -> CURSE;
            default -> throw new IllegalArgumentException("Unknown enchantment slot type: " + name);
        };
    }

    public String serializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
