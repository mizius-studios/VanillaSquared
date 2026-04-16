package blob.vanillasquared.mixin.world;

import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(net.minecraft.server.commands.EnchantCommand.class)
public abstract class EnchantCommandMixin {
    @Shadow
    private static com.mojang.brigadier.exceptions.DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY;
    @Shadow
    private static com.mojang.brigadier.exceptions.DynamicCommandExceptionType ERROR_NO_ITEM;
    @Shadow
    private static com.mojang.brigadier.exceptions.DynamicCommandExceptionType ERROR_INCOMPATIBLE;
    @Shadow
    private static com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH;
    @Shadow
    private static com.mojang.brigadier.exceptions.SimpleCommandExceptionType ERROR_NOTHING_HAPPENED;

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

            int maxLevel = VSQEnchantmentSlots.maxLevel(item, enchantmentHolder);
            if (level > maxLevel) {
                if (targets.size() == 1) {
                    throw ERROR_LEVEL_TOO_HIGH.create(level, maxLevel);
                }
                continue;
            }

            boolean compatible = enchantment.canEnchant(item)
                    && VSQEnchantmentSlots.aggregate(item).keySet().stream().allMatch(other -> VSQEnchantmentSlots.areCompatible(item, other, enchantmentHolder))
                    && VSQEnchantmentSlots.canApplyInSlots(item, enchantmentHolder, level);
            if (!compatible) {
                if (targets.size() == 1) {
                    throw ERROR_INCOMPATIBLE.create(item.getHoverName().getString());
                }
                continue;
            }

            if (VSQEnchantmentSlots.setEnchantmentLevel(item, enchantmentHolder, level)) {
                success++;
            } else if (targets.size() == 1) {
                throw ERROR_INCOMPATIBLE.create(item.getHoverName().getString());
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
