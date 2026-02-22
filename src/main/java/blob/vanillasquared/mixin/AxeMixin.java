package blob.vanillasquared.mixin;

import blob.vanillasquared.util.data.BlockComponent;
import blob.vanillasquared.util.data.GeneralWeapon;
import blob.vanillasquared.util.data.Dura;
import blob.vanillasquared.util.api.other.vsqIdentifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Debug(export = true)
@Mixin(Item.Properties.class)
public abstract class AxeMixin {

    @Shadow
    public abstract Item.Properties tool(ToolMaterial material, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag, float f, float g, float d);

    @Unique
    private static final Map<ToolMaterial, GeneralWeapon> AXE_WEAPON = Map.of(
            ToolMaterial.WOOD, new GeneralWeapon(6.0d, -3.2d, -0.5d),
            ToolMaterial.STONE, new GeneralWeapon(7.0d, -3.3d, -0.5d),
            ToolMaterial.COPPER, new GeneralWeapon(7.0d, -3.0d, -0.5d),
            ToolMaterial.IRON, new GeneralWeapon(9.0d, -3.0d, -0.5d),
            ToolMaterial.GOLD, new GeneralWeapon(7.0d, -2.9d, 0.0d),
            ToolMaterial.DIAMOND, new GeneralWeapon(10.0d, -3.0d, -0.5d),
            ToolMaterial.NETHERITE, new GeneralWeapon(11.0d, -3.0d, -0.5d)
    );

    @Unique
    private static final Map<ToolMaterial, BlockComponent> AXE = Map.of(
            ToolMaterial.WOOD, new BlockComponent(0.1F, 5.0F, 0.2F, 2.0F, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR),
            ToolMaterial.STONE, new BlockComponent(0.4F, 5.0F, 0.2F, 1.0F, SoundEvents.STONE_PLACE, SoundEvents.STONE_BREAK),
            ToolMaterial.COPPER, new BlockComponent(0.5F, 5.0F, 0.25F, 2.0F, SoundEvents.COPPER_BULB_PLACE, SoundEvents.COPPER_GRATE_BREAK),
            ToolMaterial.IRON, new BlockComponent(0.85F, 5.0F, 0.4F, 2.0F, SoundEvents.IRON_PLACE, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR),
            ToolMaterial.GOLD, new BlockComponent(0F, 5.0F, 1.0F, 0.5F, SoundEvents.IRON_PLACE, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR),
            ToolMaterial.DIAMOND, new BlockComponent(0.95F, 5.0F, 0.5F, 1.0F, SoundEvents.AMETHYST_BLOCK_STEP, SoundEvents.AMETHYST_CLUSTER_BREAK),
            ToolMaterial.NETHERITE, new BlockComponent(1.0F, 5.0F, 0.625F, 1.0F, SoundEvents.NETHERITE_BLOCK_PLACE, SoundEvents.NETHERITE_BLOCK_BREAK)
    );

    @Unique
    private static final Map<ToolMaterial, Dura> DURABILITY = Map.of(
            ToolMaterial.WOOD, new Dura(75),
            ToolMaterial.STONE, new Dura(150),
            ToolMaterial.COPPER, new Dura(200),
            ToolMaterial.IRON, new Dura(250),
            ToolMaterial.GOLD, new Dura(100),
            ToolMaterial.DIAMOND, new Dura(1550),
            ToolMaterial.NETHERITE, new Dura(2069)
    );

    @Inject(at = @At("HEAD"), method = "axe", cancellable = true)
    public void init(ToolMaterial toolMaterial, float f, float g, CallbackInfoReturnable<Item.Properties> cir) {
        BlockComponent s = AXE.getOrDefault(toolMaterial, BlockComponent.DEFAULT);
        GeneralWeapon a = AXE_WEAPON.getOrDefault(toolMaterial, GeneralWeapon.DEFAULT);
        Dura d = DURABILITY.getOrDefault(toolMaterial, Dura.DEFAULT);

        float blockDelay = s.blockDelay();
        float shieldBreakCooldown = s.shieldBreakCooldown();
        float damageReduction = s.damageReduction();
        float duraDMG = s.duraDamage();
        double attackDMG = a.attackDamage();
        double attackSpeed = a.attackSpeed();
        double attackReach = a.entityReach();
        int dura = d.dura();

        Holder<SoundEvent> blockSound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s.blockSound());
        Holder<SoundEvent> breakSound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s.breakSound());

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDMG, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

        if (attackReach != 0.0d) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(vsqIdentifiers.vsqAxeReachOverride.identifier(), attackReach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }

        BlocksAttacks axeBlock = new BlocksAttacks(
                blockDelay,
                shieldBreakCooldown,
                List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, damageReduction)),
                new BlocksAttacks.ItemDamageFunction(1.0F, 1.0F, duraDMG),
                Optional.of(DamageTypeTags.BYPASSES_SHIELD),
                Optional.of(blockSound),
                Optional.of(breakSound)
        );

        cir.setReturnValue(this.tool(toolMaterial, BlockTags.MINEABLE_WITH_AXE, f, g, 5.0F).component(DataComponents.BLOCKS_ATTACKS, axeBlock).component(DataComponents.WEAPON, new Weapon(1, shieldBreakCooldown)).attributes(builder.build()).durability(dura));
    }
}
