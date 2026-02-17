package org.oryxel.viabedrockutility.renderer;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.render.*;
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
//?} else {
/*import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
*///?}
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.oryxel.viabedrockutility.animation.animator.Animator;
import org.oryxel.viabedrockutility.entity.CustomEntityTicker;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.material.data.Material;
import org.oryxel.viabedrockutility.pack.definitions.AnimationDefinitions;
import org.oryxel.viabedrockutility.renderer.model.CustomEntityModel;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CustomEntityRenderer<T extends Entity> extends EntityRenderer<T, CustomEntityRenderer.CustomEntityRenderState> {
    private final Map<String, Animator> animators = new ConcurrentHashMap<>();

    private final CustomEntityTicker ticker;
    private final List<Model> models;

    public CustomEntityRenderer(final CustomEntityTicker ticker, final List<Model> models, EntityRendererFactory.Context context) {
        super(context);
        this.models = models;
        this.ticker = ticker;
    }

    //? if >=1.21.9 {
    private int renderLogCounter = 0;
    @Override
    public void render(CustomEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        if (renderLogCounter++ % 100 == 0) {
            ViaBedrockUtilityFabric.LOGGER.info("[Render] CustomEntityRenderer.render() called, models={}, animators={}", this.models.size(), this.animators.size());
        }
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-1.0F, -1.0F, 1.0F);
            matrices.translate(0.0F, -1.501F, 0.0F);
            this.animators.values().forEach(animator -> {
                try {
                    animator.animate(model.model(), state);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            try {
                RenderLayer renderLayer = model.material.info().getVariants().get("skinning_color").build().apply(model.texture);
                if (renderLayer != null) {
                    RenderCommandQueue batchQueue = queue.getBatchingQueue(0);
                    batchQueue.submitModelPart(model.model.getRootPart(), matrices, renderLayer, state.light, OverlayTexture.packUv(0, 10), null);
                } else if (renderLogCounter % 100 == 1) {
                    ViaBedrockUtilityFabric.LOGGER.warn("[Render] RenderLayer is null for model key={}, texture={}", model.key(), model.texture());
                }
            } catch (Exception e) {
                if (renderLogCounter % 100 == 1) {
                    ViaBedrockUtilityFabric.LOGGER.error("[Render] Error rendering model key={}, texture={}", model.key(), model.texture(), e);
                }
            }

            matrices.pop();
        }
    }
    //?} else {
    /*@Override
    public void render(CustomEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-1.0F, -1.0F, 1.0F);
            matrices.translate(0.0F, -1.501F, 0.0F);
            this.animators.values().forEach(animator -> {
                try {
                    animator.animate(model.model(), state);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            RenderLayer renderLayer = model.material.info().getVariants().get("skinning_color").build().apply(model.texture);
            if (renderLayer != null) {
                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
                model.model.render(matrices, vertexConsumer, light, OverlayTexture.packUv(0, 10));
            }

            matrices.pop();
        }
    }
    *///?}

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        double d = 64.0F * Entity.getRenderDistanceMultiplier();
        return entity.squaredDistanceTo(x, y, z) <= d * d;
    }

    @Override
    public void updateRenderState(T entity, CustomEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.setCustomRenderer(this);
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
        @Setter
        private CustomEntityRenderer<?> customRenderer;
    }

    public void reset() {
        this.animators.values().forEach(animator -> this.models.forEach(m -> animator.stop(m.model(), true)));
        this.animators.clear();
    }

    public void play(final AnimationDefinitions.AnimationData data) {
        if (data == null) {
            return;
        }

        this.animators.put(data.animation().getIdentifier(), new Animator(this.ticker, data));
    }
}
