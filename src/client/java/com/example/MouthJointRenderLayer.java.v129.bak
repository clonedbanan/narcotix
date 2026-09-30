package com.example;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class MouthJointRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public MouthJointRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            AvatarRenderState state,
            float yRot,
            float xRot
    ) {
        if (!state.isUsingItem) {
            return;
        }

        HumanoidArm useArm = getUseArm(state);
        ItemStack useStack = state.getUseItemStackForArm(useArm);

        if (!isSmokeItem(useStack)) {
            return;
        }

        ItemStackRenderState itemState = useArm == HumanoidArm.RIGHT
                ? state.rightHandItemState
                : state.leftHandItemState;

        if (itemState == null || itemState.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        try {
            this.getParentModel().head.translateAndRotate(poseStack);

            /*
             * Mouth placement.
             * This keeps the same position you tuned for the joint.
             * The blunt/cigarette use the same mouth point and same arm pose.
             */
            poseStack.translate(0.08D, -0.30D, -0.72D);
            poseStack.scale(1.25F, 1.25F, 1.25F);

            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(80.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-80.0F));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(80.0F));

            itemState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        } finally {
            poseStack.popPose();
        }
    }

    private static HumanoidArm getUseArm(AvatarRenderState state) {
        if (state.useItemHand == InteractionHand.MAIN_HAND) {
            return state.mainArm;
        }

        return state.mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    private static boolean isSmokeItem(ItemStack stack) {
        return stack.is(NarcotixMod.JOINT)
                || stack.is(NarcotixMod.BLUNT)
                || stack.is(NarcotixMod.CIGARETTE);
    }
}
