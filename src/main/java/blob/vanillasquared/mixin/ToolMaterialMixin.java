package blob.vanillasquared.mixin;

import blob.vanillasquared.util.data.GeneralWeapon;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Debug(export = true)
@Mixin(ToolMaterial.class)
public class ToolMaterialMixin {

    @Unique
    private static final Map<ToolMaterial, GeneralWeapon> SWORD = Map.of(
            ToolMaterial.WOOD, new GeneralWeapon(4.0d, -2.4d, 0.0d, 75),
            ToolMaterial.STONE, new GeneralWeapon(5.0d, -2.4d, 0.0d, 150),
            ToolMaterial.COPPER, new GeneralWeapon(5.0d, -2.4d, 0.0d, 200),
            ToolMaterial.IRON, new GeneralWeapon(6.0d, -2.4d, 0.0d, 250),
            ToolMaterial.GOLD, new GeneralWeapon(4.0d, -2.4d, 0.0d, 100),
            ToolMaterial.DIAMOND, new GeneralWeapon(7.0d, -2.4d, 0.0d, 1550),
            ToolMaterial.NETHERITE, new GeneralWeapon(8.0d, -2.4d, 0.0d, 2069)
    );

    @Inject(method = "applySwordProperties", at = @At("HEAD"), cancellable = true)
    private static void applySwordProperties(Item.Properties properties, float f, float g, CallbackInfoReturnable<Item.Properties> cir) {
        cir.setReturnValue(this.applyCommonProperties(properties).component(DataComponents.TOOL, new Tool(List.of(Tool.Rule.minesAndDrops(HolderSet.direct(new Holder[]{Blocks.COBWEB.builtInRegistryHolder()}), 15.0F), Tool.Rule.overrideSpeed(holderGetter.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE), Tool.Rule.overrideSpeed(holderGetter.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F)), 1.0F, 2, false)).attributes(this.createSwordAttributes(f, g)).component(DataComponents.WEAPON, new Weapon(1)));

    }
    @Inject(method = "createSwordAttributes", at = @At("HEAD"), cancellable = true)
    private static void createSwordAttributes(float f, float g, CallbackInfoReturnable<ItemAttributeModifiers> cir) {

    }
}