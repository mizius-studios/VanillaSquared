package blob.vanillasquared.util.api.references;

public enum ArmorKeys {
    LEATHER_ARMOR("leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots"),
    COPPER_ARMOR("copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots"),
    CHAINMAIL_ARMOR("chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots"),
    IRON_ARMOR("iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"),
    GOLDEN_ARMOR("golden_helmet", "golden_chestplate", "golden_leggings", "golden_boots"),
    DIAMOND_ARMOR("diamond_helmet", "diamond_chestplate", "diamond_leggings", "diamond_boots"),
    NETHERITE_ARMOR("netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots"),
    TURTLE_ARMOR("turtle_helmet", null, null, null);

    private final String helmet;
    private final String chestplate;
    private final String leggings;
    private final String boots;

    ArmorKeys(String helmet, String chestplate, String leggings, String boots) {
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    public String helmet() {
        return helmet;
    }

    public String chestplate() {
        return chestplate;
    }

    public String leggings() {
        return leggings;
    }

    public String boots() {
        return boots;
    }
}
