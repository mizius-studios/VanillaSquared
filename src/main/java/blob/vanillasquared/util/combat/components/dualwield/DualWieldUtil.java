package blob.vanillasquared.util.combat.components.dualwield;

import blob.vanillasquared.util.modules.components.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DualWieldUtil {
    private DualWieldUtil() {
    }

    public static Optional<ActiveDualWield> getActiveDualWield(ItemStack mainHand, ItemStack offHand) {
        if (mainHand.isEmpty() || offHand.isEmpty()) {
            return Optional.empty();
        }
        if (!mainHand.has(DataComponents.DUAL_WIELD) || !offHand.has(DataComponents.DUAL_WIELD)) {
            return Optional.empty();
        }

        DualWieldComponent mainComponent = mainHand.get(DataComponents.DUAL_WIELD);
        DualWieldComponent offComponent = offHand.get(DataComponents.DUAL_WIELD);
        if (mainComponent == null || offComponent == null) {
            return Optional.empty();
        }
        if (!hasMatchingIdentifier(mainComponent.identifiers(), offComponent.identifiers())) {
            return Optional.empty();
        }

        return Optional.of(new ActiveDualWield(mainComponent, offComponent));
    }

    public static ItemEnchantments mergeEnchantments(ItemStack mainHand, ItemStack offHand, ActiveDualWield activeDualWield) {
        ItemEnchantments mainEnchantments = mainHand.getEnchantments();
        ItemEnchantments offEnchantments = offHand.getEnchantments();

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(mainEnchantments);

        for (Holder<Enchantment> enchantment : offEnchantments.keySet()) {
            int offLevel = offEnchantments.getLevel(enchantment);
            if (offLevel <= 0) {
                continue;
            }

            int mainLevel = mainEnchantments.getLevel(enchantment);
            int mergedLevel = isBlocked(enchantment, activeDualWield.mainHand().blockedEnchantmentsTag(), activeDualWield.offHand().blockedEnchantmentsTag())
                    ? Math.max(mainLevel, offLevel)
                    : mainLevel + offLevel;
            mutable.set(enchantment, mergedLevel);
        }

        return mutable.toImmutable();
    }

    public static float getItemAttackDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return (float) modifiers.compute(Attributes.ATTACK_DAMAGE, 0.0D, EquipmentSlot.MAINHAND);
    }

    public static float calculateExtraSweepDamage(float offHandAttackDamage, DualWieldComponent component, boolean critical) {
        float percentage = critical ? component.criticalDamage() : component.sweepingDamage();
        return offHandAttackDamage * (percentage / 100.0F);
    }

    public static void spawnSweepEffects(ServerLevel serverLevel, Player player) {
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                player.getSoundSource(),
                1.0F,
                1.0F
        );

        float yawRadians = player.getYRot() * ((float) Math.PI / 180.0F);
        double xOffset = -Mth.sin(yawRadians);
        double zOffset = Mth.cos(yawRadians);
        serverLevel.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                player.getX() + xOffset,
                player.getY(0.5D),
                player.getZ() + zOffset,
                0,
                xOffset,
                0.0D,
                zOffset,
                0.0D
        );
    }

    public static void playCriticalEffects(Player player, LivingEntity target) {
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT,
                player.getSoundSource(),
                1.0F,
                1.0F
        );
        player.crit(target);
    }

    private static boolean hasMatchingIdentifier(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }

        Set<String> normalized = new HashSet<>(left);
        for (String identifier : right) {
            if (normalized.contains(identifier)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlocked(
            Holder<Enchantment> enchantment,
            Identifier mainBlockedTagId,
            Identifier offBlockedTagId
    ) {
        TagKey<Enchantment> mainBlockedTag = TagKey.create(Registries.ENCHANTMENT, mainBlockedTagId);
        TagKey<Enchantment> offBlockedTag = TagKey.create(Registries.ENCHANTMENT, offBlockedTagId);
        return enchantment.is(mainBlockedTag) || enchantment.is(offBlockedTag);
    }

    public record ActiveDualWield(DualWieldComponent mainHand, DualWieldComponent offHand) {
    }
}
