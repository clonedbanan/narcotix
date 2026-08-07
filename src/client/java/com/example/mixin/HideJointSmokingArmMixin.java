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
public class HideJointSmokingArmMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("HEAD")
    )
    private void narcotix$restoreArmsBeforeSetup(HumanoidRenderState state, CallbackInfo ci) {
        this.rightArm.visible = true;
        this.leftArm.visible = true;
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL")
    )
    private void narcotix$hideSmokingArm(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof ArmedEntityRenderState armedState)) {
            return;
        }

        if (!state.isUsingItem) {
            return;
        }

        HumanoidArm useArm = getUseArm(state);
        ItemStack useStack = armedState.getUseItemStackForArm(useArm);

        if (!useStack.is(NarcotixMod.JOINT)) {
            return;
        }

        if (useArm == HumanoidArm.RIGHT) {
            this.rightArm.visible = false;
        } else {
            this.leftArm.visible = false;
        }
    }

    private static HumanoidArm getUseArm(HumanoidRenderState state) {
        if (state.useItemHand == InteractionHand.MAIN_HAND) {
            return state.mainArm;
        }

        return state.mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }
}