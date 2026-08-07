package com.example;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;

public class BentSmokingArmRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final BentSmokingArmModel wideRightArmModel;
    private final BentSmokingArmModel wideLeftArmModel;
    private final BentSmokingArmModel slimRightArmModel;
    private final BentSmokingArmModel slimLeftArmModel;

    public BentSmokingArmRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);

        this.wideRightArmModel = new BentSmokingArmModel(BentSmokingArmModel.createLayer(false, true).bakeRoot());
        this.wideLeftArmModel = new BentSmokingArmModel(BentSmokingArmModel.createLayer(false, false).bakeRoot());
        this.slimRightArmModel = new BentSmokingArmModel(BentSmokingArmModel.createLayer(true, true).bakeRoot());
        this.slimLeftArmModel = new BentSmokingArmModel(BentSmokingArmModel.createLayer(true, false).bakeRoot());
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

        if (!useStack.is(NarcotixMod.JOINT)) {
            return;
        }

        boolean rightArm = useArm == HumanoidArm.RIGHT;
        boolean slim = state.skin.model() == PlayerModelType.SLIM;

        BentSmokingArmModel model;

        if (slim) {
            model = rightArm ? this.slimRightArmModel : this.slimLeftArmModel;
        } else {
            model = rightArm ? this.wideRightArmModel : this.wideLeftArmModel;
        }

        poseStack.pushPose();

        try {
            /*
             * Position of the custom arm.
             * Since scale is now 1.0, these values may need tuning.
             *
             * X = left/right
             * Y = up/down
             * Z = forward/back
             */
            if (rightArm) {
                poseStack.translate(-0.38D, -0.02D, -0.08D);
            } else {
                poseStack.translate(0.38D, -0.02D, -0.08D);
            }

            poseStack.scale(1.0F, 1.0F, 1.0F);

            model.setupSmokingPose(
                    rightArm,
                    this.getParentModel().head.xRot,
                    this.getParentModel().head.yRot
            );

            Identifier texture = state.skin.body().texturePath();

            renderColoredCutoutModel(
                    model,
                    texture,
                    poseStack,
                    collector,
                    packedLight,
                    state,
                    -1,
                    OverlayTexture.NO_OVERLAY
            );
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
}