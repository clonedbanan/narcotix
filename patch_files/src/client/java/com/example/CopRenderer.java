package com.example;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public class CopRenderer extends VillagerRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NarcotixMod.MOD_ID,
            "textures/entity/cop.png"
    );

    public CopRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }
}
