package com.example.mixin;

import com.example.NarcotixMod;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public class PlayerItemInHandLayerMixin {
    @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void narcotix$hideSmokeItemInHandWhileSmoking(
            AvatarRenderState state,
            ItemStackRenderState itemStackRenderState,
            ItemStack itemStack,
            HumanoidArm humanoidArm,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        if (state.isUsingItem && isSmokeItem(itemStack)) {
            ci.cancel();
        }
    }

    private static boolean isSmokeItem(ItemStack stack) {
        return stack.is(NarcotixMod.JOINT)
                || stack.is(NarcotixMod.BLUNT)
                || stack.is(NarcotixMod.CIGARETTE);
    }
}
