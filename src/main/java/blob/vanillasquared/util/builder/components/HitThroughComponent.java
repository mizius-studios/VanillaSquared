package blob.vanillasquared.util.builder.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public record HitThroughComponent(Identifier tag) {
    public static final Codec<HitThroughComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("tag").forGetter(HitThroughComponent::tag)
            ).apply(instance, HitThroughComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HitThroughComponent> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, HitThroughComponent::tag,
            HitThroughComponent::new
    );

    public HitThroughComponent {
        tag = Objects.requireNonNull(tag, "tag");
    }

    public boolean canHitThrough(BlockState blockState) {
        TagKey<Block> blockTag = TagKey.create(Registries.BLOCK, this.tag);
        return blockState.is(blockTag);
    }
}
