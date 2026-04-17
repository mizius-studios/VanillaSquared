package blob.vanillasquared.util.api.builder.general;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class GeneralWeapon {
    private final ItemAttributeModifiers modifiers;

    public GeneralWeapon(UtilIdentifiers identifier, EquipmentSlotGroup slotGroup, double attackDamage, double attackSpeed, double attackRange) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (attackDamage != 0) {
            builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(identifier.get(UtilIdentifiers.Type.ATTACK_DMG), attackDamage, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (attackSpeed != 0) {
            builder.add(Attributes.ATTACK_SPEED, new AttributeModifier(identifier.get(UtilIdentifiers.Type.ATTACK_SPEED), attackSpeed, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (attackRange != 0) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(identifier.get(UtilIdentifiers.Type.ATTACK_RANGE), attackRange, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }

        this.modifiers = builder.build();
    }
    public enum UtilIdentifiers {
        swordOverride(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "sword_attack_damage"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "sword_attack_speed"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "sword_attack_range")),
        axeOverride(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "axe_attack_damage"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "axe_attack_speed"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "axe_attack_range")),
        tridentOverride(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "trident_attack_damage"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "trident_attack_speed"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "trident_attack_range"));

        private final Identifier attackDMG;
        private final Identifier attackSpeed;
        private final Identifier attackRange;
        UtilIdentifiers(Identifier attackDMG, Identifier attackSpeed, Identifier attackRange) {
            this.attackDMG = attackDMG;
            this.attackSpeed = attackSpeed;
            this.attackRange = attackRange;
        }
        public Identifier get(Type type) {
            return switch (type) {
                case Type.ATTACK_DMG -> attackDMG;
                case Type.ATTACK_SPEED -> attackSpeed;
                case Type.ATTACK_RANGE -> attackRange;
            };
        }
        public boolean is(AttributeModifier modifier) {
            return modifier.is(attackDMG) || modifier.is(attackSpeed) || modifier.is(attackRange);
        }

        public boolean isItem(AttributeModifier modifier, Item item) {
            return switch (item) {
                case Item.SWORD -> UtilIdentifiers.swordOverride.is(modifier);
                case Item.AXE -> UtilIdentifiers.axeOverride.is(modifier);
                case Item.TRIDENT -> UtilIdentifiers.tridentOverride.is(modifier);
            };
        }

        public enum Type {
            ATTACK_DMG,
            ATTACK_SPEED,
            ATTACK_RANGE
        }
        public enum Item {
            SWORD,
            AXE,
            TRIDENT
        }
    }

    public ItemAttributeModifiers build() {
        return modifiers;
    }
}
