package com.example.mixin;

import com.example.NarcotixMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class JointSmokingArmPoseMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("HEAD")
    )
    private void narcotix$restoreArmDefaults(HumanoidRenderState state, CallbackInfo ci) {
        this.rightArm.x = -5.0F;
        this.rightArm.y = 2.0F;
        this.rightArm.z = 0.0F;

        this.leftArm.x = 5.0F;
        this.leftArm.y = 2.0F;
        this.leftArm.z = 0.0F;

        this.rightArm.visible = true;
        this.leftArm.visible = true;
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL")
    )
    private void narcotix$poseSmokingArm(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof ArmedEntityRenderState armedState)) {
            return;
        }

        if (!state.isUsingItem) {
            return;
        }

        HumanoidArm useArm = getUseArm(state);
        ItemStack useStack = armedState.getUseItemStackForArm(useArm);

        if (!isSmokeItem(useStack)) {
            return;
        }

        ModelPart arm = useArm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;

        /*
         * Same smoking arm pose for joint, blunt, and cigarette.
         * Tune these values exactly like you tuned the joint pose before.
         */
        if (useArm == HumanoidArm.RIGHT) {
            arm.xRot = -1.95F + (this.head.xRot * 0.55F);
            arm.yRot = -0.40F + (this.head.yRot * 0.65F);
            arm.zRot = 0.35F;
        } else {
            arm.xRot = -1.95F + (this.head.xRot * 0.55F);
            arm.yRot = 0.40F + (this.head.yRot * 0.65F);
            arm.zRot = -0.35F;
        }
    }

    private static HumanoidArm getUseArm(HumanoidRenderState state) {
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
