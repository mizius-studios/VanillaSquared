package blob.vanillasquared.mixin.world;

import blob.vanillasquared.main.world.VSQExperiments;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.SynchronizeRegistriesTask;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImpl {
    @Shadow
    private SynchronizeRegistriesTask synchronizeRegistriesTask;

    protected ServerConfigurationPacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    @Inject(
            method = "startConfiguration",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;synchronizeRegistriesTask:Lnet/minecraft/server/network/config/SynchronizeRegistriesTask;",
                    opcode = 181,
                    shift = At.Shift.AFTER
            )
    )
    private void vsq$sendExperimentRegistriesOverNetwork(CallbackInfo ci) {
        SynchronizeRegistriesTaskAccessor accessor = (SynchronizeRegistriesTaskAccessor) this.synchronizeRegistriesTask;
        List<KnownPack> requestedPacks = accessor.vsq$getRequestedPacks();
        List<KnownPack> filteredPacks = requestedPacks.stream()
                .filter(knownPack -> !VSQExperiments.isBuiltinPackId(knownPack.id()))
                .toList();

        if (filteredPacks.size() != requestedPacks.size()) {
            accessor.vsq$setRequestedPacks(filteredPacks);
        }
    }
}
