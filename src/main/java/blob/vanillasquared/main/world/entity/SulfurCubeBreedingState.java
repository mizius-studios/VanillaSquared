package blob.vanillasquared.main.world.entity;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public interface SulfurCubeBreedingState {
    boolean vsq$isInLove();

    void vsq$resetLove();

    @Nullable
    ServerPlayer vsq$loveCause();
}
