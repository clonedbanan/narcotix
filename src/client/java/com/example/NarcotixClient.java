package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class NarcotixClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
                @SuppressWarnings("unchecked")
                RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                        (RenderLayerParent<AvatarRenderState, PlayerModel>) avatarRenderer;

                helper.register(new MouthJointRenderLayer(parent));
            }
        });
    }
}