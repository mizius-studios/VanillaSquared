package blob.vanillasquared.main.world.item.components.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record VSQSendChatMessageEffect(
        Component message
) implements EnchantmentEntityEffect {
    private static final Pattern PLACEHOLDERS = Pattern.compile("\\$(a|e|i)");

    public static final MapCodec<VSQSendChatMessageEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("message", Component.empty()).forGetter(VSQSendChatMessageEffect::message)
    ).apply(instance, VSQSendChatMessageEffect::new));

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        Entity affectedResolved = entity;
        Entity enchantedResolved = item.owner() != null ? item.owner() : entity;
        Map<String, Component> replacements = Map.of(
                "a", affectedResolved.getName(),
                "e", enchantedResolved.getName(),
                "i", item.itemStack().getHoverName()
        );
        MutableComponent text = Component.empty();
        this.message.visit((Style style, String content) -> {
            Matcher matcher = PLACEHOLDERS.matcher(content);
            int lastIndex = 0;
            while (matcher.find()) {
                if (matcher.start() > lastIndex) {
                    text.append(Component.literal(content.substring(lastIndex, matcher.start())).withStyle(style));
                }
                text.append(replacements.get(matcher.group(1)).copy().withStyle(style));
                lastIndex = matcher.end();
            }
            if (lastIndex < content.length()) {
                text.append(Component.literal(content.substring(lastIndex)).withStyle(style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        if (affectedResolved instanceof ServerPlayer target) {
            target.sendSystemMessage(text);
        } else if (enchantedResolved instanceof ServerPlayer player) {
            player.sendSystemMessage(text);
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return MAP_CODEC;
    }
}
