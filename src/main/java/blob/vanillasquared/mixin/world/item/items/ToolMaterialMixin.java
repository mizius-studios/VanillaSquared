package blob.vanillasquared.mixin.world.item.items;

import blob.vanillasquared.util.builder.components.DualWieldBuilder;
import blob.vanillasquared.util.builder.components.HitThroughBuilder;
import blob.vanillasquared.util.builder.general.GeneralWeapon;
import blob.vanillasquared.util.builder.durability.Durability;
import blob.vanillasquared.util.api.other.vsqIdentifiers;
import blob.vanillasquared.util.modules.components.DataComponents;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(ToolMaterial.class)
public abstract class ToolMaterialMixin {

    @Shadow
    @Final
    private TagKey<Block> incorrectBlocksForDrops;

    @Shadow
    @Final
    private float speed;

    @Shadow
    private Item.Properties applyCommonProperties(Item.Properties properties) {
        throw new AssertionError();
    }
    @Shadow
    private ItemAttributeModifiers createToolAttributes(float f, float g) {
        throw new AssertionError();
    }

    @Unique
    private static final Map<ToolMaterial, GeneralWeapon> SWORD = Map.of(
            ToolMaterial.WOOD, new GeneralWeapon(vsqIdentifiers.swordOverride.identifier(), EquipmentSlotGroup.MAINHAND, 4.0D, -2.4D, 0.0D),
            ToolMaterial.STONE, new GeneralWeapon(vsqIdentifiers.swordOverride.identifier(), EquipmentSlotGroup.MAINHAND, 5.0D, -2.4D, 0.0D),
            ToolMaterial.COPPER, new GeneralWeapon(vsqIdentifiers.swordOverride.identifier(), EquipmentSlotGroup.MAINHAND, 5.0D, -2.4D, 0.0D),
            ToolMaterial.IRON, new GeneralWeapon(vsqIdentifiers.swordOverride.identifier(), EquipmentSlotGroup.MAINHAND, 6.0D, -2.4D, 0.0D),
            ToolMaterial.GOLD, new GeneralWeapon(vsqIdentifiers.swordOverride.identifier(), EquipmentSlotGroup.MAINHAND, 6.0D, -2.4D, 0.0D),
            ToolMaterial.DIAMOND, new GeneralWeapon(vsqIdentifiers.swordOverride.identifier(), EquipmentSlotGroup.MAINHAND, 7.0D, -2.4D, 0.0D),
            ToolMaterial.NETHERITE, new GeneralWeapon(vsqIdentifiers.swordOverride.identifier(), EquipmentSlotGroup.MAINHAND, 10.0D, -2.4D, 0.0D)
    );

    @Unique
    private static final Map<ToolMaterial, Durability> DURABILITY = Map.of(
            ToolMaterial.WOOD, new Durability(75),
            ToolMaterial.STONE, new Durability(150),
            ToolMaterial.COPPER, new Durability(200),
            ToolMaterial.IRON, new Durability(250),
            ToolMaterial.GOLD, new Durability(100),
            ToolMaterial.DIAMOND, new Durability(1550),
            ToolMaterial.NETHERITE, new Durability(2069)
    );

    @Unique
    private static final Map<ToolMaterial, DualWieldBuilder> DUAL_WIELD = Map.of(
            ToolMaterial.WOOD, new DualWieldBuilder(List.of("vsq$sword"), 1000, 1, 50, 200, "vsq", "dual_wield_blocked"),
            ToolMaterial.STONE, new DualWieldBuilder(List.of("vsq$sword"), 1000, 1, 50, 200, "vsq", "dual_wield_blocked"),
            ToolMaterial.COPPER, new DualWieldBuilder(List.of("vsq$sword"), 1000, 1, 50, 200, "vsq", "dual_wield_blocked"),
            ToolMaterial.IRON, new DualWieldBuilder(List.of("vsq$sword"), 1000, 1, 50, 200, "vsq", "dual_wield_blocked"),
            ToolMaterial.GOLD, new DualWieldBuilder(List.of("vsq$sword"), 1000, 1, 50, 200, "vsq", "dual_wield_blocked"),
            ToolMaterial.DIAMOND, new DualWieldBuilder(List.of("vsq$sword"), 1000, 1, 50, 200, "vsq", "dual_wield_blocked"),
            ToolMaterial.NETHERITE, new DualWieldBuilder(List.of("vsq$sword"), 1000, 1, 50, 200, "vsq", "dual_wield_blocked")
    );

