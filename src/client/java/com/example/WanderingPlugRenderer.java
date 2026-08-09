package com.example;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public class WanderingPlugRenderer extends AgeableMobRenderer<WanderingPlugEntity, VillagerRenderState, VillagerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            NarcotixMod.MOD_ID,
            "textures/entity/wandering_plug.png"
    );

    public WanderingPlugRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)),
                new VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)),
                0.5F
        );
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }
}
