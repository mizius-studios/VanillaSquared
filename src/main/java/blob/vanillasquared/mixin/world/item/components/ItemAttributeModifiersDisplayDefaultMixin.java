package blob.vanillasquared.mixin.world.item.components;

import blob.vanillasquared.util.api.builder.general.WeaponAttributeBuilder.ModifierIds;
import blob.vanillasquared.util.api.builder.general.WeaponAttributeBuilder.ItemType;
import blob.vanillasquared.util.api.builder.general.WeaponAttributeBuilder.ModifierType;
import blob.vanillasquared.util.api.modules.attributes.VSQAttributes;
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

    @ModifyVariable(method = "apply", at = @At(value = "STORE"), name = "displayWithBase")
    private boolean modifyBl(boolean displayWithBase, Consumer<Component> consumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier) {
        this.modifier = modifier;
        attributeHolder = attribute;
        player_ = player;
        if (ModifierIds.SWORD.isItem(this.modifier, ItemType.SWORD)
            || ModifierIds.AXE.isItem(this.modifier, ItemType.AXE)
            || ModifierIds.TRIDENT.isItem(this.modifier, ItemType.TRIDENT)
            || ModifierIds.SPEAR.isItem(this.modifier, ItemType.SPEAR)) {
            return !this.modifier.is(ModifierIds.SWORD.get(ModifierType.ATTACK_RANGE))
                    && !this.modifier.is(ModifierIds.AXE.get(ModifierType.ATTACK_RANGE))
                    && !this.modifier.is(ModifierIds.TRIDENT.get(ModifierType.ATTACK_RANGE))
                    && !this.modifier.is(ModifierIds.SPEAR.get(ModifierType.ATTACK_RANGE));
        } return displayWithBase;
    }

    @ModifyArg(method = "apply", at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;"), index = 0)
    private double vsq$modifyTooltipValue(double value) {
        if (player_ != null) {
            double playerBaseAttackDMG = player_.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            double playerBaseAttackSpeed = player_.getAttributeBaseValue(Attributes.ATTACK_SPEED);

            if (attributeHolder.is(VSQAttributes.Keys.MACE_PROTECTION.id())
                    || attributeHolder.is(VSQAttributes.Keys.MAGIC_PROTECTION.id())
                    || attributeHolder.is(VSQAttributes.Keys.DRIPSTONE_PROTECTION.id())
                    || attributeHolder.is(VSQAttributes.Keys.SPEAR_PROTECTION.id())) {
                return value * 10;
            } else if (modifier.is(ModifierIds.SWORD.get(ModifierType.ATTACK_DAMAGE))
                    || modifier.is(ModifierIds.AXE.get(ModifierType.ATTACK_DAMAGE))
                    || modifier.is(ModifierIds.TRIDENT.get(ModifierType.ATTACK_DAMAGE))
                    || modifier.is(ModifierIds.SPEAR.get(ModifierType.ATTACK_DAMAGE))) {
                return value + playerBaseAttackDMG;
            } else if (modifier.is(ModifierIds.SWORD.get(ModifierType.ATTACK_SPEED))
                    || modifier.is(ModifierIds.AXE.get(ModifierType.ATTACK_SPEED))
                    || modifier.is(ModifierIds.TRIDENT.get(ModifierType.ATTACK_SPEED))
                    || modifier.is(ModifierIds.SPEAR.get(ModifierType.ATTACK_SPEED))) {
                return playerBaseAttackSpeed + value;
            }
        }
        return value;
    }
}
