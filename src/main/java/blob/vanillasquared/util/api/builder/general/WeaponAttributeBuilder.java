package blob.vanillasquared.util.api.builder.general;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class WeaponAttributeBuilder {
    private final ItemAttributeModifiers modifiers;

    public WeaponAttributeBuilder(ModifierIds ids, EquipmentSlotGroup slotGroup, double attackDamage, double attackSpeed, double attackRange) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (attackDamage != 0) {
            builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ids.get(ModifierType.ATTACK_DAMAGE), attackDamage, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (attackSpeed != 0) {
            builder.add(Attributes.ATTACK_SPEED, new AttributeModifier(ids.get(ModifierType.ATTACK_SPEED), attackSpeed, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        if (attackRange != 0) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ids.get(ModifierType.ATTACK_RANGE), attackRange, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
        this.modifiers = builder.build();
    }

    public ItemAttributeModifiers build() {
        return modifiers;
    }

    public enum ModifierIds {
        SWORD(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "sword_attack_damage"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "sword_attack_speed"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "sword_attack_range")
        ),
        AXE(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "axe_attack_damage"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "axe_attack_speed"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "axe_attack_range")
        ),
        TRIDENT(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "trident_attack_damage"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "trident_attack_speed"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "trident_attack_range")
        ),
        SPEAR(
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "spear_attack_damage"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "spear_attack_speed"),
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "spear_attack_range")
        );

        private final Identifier attackDamage;
        private final Identifier attackSpeed;
        private final Identifier attackRange;

        ModifierIds(Identifier attackDamage, Identifier attackSpeed, Identifier attackRange) {
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
            this.attackRange = attackRange;
        }

        public Identifier get(ModifierType type) {
            return switch (type) {
                case ATTACK_DAMAGE -> attackDamage;
                case ATTACK_SPEED -> attackSpeed;
                case ATTACK_RANGE -> attackRange;
            };
        }

        public boolean is(AttributeModifier modifier) {
            return modifier.is(attackDamage) || modifier.is(attackSpeed) || modifier.is(attackRange);
        }

        public boolean isItem(AttributeModifier modifier, ItemType item) {
            return switch (item) {
                case SWORD -> SWORD.is(modifier);
                case AXE -> AXE.is(modifier);
                case TRIDENT -> TRIDENT.is(modifier);
                case SPEAR -> SPEAR.is(modifier);
            };
        }
    }

    public enum ModifierType {
        ATTACK_DAMAGE,
        ATTACK_SPEED,
        ATTACK_RANGE
    }

    public enum ItemType {
        SWORD,
        AXE,
        TRIDENT,
        SPEAR
    }
}
