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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.cube.converter.data.bedrock.controller.BedrockRenderController;
import org.oryxel.viabedrockutility.animation.animator.Animator;
import org.oryxel.viabedrockutility.entity.CustomEntityTicker;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.material.data.Material;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.mocha.LayeredScope;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import org.oryxel.viabedrockutility.pack.definitions.AnimationDefinitions;
import org.oryxel.viabedrockutility.renderer.model.CustomEntityModel;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CustomEntityRenderer<T extends Entity> extends EntityRenderer<T, CustomEntityRenderer.CustomEntityRenderState> {
    private final Map<String, Animator> animators = new ConcurrentHashMap<>();

    private final CustomEntityTicker ticker;
    private final List<Model> models;

    // Bedrock-style head/body rotation simulation (per-entity)
    private float smoothedHeadYaw;
    private float smoothedHeadPitch;
    private float simulatedBodyYaw;
    private float lastSignificantHeadYaw;  // last head yaw that triggered delay reset
    private long headStableStartMs;         // when head last became "stable"
    private boolean rotationInitialized;

    // Per-tick movement tracking with exponential smoothing (like Java LimbAnimator)
    private int lastEntityAge = -1;
    private float smoothedSpeed;       // blocks/tick, smoothed
    private float smoothedVerticalSpeed; // blocks/tick, smoothed
    private float smoothedDistance;     // accumulated smoothed distance in blocks

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

        // Build per-frame scope with all query bindings, execute pre_animation, set up animators
        final Scope frameScope = this.buildFrameScope(state);
        this.ticker.runPreAnimationTask(frameScope);
        this.animators.values().forEach(a -> a.setBaseScope(frameScope));
        this.evaluateAnimationConditions(frameScope);

        float s = (this.ticker.getScale() != null) ? this.ticker.getScale() : 1.0F;
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-s, -s, s);
            matrices.translate(0.0F, -1.501F, 0.0F);

            this.animators.values().forEach(animator -> {
                try {
                    animator.animate(model.model(), state);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Apply part_visibility
            if (model.controller() != null && !model.controller().partVisibility().isEmpty()) {
                applyPartVisibility(model.model(), model.controller().partVisibility(), frameScope);
            }

            try {
                RenderLayer renderLayer = model.material.info().getVariants().get("skinning_color").build().apply(model.texture);
                if (renderLayer != null) {
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
        super.render(state, matrices, queue, cameraState);
    }
    //?} else {
    /*@Override
    public void render(CustomEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Build per-frame scope with all query bindings, execute pre_animation, set up animators
        final Scope frameScope = this.buildFrameScope(state);
        this.ticker.runPreAnimationTask(frameScope);
        this.animators.values().forEach(a -> a.setBaseScope(frameScope));
        this.evaluateAnimationConditions(frameScope);

        float s = (this.ticker.getScale() != null) ? this.ticker.getScale() : 1.0F;
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-s, -s, s);
            matrices.translate(0.0F, -1.501F, 0.0F);

            this.animators.values().forEach(animator -> {
                try {
                    animator.animate(model.model(), state);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Apply part_visibility
            if (model.controller() != null && !model.controller().partVisibility().isEmpty()) {
                applyPartVisibility(model.model(), model.controller().partVisibility(), frameScope);
            }

            RenderLayer renderLayer = model.material.info().getVariants().get("skinning_color").build().apply(model.texture);
            if (renderLayer != null) {
                int effectiveLight = light;
                if (model.controller() != null && model.controller().ignoreLighting()) {
                    effectiveLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
                }

                VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
                model.model.render(matrices, vertexConsumer, effectiveLight, OverlayTexture.packUv(0, 10));
            }

            matrices.pop();
        }
        super.render(state, matrices, vertexConsumers, light);
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
        float serverYaw = entity.getYaw(tickDelta);
        float serverPitch = entity.getPitch(tickDelta);
        state.distanceTraveled = entity.distanceTraveled;

        // Initialize on first frame
        if (!this.rotationInitialized) {
            this.smoothedHeadYaw = serverYaw;
            this.smoothedHeadPitch = serverPitch;
            this.simulatedBodyYaw = serverYaw;
            this.lastSignificantHeadYaw = serverYaw;
            this.headStableStartMs = System.currentTimeMillis();
            this.rotationInitialized = true;
        }

        // --- Head smoothing: fast interpolation towards server values ---
        this.smoothedHeadYaw += MathHelper.wrapDegrees(serverYaw - this.smoothedHeadYaw) * 0.5F;
        this.smoothedHeadPitch += (serverPitch - this.smoothedHeadPitch) * 0.5F;

        // --- Body delay mechanism (based on vanilla BodyControl) ---
        // Max allowed head-body angle (vanilla: getMaxHeadRotation() = 75 for most mobs)
        float maxHeadRot = 50.0F;
        long now = System.currentTimeMillis();

        if (Math.abs(MathHelper.wrapDegrees(this.smoothedHeadYaw - this.lastSignificantHeadYaw)) > 15.0F) {
            // Head moved significantly: restart delay, clamp body within range
            this.lastSignificantHeadYaw = this.smoothedHeadYaw;
            this.headStableStartMs = now;
            this.simulatedBodyYaw = MathHelper.clampAngle(this.simulatedBodyYaw, this.smoothedHeadYaw, maxHeadRot);
        } else {
            long elapsedMs = now - this.headStableStartMs;
            if (elapsedMs > 500) {
                // After 0.5s delay: gradually reduce allowed body-head angle over 0.5s
                float progress = MathHelper.clamp((elapsedMs - 500) / 500.0F, 0.0F, 1.0F);
                float maxAngle = maxHeadRot * (1.0F - progress);
                this.simulatedBodyYaw = MathHelper.clampAngle(this.simulatedBodyYaw, this.smoothedHeadYaw, maxAngle);
            }
        }

        state.yaw = this.smoothedHeadYaw;
        state.bodyYaw = this.simulatedBodyYaw;
        state.bodyPitch = this.smoothedHeadPitch;

        // target_x_rotation = head pitch (body pitch ≈ 0 for standing entities)
        // target_y_rotation = head yaw - body yaw (from body delay mechanism)
        state.setTargetXRotation(state.bodyPitch);
        state.setTargetYRotation(MathHelper.wrapDegrees(state.yaw - state.bodyYaw));

        // Per-frame entity state for MoLang queries
        state.setEntityOnGround(entity.isOnGround());
        state.setEntityAlive(entity.isAlive());
        state.setEntityLifeTime(entity.age / 20.0f);

        // Per-tick movement tracking with exponential smoothing (like Java LimbAnimator).
        // Only update once per tick (when entity.age changes) to avoid per-frame spikes.
        // INTERACTION/ITEM_DISPLAY entities don't receive velocity packets, so we compute
        // speed from lastX/X deltas which are updated per server tick.
        if (entity.age != this.lastEntityAge) {
            final double dx = entity.getX() - entity.lastX;
            final double dy = entity.getY() - entity.lastY;
            final double dz = entity.getZ() - entity.lastZ;
            final float tickMovement = (float) Math.sqrt(dx * dx + dz * dz);

            // Exponential smoothing (factor 0.4, same as Java LimbAnimator)
            this.smoothedSpeed += (tickMovement - this.smoothedSpeed) * 0.4f;
            this.smoothedDistance += this.smoothedSpeed;
            this.smoothedVerticalSpeed += ((float) dy - this.smoothedVerticalSpeed) * 0.4f;

            this.lastEntityAge = entity.age;
        }

        state.distanceTraveled = this.smoothedDistance;
        state.setPositionDeltaX(entity.getX() - entity.lastX);
        state.setPositionDeltaY(entity.getY() - entity.lastY);
        state.setPositionDeltaZ(entity.getZ() - entity.lastZ);
        // Convert blocks/tick to blocks/second for MoLang query values
        state.setGroundSpeed(this.smoothedSpeed * 20.0f);
        state.setVerticalSpeed(this.smoothedVerticalSpeed * 20.0f);

        // Calculate rotation_to_camera for billboard effect
        final var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        //? if >=1.21.6 {
        final Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*final Vec3d cameraPos = camera.getPos();
        *///?}
        final Vec3d camEntityPos = entity.getLerpedPos(tickDelta);
        final double dx = cameraPos.x - camEntityPos.x;
        final double dy = cameraPos.y - camEntityPos.y;
        final double dz = cameraPos.z - camEntityPos.z;
        final double cameraHorizontalDist = Math.sqrt(dx * dx + dz * dz);
        state.setRotationToCameraX(-(float) Math.toDegrees(Math.atan2(dy, cameraHorizontalDist)));
        state.setRotationToCameraY(-(float) (Math.toDegrees(Math.atan2(dx, dz)) + state.bodyYaw - 180.0));

        state.setDistanceFromCamera(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    private void setupTransforms(CustomEntityRenderState state, MatrixStack matrices) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - state.bodyYaw));
    }

    /**
     * Builds a per-frame scope with complete query bindings (entity-level + per-frame).
     * Uses LayeredScope on top of entityScope to avoid expensive deep copy.
     */
    private Scope buildFrameScope(CustomEntityRenderState state) {
        final LayeredScope frameScope = new LayeredScope(this.ticker.getEntityScope());

        final MutableObjectBinding queryBinding = new MutableObjectBinding();

        // Entity-level queries (variant, flags) from ticker
        this.ticker.populateEntityQueries(queryBinding);

        // Per-frame movement and state queries
        queryBinding.set("modified_distance_moved", Value.of(state.getDistanceTraveled()));
        queryBinding.set("modified_move_speed", Value.of(state.getGroundSpeed()));
        queryBinding.set("is_on_ground", Value.of(state.isEntityOnGround()));
        queryBinding.set("is_alive", Value.of(state.isEntityAlive()));
        queryBinding.set("life_time", Value.of(state.getEntityLifeTime()));
        queryBinding.set("ground_speed", Value.of(state.getGroundSpeed()));
        queryBinding.set("vertical_speed", Value.of(state.getVerticalSpeed()));
        queryBinding.set("distance_from_camera", Value.of(state.getDistanceFromCamera()));

        // Rotation queries
        queryBinding.set("body_y_rotation", Value.of(state.getBodyYaw()));
        queryBinding.set("body_x_rotation", Value.of(state.getBodyPitch()));
        queryBinding.set("target_x_rotation", Value.of(state.getTargetXRotation()));
        queryBinding.set("target_y_rotation", Value.of(state.getTargetYRotation()));
        queryBinding.set("head_x_rotation", Value.of(state.getTargetXRotation()));
        queryBinding.set("head_y_rotation", Value.of(state.getTargetYRotation()));

        // Function-type queries
        queryBinding.setFunction("position_delta", (double arg) -> {
            int axis = (int) arg;
            if (axis == 0) return state.getPositionDeltaX();
            if (axis == 1) return state.getPositionDeltaY();
            if (axis == 2) return state.getPositionDeltaZ();
            return 0.0;
        });
        queryBinding.setFunction("rotation_to_camera", (double arg) -> {
            if ((int) arg == 0) return (double) state.getRotationToCameraX();
            if ((int) arg == 1) return (double) state.getRotationToCameraY();
            return 0.0;
        });

        frameScope.set("query", queryBinding);
        frameScope.set("q", queryBinding);

        return frameScope;
    }

    /**
     * Evaluates animation blend weight conditions using pre-parsed expressions.
     */
    private void evaluateAnimationConditions(final Scope frameScope) {
        this.animators.forEach((animId, animator) -> {
            final List<Expression> parsedCondition = this.ticker.getParsedAnimationConditions().get(animId);
            if (parsedCondition != null) {
                try {
                    float weight = (float) MoLangEngine.eval(frameScope, parsedCondition).getAsNumber();
                    animator.setBlendWeight(weight);
                } catch (Throwable e) {
                    animator.setBlendWeight(0);
                }
            } else {
                animator.setBlendWeight(1.0f);
            }
        });
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
        @Setter
        private float targetXRotation;
        @Setter
        private float targetYRotation;
        // Per-frame entity state for MoLang queries
        @Setter private boolean entityOnGround;
        @Setter private boolean entityAlive;
        @Setter private float entityLifeTime;
        @Setter private double positionDeltaX;
        @Setter private double positionDeltaY;
        @Setter private double positionDeltaZ;
        @Setter private float groundSpeed;
        @Setter private float verticalSpeed;
        @Setter private double distanceFromCamera;
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

        // Override specific bones using name index — O(pv.size()) instead of O(pv.size() × allParts.size())
        // In VBU's model hierarchy, each bone produces both a bone ModelPart and
        // a cube wrapper ModelPart sharing the same name; both must be updated.
        final Map<String, List<ModelPart>> partsByName = entityModel.getPartsByName();
        for (Map.Entry<String, String> entry : pv.entrySet()) {
            if ("*".equals(entry.getKey())) continue;
            List<ModelPart> matchingParts = partsByName.get(entry.getKey());
            if (matchingParts != null) {
                boolean vis = evalVisibility(entry.getValue(), scope);
                for (ModelPart part : matchingParts) {
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
