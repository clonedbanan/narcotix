package com.example;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class BentSmokingArmModel extends EntityModel<AvatarRenderState> {
    public final ModelPart upperArm;
    public final ModelPart forearm;

    public BentSmokingArmModel(ModelPart root) {
        super(root);
        this.upperArm = root.getChild("upper_arm");
        this.forearm = this.upperArm.getChild("forearm");
    }

    public static LayerDefinition createLayer(boolean slim, boolean rightArm) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float armWidth = slim ? 3.0F : 4.0F;
        float halfWidth = armWidth / 2.0F;

        /*
         * Vanilla skin UV areas:
         * Right arm: x 40, y 16
         * Left arm:  x 32, y 48
         */
        int armTexX = rightArm ? 40 : 32;
        int armTexY = rightArm ? 16 : 48;

        PartDefinition upperArm = root.addOrReplaceChild(
                "upper_arm",
                CubeListBuilder.create()
                        .texOffs(armTexX, armTexY)
                        .addBox(-halfWidth, -2.0F, -2.0F, armWidth, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        upperArm.addOrReplaceChild(
                "forearm",
                CubeListBuilder.create()
                        .texOffs(armTexX, armTexY + 6)
                        .addBox(-halfWidth, 0.0F, -2.0F, armWidth, 6.0F, 4.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setupSmokingPose(boolean rightArm, float headXRot, float headYRot) {
        this.resetPose();

        this.upperArm.xRot = -1.20F + (headXRot * 0.55F);

        if (rightArm) {
            this.upperArm.yRot = -0.75F + (headYRot * 0.75F);
            this.upperArm.zRot = 0.55F + (headYRot * 0.20F);

            this.forearm.xRot = -1.55F;
            this.forearm.yRot = -0.25F;
            this.forearm.zRot = 0.10F;
        } else {
            this.upperArm.yRot = 0.75F + (headYRot * 0.75F);
            this.upperArm.zRot = -0.55F - (headYRot * 0.20F);

            this.forearm.xRot = -1.55F;
            this.forearm.yRot = 0.25F;
            this.forearm.zRot = -0.10F;
        }
    }
}