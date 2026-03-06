package blob.vanillasquared.mixin.world.item.components;

import blob.vanillasquared.util.api.modules.attributes.RegisterAttributes;
import blob.vanillasquared.util.api.builder.general.GeneralWeapon.UtilIdentifiers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(ItemAttributeModifiers.Display.Default.class)
public class ItemAttributeModifiersDisplayDefaultMixin {

    @Unique
    private Holder<Attribute> attributeHolder;
    @Unique
    private AttributeModifier modifier;
    @Unique
    private Player player_;

    @ModifyVariable(method = "apply", at = @At(value = "STORE"), ordinal = 0)
    private boolean modifyBl(boolean bl, Consumer<Component> consumer, @Nullable Player player, Holder<Attribute> holder, AttributeModifier attributeModifier) {
        modifier = attributeModifier;
        attributeHolder = holder;
        player_ = player;
        if (UtilIdentifiers.swordOverride.isItem(modifier, UtilIdentifiers.Item.SWORD)
            || UtilIdentifiers.axeOverride.isItem(modifier, UtilIdentifiers.Item.AXE)
            || UtilIdentifiers.tridentOverride.isItem(modifier, UtilIdentifiers.Item.TRIDENT)) {
            return !modifier.is(UtilIdentifiers.swordOverride.get(UtilIdentifiers.Type.ATTACK_RANGE))
                    && !modifier.is(UtilIdentifiers.axeOverride.get(UtilIdentifiers.Type.ATTACK_RANGE))
                    && !modifier.is(UtilIdentifiers.tridentOverride.get(UtilIdentifiers.Type.ATTACK_RANGE));
        } return bl;
    }

    @ModifyArg(method = "apply", at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;"), index = 0)
    private double vsq$modifyTooltipValue(double value) {
        double playerBaseAttackDMG = player_.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        double playerBaseAttackSpeed = player_.getAttributeBaseValue(Attributes.ATTACK_SPEED);

        if (attributeHolder.is(RegisterAttributes.UtilIdentifiers.maceProtectionAttribute.get())
                || attributeHolder.is(RegisterAttributes.UtilIdentifiers.magicProtectionAttribute.get())
                || attributeHolder.is(RegisterAttributes.UtilIdentifiers.dripstoneProtectionAttribute.get())
                || attributeHolder.is(RegisterAttributes.UtilIdentifiers.spearProtectionAttribute.get())) {
            return value * 10;
        } else if (modifier.is(UtilIdentifiers.swordOverride.get(UtilIdentifiers.Type.ATTACK_DMG))
                || modifier.is(UtilIdentifiers.axeOverride.get(UtilIdentifiers.Type.ATTACK_DMG))
                || modifier.is(UtilIdentifiers.tridentOverride.get(UtilIdentifiers.Type.ATTACK_DMG))) {
            return value + playerBaseAttackDMG;
        } else if (modifier.is(UtilIdentifiers.swordOverride.get(UtilIdentifiers.Type.ATTACK_SPEED))
                || modifier.is(UtilIdentifiers.axeOverride.get(UtilIdentifiers.Type.ATTACK_SPEED))
                || modifier.is(UtilIdentifiers.tridentOverride.get(UtilIdentifiers.Type.ATTACK_SPEED))) {
            return playerBaseAttackSpeed + value;
        } return value;
    }
}
