package blob.vanillasquared.util.api.builder.durability;

import blob.vanillasquared.util.api.references.armor.Armor;
import blob.vanillasquared.util.api.references.armor.ArmorKeys;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.Map;
import java.util.Optional;

public final class ArmorDurability {

    private static final Map<ArmorKeys, Durability> DURABILITY = Map.of(
            ArmorKeys.LEATHER_ARMOR, new Durability(196),
            ArmorKeys.COPPER_ARMOR, new Durability(267),
            ArmorKeys.CHAINMAIL_ARMOR, new Durability(400),
            ArmorKeys.IRON_ARMOR, new Durability(425),
            ArmorKeys.GOLDEN_ARMOR, new Durability(225),
            ArmorKeys.DIAMOND_ARMOR, new Durability(550),
            ArmorKeys.NETHERITE_ARMOR, new Durability(669),
            ArmorKeys.TURTLE_ARMOR, new Durability(669)
    );

    private ArmorDurability() {
    }

    public static Optional<Durability> findByMaterial(ArmorMaterial material) {
        return Armor.find(material, ArmorType.HELMET)
                .map(ArmorDurability::toArmorKey)
                .map(DURABILITY::get);
    }

    private static ArmorKeys toArmorKey(Armor armor) {
        return switch (armor) {
            case LEATHER_HELMET -> ArmorKeys.LEATHER_ARMOR;
            case COPPER_HELMET -> ArmorKeys.COPPER_ARMOR;
            case CHAINMAIL_HELMET -> ArmorKeys.CHAINMAIL_ARMOR;
            case IRON_HELMET -> ArmorKeys.IRON_ARMOR;
            case GOLD_HELMET -> ArmorKeys.GOLDEN_ARMOR;
            case DIAMOND_HELMET -> ArmorKeys.DIAMOND_ARMOR;
            case NETHERITE_HELMET -> ArmorKeys.NETHERITE_ARMOR;
            case TURTLE_HELMET -> ArmorKeys.TURTLE_ARMOR;
            default -> throw new IllegalArgumentException("Unsupported armor type for material lookup: " + armor);
        };
    }
}
