package blob.vanillasquared.mixin.client.world.item;

import blob.vanillasquared.main.gui.enchantment.VSQEnchantmentTooltipState;
import blob.vanillasquared.main.world.item.enchantment.VSQEnchantmentComponent;
import blob.vanillasquared.util.api.enchantment.VSQEnchantments;
import blob.vanillasquared.util.api.modules.components.VSQDataComponents;
import blob.vanillasquared.util.api.modules.components.VSQItemComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.resources.ResourceKey;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void vsq$addEnchantRecipeTooltip(Item.TooltipContext context, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        ResourceKey<Recipe<?>> recipeKey = VSQItemComponents.getEnchantRecipe(stack);
        if (recipeKey == null) {
            return;
        }

        List<Component> tooltip = new ArrayList<>(cir.getReturnValue());
        tooltip.add(vsq$slotTooltipInsertionIndex(tooltip), vsq$recipeDisplayName(recipeKey).withStyle(ChatFormatting.GRAY));
        cir.setReturnValue(List.copyOf(tooltip));
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void vsq$replaceVanillaEnchantTooltip(Item.TooltipContext context, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        VSQEnchantmentComponent component = VSQItemComponents.getEnchantmentComponent(stack);
        TooltipDisplay tooltipDisplay = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (!tooltipDisplay.shows(VSQDataComponents.ENCHANTMENT)) {
            component = null;
        }
        VSQEnchantmentTooltipState.onTooltip(ItemStack.hashItemAndComponents(stack), component);
        if (component == null) {
            return;
        }

        boolean leftAltHeld = Minecraft.getInstance().gui.screen() != null
                && org.lwjgl.glfw.GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;

        List<Component> filtered = new ArrayList<>(cir.getReturnValue());
        for (Component enchantLine : VSQEnchantments.aggregate(stack).entrySet().stream().map(entry -> net.minecraft.world.item.enchantment.Enchantment.getFullname(entry.getKey(), entry.getIntValue())).toList()) {
            filtered.removeIf(line -> line.getString().equals(enchantLine.getString()));
        }

        int insertionIndex = vsq$slotTooltipInsertionIndex(filtered);
        filtered.addAll(insertionIndex, component.tooltipLines(VSQEnchantmentTooltipState.selectedIndex(component), leftAltHeld));
        cir.setReturnValue(List.copyOf(filtered));
    }

    @Unique
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

    @Unique
    private static boolean vsq$isBottomInfoLine(Component line) {
        if (line.getContents() instanceof TranslatableContents translatableContents) {
            String key = translatableContents.getKey();
            if (key.startsWith("itemGroup.") || key.equals("item.durability") || key.equals("item.nbt_tags") || key.equals("item.components")) {
                return true;
            }
        }

        return vsq$hasColor(line, ChatFormatting.DARK_GRAY);
    }

    @Unique
    private static boolean vsq$hasColor(Component line, ChatFormatting formatting) {
        TextColor color = TextColor.fromLegacyFormat(formatting);
        if (line.getStyle().getColor() == null || color == null) {
            return false;
        }
        return line.getStyle().getColor().equals(color);
    }

    @Unique
    private static MutableComponent vsq$recipeDisplayName(ResourceKey<Recipe<?>> recipeKey) {
        String namespace = recipeKey.identifier().getNamespace();
        String path = recipeKey.identifier().getPath().replace('/', '.');
        String namespacedRecipeKey = "vsq.recipe." + namespace + "." + path;
        String recipeKeyWithoutNamespace = "vsq.recipe." + path;
        String enchantmentKey = "enchantment." + namespace + "." + path;

        for (String key : List.of(namespacedRecipeKey, recipeKeyWithoutNamespace, enchantmentKey)) {
            if (Language.getInstance().has(key)) {
                return Component.translatable(key);
            }
        }

        return Component.literal(recipeKey.identifier().toString());
    }
}
