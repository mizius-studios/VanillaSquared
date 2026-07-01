package blob.vanillasquared.util.api.combat;

import blob.vanillasquared.main.VanillaSquared;
import blob.vanillasquared.util.api.builder.components.HitThroughComponentBuilder;
import blob.vanillasquared.util.api.builder.durability.Durability;
import blob.vanillasquared.util.api.builder.general.ArmorAttributeBuilder;
import blob.vanillasquared.util.api.builder.general.WeaponAttributeBuilder;
import blob.vanillasquared.util.api.references.armor.Armor;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ToolMaterial;

import java.util.Map;

public final class VSQCombatPresets {
    private static final Map<ToolMaterial, WeaponAttributeBuilder> AXE_ATTRIBUTES = Map.of(
            ToolMaterial.WOOD, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.AXE, EquipmentSlotGroup.MAINHAND, 6.0D, -3.2D, -0.5D),
            ToolMaterial.STONE, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.AXE, EquipmentSlotGroup.MAINHAND, 7.0D, -3.2D, -0.5D),
            ToolMaterial.COPPER, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.AXE, EquipmentSlotGroup.MAINHAND, 7.0D, -3.2D, -0.5D),
            ToolMaterial.IRON, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.AXE, EquipmentSlotGroup.MAINHAND, 8.0D, -3.2D, -0.5D),
            ToolMaterial.GOLD, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.AXE, EquipmentSlotGroup.MAINHAND, 6.0D, -3.2D, 0.0D),
            ToolMaterial.DIAMOND, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.AXE, EquipmentSlotGroup.MAINHAND, 9.0D, -3.0D, -0.5D),
            ToolMaterial.NETHERITE, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.AXE, EquipmentSlotGroup.MAINHAND, 10.0D, -3.0D, -0.5D)
    );

    private static final Map<ToolMaterial, WeaponAttributeBuilder> SPEAR_ATTRIBUTES = Map.of(
            ToolMaterial.WOOD, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.SPEAR, EquipmentSlotGroup.MAINHAND, 1.0D, -1.1435, 0.0D),
            ToolMaterial.STONE, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.SPEAR, EquipmentSlotGroup.MAINHAND, 2.0D, -1.4455D, 0.0D),
            ToolMaterial.COPPER, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.SPEAR, EquipmentSlotGroup.MAINHAND, 2.0D, -1.453D, 0.0D),
            ToolMaterial.IRON, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.SPEAR, EquipmentSlotGroup.MAINHAND, 3.0D, -1.5325D, 0.0D),
            ToolMaterial.GOLD, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.SPEAR, EquipmentSlotGroup.MAINHAND, 1.0D, -1.535D, 0.0D),
            ToolMaterial.DIAMOND, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.SPEAR, EquipmentSlotGroup.MAINHAND, 4.0D, -1.55D, 0.0D),
            ToolMaterial.NETHERITE, new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.SPEAR, EquipmentSlotGroup.MAINHAND, 5.0D, -1.6D, 0.0D)
    );

    private static final Map<ToolMaterial, Durability> TOOL_DURABILITY = Map.of(
            ToolMaterial.WOOD, new Durability(75),
            ToolMaterial.STONE, new Durability(150),
            ToolMaterial.COPPER, new Durability(200),
            ToolMaterial.IRON, new Durability(250),
            ToolMaterial.GOLD, new Durability(100),
            ToolMaterial.DIAMOND, new Durability(1550),
            ToolMaterial.NETHERITE, new Durability(2069)
    );

    private static final Map<Armor, ArmorAttributeBuilder> ARMOR_ATTRIBUTES = Map.ofEntries(
            Map.entry(Armor.LEATHER_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.LEATHER_CHESTPLATE, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.CHESTPLATE, EquipmentSlotGroup.CHEST, 3.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.LEATHER_LEGGINGS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.LEGGINGS, EquipmentSlotGroup.LEGS, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.LEATHER_BOOTS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.BOOTS, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.COPPER_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.COPPER_CHESTPLATE, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.CHESTPLATE, EquipmentSlotGroup.CHEST, 4.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.COPPER_LEGGINGS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.LEGGINGS, EquipmentSlotGroup.LEGS, 3.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.COPPER_BOOTS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.BOOTS, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.CHAINMAIL_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 3.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.2D, 0.2D)),
            Map.entry(Armor.CHAINMAIL_CHESTPLATE, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.CHESTPLATE, EquipmentSlotGroup.CHEST, 5.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.2D, 0.2D)),
            Map.entry(Armor.CHAINMAIL_LEGGINGS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.LEGGINGS, EquipmentSlotGroup.LEGS, 4.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.2D, 0.2D)),
            Map.entry(Armor.CHAINMAIL_BOOTS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.BOOTS, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.2D, 0.2D)),
            Map.entry(Armor.IRON_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.IRON_CHESTPLATE, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.CHESTPLATE, EquipmentSlotGroup.CHEST, 6.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.IRON_LEGGINGS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.LEGGINGS, EquipmentSlotGroup.LEGS, 5.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.IRON_BOOTS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.BOOTS, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.GOLD_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 2.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),
            Map.entry(Armor.GOLD_CHESTPLATE, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.CHESTPLATE, EquipmentSlotGroup.CHEST, 6.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),
            Map.entry(Armor.GOLD_LEGGINGS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.LEGGINGS, EquipmentSlotGroup.LEGS, 5.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),
            Map.entry(Armor.GOLD_BOOTS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.BOOTS, EquipmentSlotGroup.FEET, 1.0D, 0.0D, 0.0D, 0.0D, 0.2D, 0.0D, 0.0D)),
            Map.entry(Armor.DIAMOND_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 4.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.DIAMOND_CHESTPLATE, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.CHESTPLATE, EquipmentSlotGroup.CHEST, 7.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.DIAMOND_LEGGINGS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.LEGGINGS, EquipmentSlotGroup.LEGS, 6.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.DIAMOND_BOOTS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.BOOTS, EquipmentSlotGroup.FEET, 3.0D, 2.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.NETHERITE_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 5.0D, 3.0D, 0.1D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.NETHERITE_CHESTPLATE, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.CHESTPLATE, EquipmentSlotGroup.CHEST, 7.0D, 3.0D, 0.1D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.NETHERITE_LEGGINGS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.LEGGINGS, EquipmentSlotGroup.LEGS, 6.0D, 3.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.NETHERITE_BOOTS, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.BOOTS, EquipmentSlotGroup.FEET, 4.0D, 3.0D, 0.1D, 0.0D, 0.0D, 0.0D, 0.0D)),
            Map.entry(Armor.TURTLE_HELMET, new ArmorAttributeBuilder(ArmorAttributeBuilder.ModifierIds.HELMET, EquipmentSlotGroup.HEAD, 4.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D))
    );

    private static final WeaponAttributeBuilder TRIDENT_ATTRIBUTES =
            new WeaponAttributeBuilder(WeaponAttributeBuilder.ModifierIds.TRIDENT, EquipmentSlotGroup.MAINHAND, 8.0D, -2.875D, 0.5D);

    private static final HitThroughComponentBuilder HIT_THROUGH_PLANTS =
            new HitThroughComponentBuilder(VanillaSquared.MOD_ID, "hit_through");

    private VSQCombatPresets() {
    }

    public static WeaponAttributeBuilder axeAttributes(ToolMaterial material) {
        return AXE_ATTRIBUTES.get(material);
    }

    public static WeaponAttributeBuilder spearAttributes(ToolMaterial material) {
        return SPEAR_ATTRIBUTES.get(material);
    }

    public static Durability toolDurability(ToolMaterial material) {
        return TOOL_DURABILITY.getOrDefault(material, Durability.DEFAULT);
    }

    public static ArmorAttributeBuilder armorAttributes(Armor armor) {
        return ARMOR_ATTRIBUTES.get(armor);
    }

    public static WeaponAttributeBuilder tridentAttributes() {
        return TRIDENT_ATTRIBUTES;
    }

    public static HitThroughComponentBuilder hitThroughPlants() {
        return HIT_THROUGH_PLANTS;
    }
}
