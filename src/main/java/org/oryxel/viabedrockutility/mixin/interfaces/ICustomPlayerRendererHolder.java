package org.oryxel.viabedrockutility.mixin.interfaces;

import net.minecraft.client.render.entity.EntityRenderer;

public interface ICustomPlayerRendererHolder {
    EntityRenderer<?, ?> viaBedrockUtility$getCustomPlayerRenderer();
    void viaBedrockUtility$setCustomPlayerRenderer(EntityRenderer<?, ?> renderer);
}
