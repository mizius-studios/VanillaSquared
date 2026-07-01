package blob.vanillasquared.util.api.modules.components;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

public final class ComponentRegistry {
    private ComponentRegistry() {
    }

    public static <T> DataComponentType<T> registerPersistent(Identifier id, Codec<T> codec) {
        return registerPersistent(id, codec, BuiltInRegistries.DATA_COMPONENT_TYPE);
    }

    public static <T> DataComponentType<T> registerPersistent(
            Identifier id,
            Codec<T> codec,
            Registry<DataComponentType<?>> registry
    ) {
        return Registry.register(
                registry,
                id,
                DataComponentType.<T>builder()
                        .persistent(codec)
                        .build()
        );
    }

    public static <T> DataComponentType<T> registerCached(Identifier id, Codec<T> codec) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                id,
                DataComponentType.<T>builder()
                        .persistent(codec)
                        .cacheEncoding()
                        .build()
        );
    }

    public static <T> DataComponentType<T> registerSynchronized(
            Identifier id,
            Codec<T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
    ) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                id,
                DataComponentType.<T>builder()
                        .persistent(codec)
                        .networkSynchronized(streamCodec)
                        .build()
        );
    }

    public static <T> DataComponentType<T> registerSynchronizedCached(
            Identifier id,
            Codec<T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
    ) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                id,
                DataComponentType.<T>builder()
                        .persistent(codec)
                        .networkSynchronized(streamCodec)
                        .cacheEncoding()
                        .build()
        );
    }
}
