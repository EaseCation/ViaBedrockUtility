package org.oryxel.viabedrockutility.renderer;

import lombok.Getter;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.oryxel.viabedrockutility.animation.animator.Animator;
import org.oryxel.viabedrockutility.entity.CustomEntityTicker;
import org.oryxel.viabedrockutility.material.data.Material;
import org.oryxel.viabedrockutility.pack.definitions.AnimationDefinitions;
import org.oryxel.viabedrockutility.renderer.model.CustomEntityModel;

import java.util.List;

@Getter
public class CustomEntityRenderer<T extends Entity> extends EntityRenderer<T, CustomEntityRenderer.CustomEntityRenderState> {
    private final CustomEntityTicker ticker;
    private final List<Model> models;

    public CustomEntityRenderer(final CustomEntityTicker ticker, final List<Model> models, EntityRendererFactory.Context context) {
        super(context);
        this.models = models;
        this.ticker = ticker;
    }

    @Override
    public void render(CustomEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-1.0F, -1.0F, 1.0F);
            matrices.translate(0.0F, -1.501F, 0.0F);
            this.ticker.update();
            model.model.setAngles(state);

            RenderLayer renderLayer = model.material.info().getVariants().get("skinning_color").build().apply(model.texture);
            if (renderLayer != null) {
                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
                model.model.render(matrices, vertexConsumer, light, OverlayTexture.packUv(0, 10));
            }

            matrices.pop();
        }
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        double d = 64.0F * Entity.getRenderDistanceMultiplier();
        return entity.squaredDistanceTo(x, y, z) <= d * d;
    }

    @Override
    public void updateRenderState(T entity, CustomEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.yaw = entity.getYaw(tickDelta);
        state.bodyYaw = entity.getBodyYaw();
        state.bodyPitch = entity.getPitch();
        state.distanceTraveled = entity.distanceTraveled;
    }

    private void setupTransforms(CustomEntityRenderState state, MatrixStack matrices) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - state.yaw));
    }

    @Override
    public CustomEntityRenderState createRenderState() {
        return new CustomEntityRenderState();
    }

    public record Model(String key, String geometry, CustomEntityModel<CustomEntityRenderState> model, Identifier texture, Material material) {
    }

    @Getter
    public static class CustomEntityRenderState extends EntityRenderState {
        private float yaw, bodyYaw, bodyPitch;
        private float distanceTraveled;
    }

    public void reset() {
        this.getModels().forEach(m -> m.model().reset());
    }

    public void play(final AnimationDefinitions.AnimationData data) {
        if (data == null) {
            return;
        }

        this.getModels().forEach(model -> model.model().play(data.animation().getIdentifier(), new Animator(model.model(), data)));
    }
}
