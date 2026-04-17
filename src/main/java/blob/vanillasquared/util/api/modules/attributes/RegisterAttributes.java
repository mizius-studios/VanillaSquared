package blob.vanillasquared.util.api.modules.attributes;

import blob.vanillasquared.main.VanillaSquared;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class RegisterAttributes {

    public static final Holder<Attribute> maceProtectionAttribute = register(
            RegisterAttributes.UtilIdentifiers.maceProtectionAttribute.get(),
            new RangedAttribute(
                    "vsq.attributes.mace_protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );
    public static final Holder<Attribute> magicProtectionAttribute = register(
            RegisterAttributes.UtilIdentifiers.magicProtectionAttribute.get(),
            new RangedAttribute(
                    "Magic Protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );
    public static final Holder<Attribute> dripstoneProtectionAttribute = register(
            RegisterAttributes.UtilIdentifiers.dripstoneProtectionAttribute.get(),
            new RangedAttribute(
                    "Dripstone Protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );
    public static final Holder<Attribute> spearProtectionAttribute = register(
            RegisterAttributes.UtilIdentifiers.spearProtectionAttribute.get(),
            new RangedAttribute(
                    "Spear Protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );

    private static Holder<Attribute> register(Identifier identifier, Attribute attribute) {
        Registry.register(
                BuiltInRegistries.ATTRIBUTE,
                identifier,
                attribute
        );
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }
    public enum UtilIdentifiers {
        maceProtectionAttribute(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "mace_protection_attribute")),
        magicProtectionAttribute(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "magic_protection_attribute")),
        dripstoneProtectionAttribute(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "dripstone_protection_attribute")),
        spearProtectionAttribute(Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, "spear_protection_attribute"));

        private final Identifier identifier;
        UtilIdentifiers(Identifier identifier) { this.identifier = identifier; }
        public Identifier get() { return this.identifier; }
    }
    public static void initialize() {

    }
}
