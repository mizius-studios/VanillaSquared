package blob.vanillasquared.mixin.world.item;

import blob.vanillasquared.util.api.builder.durability.ArmorDurability;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.Properties.class)
public class ItemPropertiesMixin {

    @Inject(method = "humanoidArmor", at = @At("RETURN"))
    private void vsq$replaceArmorDurability(ArmorMaterial material, ArmorType type, CallbackInfoReturnable<Item.Properties> cir) {
        Item.Properties properties = cir.getReturnValue();
        ArmorDurability.findByMaterial(material).ifPresent(durability -> properties.durability(durability.durability()));
    }
}
