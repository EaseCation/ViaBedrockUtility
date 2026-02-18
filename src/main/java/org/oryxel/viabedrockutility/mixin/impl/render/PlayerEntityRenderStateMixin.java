package org.oryxel.viabedrockutility.mixin.impl.render;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.oryxel.viabedrockutility.mixin.interfaces.ICustomPlayerRendererHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements ICustomPlayerRendererHolder {
    @Unique
    private EntityRenderer<?, ?> customPlayerRenderer;

    @Override
    public EntityRenderer<?, ?> viaBedrockUtility$getCustomPlayerRenderer() {
        return this.customPlayerRenderer;
    }

    @Override
    public void viaBedrockUtility$setCustomPlayerRenderer(EntityRenderer<?, ?> renderer) {
        this.customPlayerRenderer = renderer;
    }
}
