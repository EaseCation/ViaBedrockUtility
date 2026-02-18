package org.oryxel.viabedrockutility.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.mixin.interfaces.ICustomPlayerRendererHolder;

public class CustomPlayerRenderer extends PlayerEntityRenderer {
    private final Identifier texture;

    public CustomPlayerRenderer(final EntityRendererFactory.Context ctx, final PlayerEntityModel model, final boolean slim, Identifier texture) {
        super(ctx, slim);

        if (model != null) {
            this.model = model;
        }

        this.texture = texture;
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        PlayerEntityRenderState state = super.createRenderState();
        ((ICustomPlayerRendererHolder) state).viaBedrockUtility$setCustomPlayerRenderer(this);
        return state;
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState playerEntityRenderState) {
        return this.texture;
    }
}
