package blob.vanillasquared.mixin.client.world;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentTooltipState;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlots;
import blob.vanillasquared.util.api.modules.components.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void vsq$replaceVanillaEnchantTooltip(Item.TooltipContext context, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        VSQEnchantmentComponent component = stack.get(DataComponents.VSQ_ENCHANTMENT);
        if (component == null) {
            return;
        }

        VSQEnchantmentTooltipState.onTooltip(stack);
        boolean leftAltHeld = Minecraft.getInstance().screen != null
                && org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;

        List<Component> filtered = new ArrayList<>(cir.getReturnValue());
        for (Component enchantLine : VSQEnchantmentSlots.aggregate(stack).entrySet().stream().map(entry -> net.minecraft.world.item.enchantment.Enchantment.getFullname(entry.getKey(), entry.getIntValue())).toList()) {
            filtered.removeIf(line -> line.getString().equals(enchantLine.getString()));
        }

        int insertionIndex = vsq$slotTooltipInsertionIndex(filtered);
        filtered.addAll(insertionIndex, VSQEnchantmentSlots.buildTooltipLines(component, VSQEnchantmentTooltipState.selectedIndex(component), leftAltHeld));
        cir.setReturnValue(List.copyOf(filtered));
    }

    private static int vsq$slotTooltipInsertionIndex(List<Component> tooltipLines) {
        int insertionIndex = tooltipLines.size();
        for (int index = tooltipLines.size() - 1; index >= 0; index--) {
            if (vsq$isBottomInfoLine(tooltipLines.get(index))) {
                insertionIndex = index;
            } else if (insertionIndex != tooltipLines.size()) {
                break;
            }
        }
        return insertionIndex;
    }

    private static boolean vsq$isBottomInfoLine(Component line) {
        if (line.getContents() instanceof TranslatableContents translatableContents) {
            String key = translatableContents.getKey();
            if (key.startsWith("itemGroup.") || key.equals("item.durability") || key.equals("item.nbt_tags") || key.equals("item.components")) {
                return true;
            }
        }

        return vsq$hasColor(line, ChatFormatting.DARK_GRAY);
    }

    private static boolean vsq$hasColor(Component line, ChatFormatting formatting) {
        if (line.getStyle().getColor() == null || formatting.getColor() == null) {
            return false;
        }
        return line.getStyle().getColor().getValue() == formatting.getColor();
    }
}
