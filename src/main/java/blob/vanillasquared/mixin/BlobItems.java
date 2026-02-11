package blob.vanillasquared.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(Items.class)
public class BlobItems {

    @Inject(method = "registerItem(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/item/Item$Properties;)Lnet/minecraft/world/item/Item;", at = @At("HEAD"))
    private static void registerItem(String string, Function<Item.Properties, Item> function, Item.Properties properties, CallbackInfoReturnable<Item> cir) {
        switch (string) {
            case "fishing_rod": {
                properties.durability(250);
            }
        }
    }

    @Inject(method = "registerItem(Ljava/lang/String;Lnet/minecraft/world/item/Item$Properties;)Lnet/minecraft/world/item/Item;", at = @At("HEAD"))
    private static void registerItem(String string, Item.Properties properties, CallbackInfoReturnable<Item> cir) {
        switch (string) {
            case "netherite_chestplate": {
                // Keep durability tweak; do not replace vanilla armor attribute component.
                properties.durability(1);
            }
        }
    }
}
