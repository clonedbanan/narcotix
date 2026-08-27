package com.example;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class NarcotixCopClient {
    public static void register() {
        EntityRendererRegistry.register(NarcotixCopAdditions.COP, CopRenderer::new);
    }
}
