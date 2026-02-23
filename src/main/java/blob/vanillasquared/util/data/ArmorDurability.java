package blob.vanillasquared.util.data;

import blob.vanillasquared.util.api.references.ArmorKeys;
import blob.vanillasquared.util.api.references.Armor;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.Map;
import java.util.Optional;

public final class ArmorDurability {

    private static final Map<ArmorKeys, Dura> DURABILITY = Map.of(
            ArmorKeys.LEATHER_ARMOR, new Dura(96),
            ArmorKeys.COPPER_ARMOR, new Dura(167),
            ArmorKeys.CHAINMAIL_ARMOR, new Dura(300),
            ArmorKeys.IRON_ARMOR, new Dura(325),
            ArmorKeys.GOLDEN_ARMOR, new Dura(125),
            ArmorKeys.DIAMOND_ARMOR, new Dura(450),
            ArmorKeys.NETHERITE_ARMOR, new Dura(569),
            ArmorKeys.TURTLE_ARMOR, new Dura(569)
    );

    private ArmorDurability() {
    }

    public static Optional<Dura> findByMaterial(ArmorMaterial material) {
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
