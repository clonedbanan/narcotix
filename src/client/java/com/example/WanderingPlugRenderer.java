package com.example;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public class WanderingPlugRenderer extends WanderingTraderRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NarcotixMod.MOD_ID,
            "textures/entity/wandering_plug.png"
    );

    public WanderingPlugRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }
}
