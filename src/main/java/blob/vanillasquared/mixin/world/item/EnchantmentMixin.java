package blob.vanillasquared.mixin.world.item;

import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentAccess;
import blob.vanillasquared.main.world.item.components.enchantment.VSQEnchantmentSlotType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin implements VSQEnchantmentAccess {
    @Shadow
    @Final
    @Mutable
    public static Codec<Enchantment> DIRECT_CODEC;

    @Unique
    private VSQEnchantmentSlotType vsq$enchantmentSlotType;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void vsq$extendCodec(CallbackInfo ci) {
        DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("description").forGetter(Enchantment::description),
                Enchantment.EnchantmentDefinition.CODEC.forGetter(Enchantment::definition),
                RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("exclusive_set", HolderSet.empty()).forGetter(Enchantment::exclusiveSet),
                EnchantmentEffectComponents.CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY).forGetter(Enchantment::effects),
                VSQEnchantmentSlotType.CODEC.optionalFieldOf("enchantment_slot").forGetter(enchantment -> Optional.ofNullable(((VSQEnchantmentAccess) (Object) enchantment).vsq$getEnchantmentSlotType()))
        ).apply(instance, (description, definition, exclusiveSet, effects, slotType) -> {
            Enchantment enchantment = new Enchantment(description, definition, exclusiveSet, effects);
            ((VSQEnchantmentAccess) (Object) enchantment).vsq$setEnchantmentSlotType(slotType.orElse(null));
            return enchantment;
        }));
    }

    @Override
    public VSQEnchantmentSlotType vsq$getEnchantmentSlotType() {
        return this.vsq$enchantmentSlotType;
    }

    @Override
    public void vsq$setEnchantmentSlotType(VSQEnchantmentSlotType slotType) {
        this.vsq$enchantmentSlotType = slotType;
    }
}
