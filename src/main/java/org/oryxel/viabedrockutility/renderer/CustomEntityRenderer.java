package org.oryxel.viabedrockutility.renderer;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
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
import net.minecraft.util.math.Vec3d;
import org.cube.converter.data.bedrock.controller.BedrockRenderController;
import org.oryxel.viabedrockutility.animation.animator.Animator;
import org.oryxel.viabedrockutility.entity.CustomEntityTicker;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.material.data.Material;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import org.oryxel.viabedrockutility.pack.definitions.AnimationDefinitions;
import org.oryxel.viabedrockutility.renderer.model.CustomEntityModel;
import team.unnamed.mocha.runtime.Scope;

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
        if (renderLogCounter++ % 200 == 0) {
            StringBuilder sb = new StringBuilder();
            for (Model m : this.models) {
                sb.append(m.key()).append("(mat=").append(m.material().identifier()).append(") ");
            }
            ViaBedrockUtilityFabric.LOGGER.info("[Render] render() models={} animators={} keys=[{}]", this.models.size(), this.animators.size(), sb.toString().trim());
        }
        float s = (this.ticker.getScale() != null) ? this.ticker.getScale() : 1.0F;
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-s, -s, s);
            matrices.translate(0.0F, -1.501F, 0.0F);
            // Evaluate animate conditions per-frame and update blend weights
            final Scope conditionScope = this.ticker.getLastExecutionScope();
            if (conditionScope != null) {
                this.animators.forEach((animId, animator) -> {
                    String condition = this.ticker.getAnimationIdToCondition().get(animId);
                    if (condition != null && !condition.isBlank()) {
                        try {
                            float weight = (float) MoLangEngine.eval(conditionScope, condition).getAsNumber();
                            animator.setBlendWeight(weight);
                        } catch (Throwable e) {
                            animator.setBlendWeight(0);
                        }
                    } else {
                        animator.setBlendWeight(1.0f);
                    }
                });
            }

            this.animators.values().forEach(animator -> {
                try {
                    animator.animate(model.model(), state);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Apply part_visibility
            if (model.controller() != null && !model.controller().partVisibility().isEmpty()) {
                final Scope scope = this.ticker.getLastExecutionScope();
                if (scope != null) {
                    applyPartVisibility(model.model(), model.controller().partVisibility(), scope);
                }
            }

            try {
                RenderLayer renderLayer = model.material.info().getVariants().get("skinning_color").build().apply(model.texture);
                if (renderLayer != null) {
                    // Apply lighting properties
                    int effectiveLight = state.light;
                    if (model.controller() != null && model.controller().ignoreLighting()) {
                        effectiveLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
                    }

                    RenderCommandQueue batchQueue = queue.getBatchingQueue(0);
                    batchQueue.submitModelPart(model.model.getRootPart(), matrices, renderLayer, effectiveLight, OverlayTexture.packUv(0, 10), null);
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
        float s = (this.ticker.getScale() != null) ? this.ticker.getScale() : 1.0F;
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-s, -s, s);
            matrices.translate(0.0F, -1.501F, 0.0F);

            // Evaluate animate conditions per-frame and update blend weights
            final Scope conditionScope = this.ticker.getLastExecutionScope();
            if (conditionScope != null) {
                this.animators.forEach((animId, animator) -> {
                    String condition = this.ticker.getAnimationIdToCondition().get(animId);
                    if (condition != null && !condition.isBlank()) {
                        try {
                            float weight = (float) MoLangEngine.eval(conditionScope, condition).getAsNumber();
                            animator.setBlendWeight(weight);
                        } catch (Throwable e) {
                            animator.setBlendWeight(0);
                        }
                    } else {
                        animator.setBlendWeight(1.0f);
                    }
                });
            }

            this.animators.values().forEach(animator -> {
                try {
                    animator.animate(model.model(), state);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Apply part_visibility
            if (model.controller() != null && !model.controller().partVisibility().isEmpty()) {
                final Scope scope = this.ticker.getLastExecutionScope();
                if (scope != null) {
                    applyPartVisibility(model.model(), model.controller().partVisibility(), scope);
                }
            }

            RenderLayer renderLayer = model.material.info().getVariants().get("skinning_color").build().apply(model.texture);
            if (renderLayer != null) {
                // Apply lighting properties
                int effectiveLight = light;
                if (model.controller() != null && model.controller().ignoreLighting()) {
                    effectiveLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
                }

                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
                model.model.render(matrices, vertexConsumer, effectiveLight, OverlayTexture.packUv(0, 10));
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

        // Calculate rotation_to_camera for billboard effect
        final var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        //? if >=1.21.6 {
        final Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*final Vec3d cameraPos = camera.getPos();
        *///?}
        final Vec3d entityPos = entity.getLerpedPos(tickDelta);
        final double dx = cameraPos.x - entityPos.x;
        final double dy = cameraPos.y - entityPos.y;
        final double dz = cameraPos.z - entityPos.z;
        final double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        state.setRotationToCameraX(-(float) Math.toDegrees(Math.atan2(dy, horizontalDist)));
        state.setRotationToCameraY(-(float) (Math.toDegrees(Math.atan2(dx, dz)) + state.yaw - 180.0));
    }

    private void setupTransforms(CustomEntityRenderState state, MatrixStack matrices) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - state.yaw));
    }

    @Override
    public CustomEntityRenderState createRenderState() {
        return new CustomEntityRenderState();
    }

    public record Model(String key, String geometry, CustomEntityModel<CustomEntityRenderState> model, Identifier texture, Material material, BedrockRenderController controller) {
    }

    @Getter
    public static class CustomEntityRenderState extends EntityRenderState {
        private float yaw, bodyYaw, bodyPitch;
        private float distanceTraveled;
        @Setter
        private CustomEntityRenderer<?> customRenderer;
        @Setter
        private float rotationToCameraX;
        @Setter
        private float rotationToCameraY;
    }

    private void applyPartVisibility(CustomEntityModel<?> entityModel, Map<String, String> pv, Scope scope) {
        final List<ModelPart> allParts = entityModel.getParts();

        // Determine default visibility from wildcard rule
        boolean defaultVisible = true;
        final String defaultRule = pv.get("*");
        if (defaultRule != null) {
            defaultVisible = evalVisibility(defaultRule, scope);
        }

        // Set default visibility for all parts
        for (ModelPart part : allParts) {
            part.visible = defaultVisible;
        }

        // Override specific bones — set ALL parts with matching name (no break)
        // In VBU's model hierarchy, each bone produces both a bone ModelPart and
        // a cube wrapper ModelPart sharing the same name; both must be updated.
        for (Map.Entry<String, String> entry : pv.entrySet()) {
            if ("*".equals(entry.getKey())) continue;
            boolean vis = evalVisibility(entry.getValue(), scope);
            for (ModelPart part : allParts) {
                final String name = ((IModelPart) ((Object) part)).viaBedrockUtility$getName();
                if (name.equals(entry.getKey())) {
                    part.visible = vis;
                }
            }
        }

        // Post-pass: ensure ancestors of visible parts are also visible.
        // Java MC's ModelPart.visible is hierarchical — if a parent is invisible,
        // all children are hidden too. Bedrock's part_visibility only controls
        // per-bone cuboid rendering without affecting children. This bottom-up
        // pass bridges the semantic difference.
        ensureAncestorsVisible(entityModel.getRootPart());
    }

    /**
     * Bottom-up recursive pass: if any descendant has visible=true,
     * this part must also be visible=true to allow rendering traversal
     * to reach the visible descendant.
     */
    private boolean ensureAncestorsVisible(ModelPart part) {
        boolean anyChildVisible = false;
        for (ModelPart child : ((IModelPart) ((Object) part)).viaBedrockUtility$getChildren().values()) {
            if (ensureAncestorsVisible(child)) {
                anyChildVisible = true;
            }
        }
        if (anyChildVisible) {
            part.visible = true;
        }
        return part.visible;
    }

    private boolean evalVisibility(String expression, Scope scope) {
        if ("false".equals(expression)) return false;
        if ("true".equals(expression)) return true;
        try {
            return MoLangEngine.eval(scope, expression).getAsBoolean();
        } catch (Throwable e) {
            return true;
        }
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
