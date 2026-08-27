package com.example;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;

public class CopRenderer extends VindicatorRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NarcotixMod.MOD_ID,
            "textures/entity/cop.png"
    );

    public CopRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(IllagerRenderState state) {
        return TEXTURE;
    }
}
