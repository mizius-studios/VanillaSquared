package blob.vanillasquared.mixin.commands;

import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EnchantCommand.class)
public abstract class EnchantCommandMixin {
    @Final
    @Shadow
    private static DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY;
    @Final
    @Shadow
    private static DynamicCommandExceptionType ERROR_NO_ITEM;
    @Final
    @Shadow
    private static DynamicCommandExceptionType ERROR_INCOMPATIBLE;
    @Final
    @Shadow
    private static Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH;
    @Final
    @Shadow
    private static SimpleCommandExceptionType ERROR_NOTHING_HAPPENED;

    @Inject(method = "enchant", at = @At("HEAD"), cancellable = true)
    private static void vsq$enchantWithSlotRules(CommandSourceStack source, Collection<? extends Entity> targets, Holder<Enchantment> enchantmentHolder, int level, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        Enchantment enchantment = enchantmentHolder.value();
        int success = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity target)) {
                if (targets.size() == 1) {
                    throw ERROR_NOT_LIVING_ENTITY.create(entity.getName().getString());
                }
                continue;
            }

            ItemStack item = target.getMainHandItem();
            if (item.isEmpty()) {
                if (targets.size() == 1) {
                    throw ERROR_NO_ITEM.create(target.getName().getString());
                }
                continue;
            }

            int maxLevel = VSQEnchantments.maxLevel(item, enchantmentHolder);
            if (level > maxLevel) {
                if (targets.size() == 1) {
                    throw ERROR_LEVEL_TOO_HIGH.create(level, maxLevel);
                }
                continue;
            }

            int currentLevel = VSQEnchantments.currentLevel(item, enchantmentHolder);
            if (currentLevel >= level) {
                if (targets.size() == 1) {
                    throw ERROR_NOTHING_HAPPENED.create();
                }
                continue;
            }

            boolean compatible = enchantment.canEnchant(item)
                    && VSQEnchantments.aggregate(item).keySet().stream().allMatch(other -> other.equals(enchantmentHolder) || VSQEnchantments.areCompatible(item, other, enchantmentHolder))
                    && VSQEnchantments.canApply(item, enchantmentHolder, level);
            if (!compatible) {
                if (targets.size() == 1) {
                    throw ERROR_INCOMPATIBLE.create(item.getHoverName().getString());
                }
                continue;
            }

            if (VSQEnchantments.setLevel(item, enchantmentHolder, level)) {
                success++;
            } else if (targets.size() == 1) {
                throw ERROR_NOTHING_HAPPENED.create();
            }
        }

        if (success == 0) {
            throw ERROR_NOTHING_HAPPENED.create();
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "commands.enchant.success.single",
                            Enchantment.getFullname(enchantmentHolder, level),
                            targets.iterator().next().getDisplayName()
                    ),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.translatable("commands.enchant.success.multiple", Enchantment.getFullname(enchantmentHolder, level), targets.size()),
                    true
            );
        }

        cir.setReturnValue(success);
    }
}
