package blob.vanillasquared.mixin.client.world.entities;

import blob.vanillasquared.main.gui.hud.SwirlingClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void vsq$rotateSwirlingHeldItem(
            ArmedEntityRenderState state,
            ItemStackRenderState item,
            ItemStack itemStack,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (!(state instanceof AvatarRenderState avatarState) || itemStack.isEmpty()) {
            return;
        }

        float weight = SwirlingClientState.visualWeight(avatarState.id, avatarState.ageInTicks);
        if (weight <= 1.0E-3F) {
            return;
        }

        poseStack.translate(0.0F, -0.02F * weight, -0.08F * weight);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * weight));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F * weight));
        poseStack.mulPose(Axis.ZP.rotationDegrees((arm == HumanoidArm.LEFT ? -10.0F : 10.0F) * weight));
    }
}
