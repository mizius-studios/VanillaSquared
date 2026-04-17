package blob.vanillasquared.mixin.world.loot;

import blob.vanillasquared.main.world.loot.RandomizeEnchantmentSlotsFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(LootPool.class)
public abstract class LootPoolMixin {

    @ModifyVariable(method = "addRandomItems", at = @At("HEAD"), argsOnly = true, name = "result")
    // Mixin requires the captured variable followed by the original method arguments for this target signature.
    private Consumer<ItemStack> vsq$sanitizeLootEnchantments(Consumer<ItemStack> original, Consumer<ItemStack> originalArgument, LootContext context) {
        return stack -> {
            if (stack.is(Items.ENCHANTED_BOOK)) {
                return;
            }
            stack.remove(DataComponents.ENCHANTMENTS);
            stack.remove(DataComponents.STORED_ENCHANTMENTS);
            if (!stack.has(blob.vanillasquared.util.api.modules.components.DataComponents.VSQ_ENCHANTMENT)) {
                stack = RandomizeEnchantmentSlotsFunction.DEFAULT_LOOT_RANDOMIZATION.apply(stack, context);
            }
            original.accept(stack);
        };
    }
}
