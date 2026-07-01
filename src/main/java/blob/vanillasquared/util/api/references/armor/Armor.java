package blob.vanillasquared.util.api.references.armor;

import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Armor {
    LEATHER_HELMET("leather", ArmorType.HELMET),
    LEATHER_CHESTPLATE("leather", ArmorType.CHESTPLATE),
    LEATHER_LEGGINGS("leather", ArmorType.LEGGINGS),
    LEATHER_BOOTS("leather", ArmorType.BOOTS),

    CHAINMAIL_HELMET("chainmail", ArmorType.HELMET),
    CHAINMAIL_CHESTPLATE("chainmail", ArmorType.CHESTPLATE),
    CHAINMAIL_LEGGINGS("chainmail", ArmorType.LEGGINGS),
    CHAINMAIL_BOOTS("chainmail", ArmorType.BOOTS),

    IRON_HELMET("iron", ArmorType.HELMET),
    IRON_CHESTPLATE("iron", ArmorType.CHESTPLATE),
    IRON_LEGGINGS("iron", ArmorType.LEGGINGS),
    IRON_BOOTS("iron", ArmorType.BOOTS),

    GOLD_HELMET("gold", ArmorType.HELMET),
    GOLD_CHESTPLATE("gold", ArmorType.CHESTPLATE),
    GOLD_LEGGINGS("gold", ArmorType.LEGGINGS),
    GOLD_BOOTS("gold", ArmorType.BOOTS),

    DIAMOND_HELMET("diamond", ArmorType.HELMET),
    DIAMOND_CHESTPLATE("diamond", ArmorType.CHESTPLATE),
    DIAMOND_LEGGINGS("diamond", ArmorType.LEGGINGS),
    DIAMOND_BOOTS("diamond", ArmorType.BOOTS),

    NETHERITE_HELMET("netherite", ArmorType.HELMET),
    NETHERITE_CHESTPLATE("netherite", ArmorType.CHESTPLATE),
    NETHERITE_LEGGINGS("netherite", ArmorType.LEGGINGS),
    NETHERITE_BOOTS("netherite", ArmorType.BOOTS),

    COPPER_HELMET("copper", ArmorType.HELMET),
    COPPER_CHESTPLATE("copper", ArmorType.CHESTPLATE),
    COPPER_LEGGINGS("copper", ArmorType.LEGGINGS),
    COPPER_BOOTS("copper", ArmorType.BOOTS),

    TURTLE_HELMET("turtle_scute", ArmorType.HELMET);

    private static final Map<String, Map<ArmorType, Armor>> BY_ASSET_PATH_AND_TYPE = createLookup();

    private final String assetPath;
    private final ArmorType type;

    Armor(String assetPath, ArmorType type) {
        this.assetPath = assetPath;
        this.type = type;
    }

    public String assetPath() {
        return assetPath;
    }

    public ArmorType type() {
        return type;
    }

    public static Optional<Armor> find(ArmorMaterial material, ArmorType type) {
        String assetKey = material.assetId().toString();
        for (Map.Entry<String, Map<ArmorType, Armor>> entry : BY_ASSET_PATH_AND_TYPE.entrySet()) {
            if (!assetKeyMatches(assetKey, entry.getKey())) {
                continue;
            }
            return Optional.ofNullable(entry.getValue().get(type));
        }
        return Optional.empty();
    }

    public static Armor of(ArmorMaterial material, ArmorType type) {
        return find(material, type)
            .orElseThrow(() -> new IllegalArgumentException(
                "No Armor constant for material " + material + " and type " + type));
    }

    private static Map<String, Map<ArmorType, Armor>> createLookup() {
        Map<String, Map<ArmorType, Armor>> lookup = new HashMap<>();
        for (Armor armor : values()) {
            lookup.computeIfAbsent(armor.assetPath, ignored -> new HashMap<>())
                .put(armor.type, armor);
        }
        return lookup;
    }

    private static boolean assetKeyMatches(String fullAssetKey, String assetPath) {
        return fullAssetKey.endsWith(":" + assetPath)
            || fullAssetKey.endsWith("/" + assetPath + "]")
            || fullAssetKey.contains(":" + assetPath + "]");
    }

}
