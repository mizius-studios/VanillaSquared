package blob.vanillasquared.util.modules.attributes;

import blob.vanillasquared.VanillaSquared;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class RegisterAttributes {

    public static final Holder<Attribute> maceProtectionAttribute = register(
            "mace_protection",
            new RangedAttribute(
                    "Mace Protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );
    public static final Holder<Attribute> magicProtectionAttribute = register(
            "magic_protection",
            new RangedAttribute(
                    "Magic Protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );
    public static final Holder<Attribute> dripstoneProtectionAttribute = register(
            "dripstone_protection",
            new RangedAttribute(
                    "Dripstone Protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );
    public static final Holder<Attribute> spearProtectionAttribute = register(
            "spear_protection",
            new RangedAttribute(
                    "Spear Protection",
                    0.0, 0.0, 1.0
            ).setSyncable(true)
    );

    private static Holder<Attribute> register(String name, Attribute attribute) {
        Registry.register(
                BuiltInRegistries.ATTRIBUTE,
                Identifier.fromNamespaceAndPath(VanillaSquared.MOD_ID, name),
                attribute
        );
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    public static void initialize() {

    }
}
