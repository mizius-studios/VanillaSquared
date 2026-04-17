package blob.vanillasquared.main.world.item.components.hitthrough;

import blob.vanillasquared.util.api.references.RegistryReference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public record HitThroughComponent(RegistryReference block) {
    public static final Codec<HitThroughComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryReference.CODEC.fieldOf("block").forGetter(HitThroughComponent::block)
            ).apply(instance, HitThroughComponent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HitThroughComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, component -> component.block().tag(),
            Identifier.STREAM_CODEC, component -> component.block().id(),
            (tag, id) -> new HitThroughComponent(new RegistryReference(id, tag))
    );

    public HitThroughComponent(Identifier tag) {
        this(RegistryReference.tag(tag));
    }

    public HitThroughComponent {
        block = Objects.requireNonNull(block, "block");
    }

    public Identifier tag() {
        if (!this.block.tag()) {
            throw new IllegalStateException("block reference is not a tag: " + this.block.asString());
        }
        return this.block.id();
    }

    public boolean canHitThrough(BlockState blockState) {
        if (this.block.tag()) {
            TagKey<Block> blockTag = this.block.tagKey(Registries.BLOCK);
            return blockState.is(blockTag);
        }
        return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).equals(this.block.id());
    }
}
