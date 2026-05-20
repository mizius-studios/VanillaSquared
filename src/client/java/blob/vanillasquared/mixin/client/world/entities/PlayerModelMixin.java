package blob.vanillasquared.mixin.client.world.entities;

import blob.vanillasquared.main.gui.hud.SwirlingClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {
    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void vsq$applySwirlingWeaponSwing(AvatarRenderState state, CallbackInfo ci) {
        float progress = SwirlingClientState.progress(state.id, state.ageInTicks);
        if (progress < 0.0F) {
            return;
        }

        SwirlingClientState.tick(state.ageInTicks);
        float warmup = SwirlingClientState.warmupProgress(state.id, state.ageInTicks);
        float visualWeight = SwirlingClientState.visualWeight(state.id, state.ageInTicks);
        float charge = (1.0F - (1.0F - warmup) * (1.0F - warmup)) * visualWeight;
        float pulse = Mth.sin(progress * Mth.PI);
        ModelPart mainArm = state.mainArm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
        ModelPart offArm = state.mainArm == HumanoidArm.LEFT ? this.rightArm : this.leftArm;

        this.body.yRot += (state.mainArm == HumanoidArm.LEFT ? -0.12F : 0.12F) * charge;
        mainArm.xRot = Mth.lerp(charge, -0.55F, -1.50F - pulse * 0.05F);
        mainArm.yRot = Mth.lerp(charge, state.mainArm == HumanoidArm.LEFT ? 0.18F : -0.18F, state.mainArm == HumanoidArm.LEFT ? 0.42F : -0.42F);
        mainArm.zRot = Mth.lerp(charge, state.mainArm == HumanoidArm.LEFT ? -0.38F : 0.38F, state.mainArm == HumanoidArm.LEFT ? -0.01F : 0.01F);
        offArm.xRot = Mth.lerp(charge, -0.35F, -1.50F - pulse * 0.05F);
        offArm.yRot = Mth.lerp(charge, state.mainArm == HumanoidArm.LEFT ? -0.16F : 0.16F, state.mainArm == HumanoidArm.LEFT ? -0.42F : 0.42F);
        offArm.zRot = Mth.lerp(charge, state.mainArm == HumanoidArm.LEFT ? 0.34F : -0.34F, state.mainArm == HumanoidArm.LEFT ? 0.01F : -0.01F);
    }
}
