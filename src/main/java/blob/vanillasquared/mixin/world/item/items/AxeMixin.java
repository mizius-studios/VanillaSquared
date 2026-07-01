package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.api.builder.general.WeaponAttributeBuilder;
import blob.vanillasquared.util.api.combat.VSQCombatPresets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.Properties.class)
public abstract class AxeMixin {

    @Shadow
    public abstract Item.Properties tool(ToolMaterial material, TagKey<Block> minesEfficiently, float attackDamageBaseline, float attackSpeedBaseline, float disableBlockingSeconds);

    @Inject(at = @At("HEAD"), method = "axe", cancellable = true)
    public void init(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, CallbackInfoReturnable<Item.Properties> cir) {
        WeaponAttributeBuilder generalWeapon = VSQCombatPresets.axeAttributes(material);
        if (generalWeapon == null) {
            return;
        }

        cir.setReturnValue(
                this.tool(material, BlockTags.MINEABLE_WITH_AXE, attackDamageBaseline, attackSpeedBaseline, 5.0F)
                        .component(DataComponents.WEAPON, new Weapon(1, 5.0F))
                        .attributes(generalWeapon.build())
        );
    }
}
