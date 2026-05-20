package blob.vanillasquared.mixin.client.world.entities;

import blob.vanillasquared.main.gui.hud.SwirlingClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void vsq$applySwirlingBodySpin(AvatarRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
        float progress = SwirlingClientState.progress(state.id, state.ageInTicks);
        if (progress < 0.0F) {
            return;
        }

        float warmup = SwirlingClientState.warmupProgress(state.id, state.ageInTicks);
        float spinProgress = SwirlingClientState.spinProgress(state.id, state.ageInTicks);
        float visualWeight = SwirlingClientState.visualWeight(state.id, state.ageInTicks);
        float charge = 1.0F - (1.0F - warmup) * (1.0F - warmup);
        float eased = Mth.sin(progress * Mth.PI);
        float spin = spinProgress * 360.0F * 7.5F * visualWeight;
        float lean = 8.5F * eased * charge * visualWeight;
        poseStack.mulPose(new Quaternionf().rotationX((-6.0F * (1.0F - warmup) * visualWeight) * Mth.DEG_TO_RAD));
        poseStack.mulPose(new Quaternionf().rotationY(spin * Mth.DEG_TO_RAD));
        poseStack.mulPose(new Quaternionf().rotationX(lean * Mth.DEG_TO_RAD));
    }
}
