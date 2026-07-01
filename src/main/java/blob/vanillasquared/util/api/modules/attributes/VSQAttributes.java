package blob.vanillasquared.util.api.modules.attributes;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class VSQAttributes {
    public static final Holder<Attribute> MACE_PROTECTION = register(
            Keys.MACE_PROTECTION.id(),
            new RangedAttribute("vsq.attributes.mace_protection", 0.0, 0.0, 1.0).setSyncable(true)
    );
    public static final Holder<Attribute> MAGIC_PROTECTION = register(
            Keys.MAGIC_PROTECTION.id(),
            new RangedAttribute("Magic Protection", 0.0, 0.0, 1.0).setSyncable(true)
    );
    public static final Holder<Attribute> DRIPSTONE_PROTECTION = register(
            Keys.DRIPSTONE_PROTECTION.id(),
            new RangedAttribute("Dripstone Protection", 0.0, 0.0, 1.0).setSyncable(true)
    );
    public static final Holder<Attribute> SPEAR_PROTECTION = register(
            Keys.SPEAR_PROTECTION.id(),
            new RangedAttribute("Spear Protection", 0.0, 0.0, 1.0).setSyncable(true)
    );

    private VSQAttributes() {
    }

    public static void initialize() {
    }

    private static Holder<Attribute> register(Identifier id, Attribute attribute) {
        Registry.register(BuiltInRegistries.ATTRIBUTE, id, attribute);
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    public enum Keys {
        MACE_PROTECTION(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "mace_protection_attribute")),
        MAGIC_PROTECTION(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "magic_protection_attribute")),
        DRIPSTONE_PROTECTION(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "dripstone_protection_attribute")),
        SPEAR_PROTECTION(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "spear_protection_attribute"));

        private final Identifier id;

        Keys(Identifier id) {
            this.id = id;
        }

        public Identifier id() {
            return this.id;
        }
    }
}
