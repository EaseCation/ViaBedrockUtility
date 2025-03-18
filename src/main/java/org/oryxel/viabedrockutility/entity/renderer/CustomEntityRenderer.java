package org.oryxel.viabedrockutility.entity.renderer;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.entity.CustomEntity;
import org.oryxel.viabedrockutility.entity.renderer.model.CustomEntityModel;

public class CustomEntityRenderer extends MobEntityRenderer<CustomEntity, LivingEntityRenderState, CustomEntityModel> {
    private Identifier texture;

    public CustomEntityRenderer(EntityRendererFactory.Context context, CustomEntityModel entityModel, Identifier texture) {
        super(context, entityModel, 0);
        this.texture = texture;
    }

    @Override
    public Identifier getTexture(LivingEntityRenderState state) {
        return this.texture;
    }

    @Override
    public boolean shouldRender(CustomEntity entity, Frustum frustum, double x, double y, double z) {
        double d = 64.0F * Entity.getRenderDistanceMultiplier();
        return entity.squaredDistanceTo(x, y, z) <= d * d;
    }

    @Override
    protected boolean canBeCulled(CustomEntity entity) {
        return false;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public void updateRenderState(CustomEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f) {
        super.updateRenderState(livingEntity, livingEntityRenderState, f);

        if (livingEntity.model != null) {
            this.model = livingEntity.model;
        }

        if (livingEntity.texture != null) {
            this.texture = livingEntity.texture;
        }
    }
}