    @Unique
    private static final HitThroughBuilder HIT_THROUGH_PLANTS = new HitThroughBuilder("vsq", "hit_through");

    @Inject(method = "applySwordProperties", at = @At("HEAD"), cancellable = true)
    private void applySwordProperties(Item.Properties properties, float attackDamage, float attackSpeed, CallbackInfoReturnable<Item.Properties> cir) {
        ToolMaterial material = (ToolMaterial) (Object) this;
        GeneralWeapon swordAttributes = SWORD.get(material);
        DualWieldBuilder dualWieldSword = DUAL_WIELD.get(material);
        if (swordAttributes == null || dualWieldSword == null) {
            return;
        }

        cir.setReturnValue(buildSwordProperties(properties, swordAttributes, dualWieldSword));
    }

    @Inject(method = "applyToolProperties", at = @At("HEAD"), cancellable = true)
    private void applyToolProperties(Item.Properties properties, TagKey<Block> tagKey, float attackDamage, float attackSpeed, float weaponDamage, CallbackInfoReturnable<Item.Properties> cir) {
        ToolMaterial material = (ToolMaterial) (Object) this;
        Durability durability = DURABILITY.getOrDefault(material, Durability.DEFAULT);
        cir.setReturnValue(buildToolProperties(properties, tagKey, attackDamage, attackSpeed, weaponDamage, durability));
    }

    @Unique
    private Item.Properties buildSwordProperties(Item.Properties properties, GeneralWeapon swordAttributes, DualWieldBuilder dualWieldSword) {
        HolderGetter<Block> holderGetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        Tool toolComponent = new Tool(createSwordRules(holderGetter), 1.0F, 2, false);

        return this.applyCommonProperties(properties)
                .component(net.minecraft.core.component.DataComponents.TOOL, toolComponent)
                .attributes(swordAttributes.build())
                .component(net.minecraft.core.component.DataComponents.WEAPON, new Weapon(1))
                .component(DataComponents.HIT_THROUGH, HIT_THROUGH_PLANTS.build())
                .component(DataComponents.DUAL_WIELD, dualWieldSword.build());
    }

    @Unique
    private Item.Properties buildToolProperties(
            Item.Properties properties,
            TagKey<Block> tagKey,
            float attackDamage,
            float attackSpeed,
            float weaponDamage,
            Durability durability
    ) {
        HolderGetter<Block> holderGetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        Tool toolComponent = new Tool(
                List.of(
                        Tool.Rule.deniesDrops(holderGetter.getOrThrow(this.incorrectBlocksForDrops)),
                        Tool.Rule.minesAndDrops(holderGetter.getOrThrow(tagKey), this.speed)
                ),
                1.0F,
                1,
                true
        );

        return this.applyCommonProperties(properties)
                .component(net.minecraft.core.component.DataComponents.TOOL, toolComponent)
                .attributes(this.createToolAttributes(attackDamage, attackSpeed))
                .component(net.minecraft.core.component.DataComponents.WEAPON, new Weapon(2, weaponDamage))
                .durability(durability.durability());
    }

    @Unique
    private List<Tool.Rule> createSwordRules(HolderGetter<Block> holderGetter) {
        return List.of(
                Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
                Tool.Rule.overrideSpeed(holderGetter.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE),
                Tool.Rule.overrideSpeed(holderGetter.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F)
        );
    }
}
