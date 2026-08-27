package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class NarcotixClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
EntityRendererRegistry.register((net.minecraft.world.entity.EntityType) NarcotixCopAdditions.COP, (net.minecraft.client.renderer.entity.EntityRendererProvider) CopRenderer::new);
EntityRendererRegistry.register(NarcotixEntities.WANDERING_PLUG, WanderingPlugRenderer::new);
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