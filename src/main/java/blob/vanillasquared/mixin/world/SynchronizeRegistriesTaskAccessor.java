package blob.vanillasquared.mixin.world;

import net.minecraft.server.network.config.SynchronizeRegistriesTask;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SynchronizeRegistriesTask.class)
public interface SynchronizeRegistriesTaskAccessor {
    @Accessor("requestedPacks")
    List<KnownPack> vsq$getRequestedPacks();

    @Mutable
    @Accessor("requestedPacks")
    void vsq$setRequestedPacks(List<KnownPack> requestedPacks);
}
