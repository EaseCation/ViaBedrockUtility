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
import org.oryxel.viabedrockutility.animation.controller.AnimationControllerInstance;
import org.oryxel.viabedrockutility.config.LodConfig;
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

    // LimbAnimator-style movement tracking (per-frame accumulation + per-tick smoothing)
    private double lastPosX, lastPosY, lastPosZ;
    private float tickHorizDist;
    private float tickVertDist;
    private int lastAge = -1;
    private float limbSpeed;
    private float limbDistance;
    private float limbVerticalSpeed;

    public CustomEntityRenderer(final CustomEntityTicker ticker, final List<Model> models, EntityRendererFactory.Context context) {
        super(context);
        this.models = models;
        this.ticker = ticker;
    }

    // Use identity hash as initial offset so different entities' LOD frames are evenly distributed
    private int renderFrameCounter = System.identityHashCode(this);

    //? if >=1.21.9 {
    @Override
    public void render(CustomEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        this.renderFrameCounter++;

        // Distance-based LOD: skip animation computation for distant entities (configurable)
        boolean shouldAnimate = LodConfig.getInstance().shouldAnimate(state.getDistanceFromCamera(), this.renderFrameCounter);

        final Scope frameScope;
        if (shouldAnimate) {
            // Build per-frame scope with all query bindings, execute pre_animation, set up animators
            frameScope = this.buildFrameScope(state);
            this.ticker.runPreAnimationTask(frameScope);
            this.animators.values().forEach(a -> a.setBaseScope(frameScope));
            this.evaluateAnimationConditions(frameScope);

            // Tick animation controllers (evaluate transitions, update blend weights)
            for (AnimationControllerInstance ci : this.ticker.getControllerInstances()) {
                ci.setBaseScope(frameScope);
                ci.tick(frameScope);
            }
        } else {
            frameScope = null;
        }

        float s = (this.ticker.getScale() != null) ? this.ticker.getScale() : 1.0F;
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-s, -s, s);
            matrices.translate(0.0F, -1.501F, 0.0F);

            if (shouldAnimate) {
                this.animators.values().forEach(animator -> {
                    try {
                        animator.animate(model.model(), state);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                // Apply animation controller animations
                for (AnimationControllerInstance ci : this.ticker.getControllerInstances()) {
                    ci.animate(model.model(), state);
                }

                // Apply part_visibility
                if (model.controller() != null && !model.controller().partVisibility().isEmpty()) {
                    applyPartVisibility(model.model(), model.controller().partVisibility(), frameScope);
                }
            }

            try {
                Material.MaterialInfo.Variant skinningColor = model.material.info().getVariants().get("skinning_color");
                RenderLayer renderLayer = skinningColor.build().apply(model.texture);
                if (renderLayer != null) {
                    int effectiveLight = state.light;
                    if ((model.controller() != null && model.controller().ignoreLighting())
                            || skinningColor.getDefines().contains("USE_EMISSIVE")) {
                        effectiveLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
                    }

                    RenderCommandQueue batchQueue = queue.getBatchingQueue(0);
                    batchQueue.submitModelPart(model.model.getRootPart(), matrices, renderLayer, effectiveLight, OverlayTexture.packUv(0, 10), null);
                }
            } catch (StackOverflowError soe) {
                ViaBedrockUtilityFabric.LOGGER.error(
                        "[VBU] StackOverflow rendering model! key='{}', geometry='{}', texture='{}'",
                        model.key(), model.geometry(), model.texture());
                dumpModelHierarchy(model.model().getRootPart(), "", 0);
            } catch (Exception e) {
                ViaBedrockUtilityFabric.LOGGER.debug("[Render] Error rendering model key={}, texture={}", model.key(), model.texture(), e);
            }

            matrices.pop();
        }
        super.render(state, matrices, queue, cameraState);
    }
    //?} else {
    /*@Override
    public void render(CustomEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        this.renderFrameCounter++;

        boolean shouldAnimate = LodConfig.getInstance().shouldAnimate(state.getDistanceFromCamera(), this.renderFrameCounter);

        final Scope frameScope;
        if (shouldAnimate) {
            frameScope = this.buildFrameScope(state);
            this.ticker.runPreAnimationTask(frameScope);
            this.animators.values().forEach(a -> a.setBaseScope(frameScope));
            this.evaluateAnimationConditions(frameScope);

            for (AnimationControllerInstance ci : this.ticker.getControllerInstances()) {
                ci.setBaseScope(frameScope);
                ci.tick(frameScope);
            }
        } else {
            frameScope = null;
        }

        float s = (this.ticker.getScale() != null) ? this.ticker.getScale() : 1.0F;
        for (Model model : this.models) {
            matrices.push();

            this.setupTransforms(state, matrices);
            matrices.scale(-s, -s, s);
            matrices.translate(0.0F, -1.501F, 0.0F);

            if (shouldAnimate) {
                this.animators.values().forEach(animator -> {
                    try {
                        animator.animate(model.model(), state);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                for (AnimationControllerInstance ci : this.ticker.getControllerInstances()) {
                    ci.animate(model.model(), state);
                }

                if (model.controller() != null && !model.controller().partVisibility().isEmpty()) {
                    applyPartVisibility(model.model(), model.controller().partVisibility(), frameScope);
                }
            }

            try {
                Material.MaterialInfo.Variant skinningColor = model.material.info().getVariants().get("skinning_color");
                RenderLayer renderLayer = skinningColor.build().apply(model.texture);
                if (renderLayer != null) {
                    int effectiveLight = light;
                    if ((model.controller() != null && model.controller().ignoreLighting())
                            || skinningColor.getDefines().contains("USE_EMISSIVE")) {
                        effectiveLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
                    }

                    VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
                    model.model.render(matrices, vertexConsumer, effectiveLight, OverlayTexture.packUv(0, 10));
                }
            } catch (StackOverflowError soe) {
                ViaBedrockUtilityFabric.LOGGER.error(
                        "[VBU] StackOverflow rendering model! key='{}', geometry='{}', texture='{}'",
                        model.key(), model.geometry(), model.texture());
                dumpModelHierarchy(model.model().getRootPart(), "", 0);
            } catch (Exception e) {
                ViaBedrockUtilityFabric.LOGGER.debug("[Render] Error rendering model key={}, texture={}", model.key(), model.texture(), e);
            }

            matrices.pop();
        }
        super.render(state, matrices, vertexConsumers, light);
    }
    *///?}

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        if (!frustum.isVisible(getVisualBoundingBox(entity))) return false;
        double d = 64.0F * Entity.getRenderDistanceMultiplier();
        return entity.squaredDistanceTo(x, y, z) <= d * d;
    }

    /**
     * Constructs a bounding box from geometry visible_bounds (width/height/offset) and entity scale,
     * rather than the entity's collision box which may be much smaller than the rendered model.
     */
    private net.minecraft.util.math.Box getVisualBoundingBox(T entity) {
        float scale = (this.ticker.getScale() != null) ? this.ticker.getScale() : 1.0f;

        // Find the largest visible bounds across all models
        float maxHalfWidth = 0.5f;
        float maxHeight = 2.0f;
        float offsetY = 1.0f;
        for (Model model : this.models) {
            var vb = model.visibleBounds();
            if (vb != null) {
                maxHalfWidth = Math.max(maxHalfWidth, vb.width() / 2.0f);
                maxHeight = Math.max(maxHeight, vb.height());
                offsetY = vb.offsetY();
            }
        }

        maxHalfWidth *= scale;
        maxHeight *= scale;
        offsetY *= scale;

        double ex = entity.getX();
        double ey = entity.getY();
        double ez = entity.getZ();
        double cy = ey + offsetY; // center Y of the visible bounds
        return new net.minecraft.util.math.Box(
                ex - maxHalfWidth, cy - maxHeight / 2.0, ez - maxHalfWidth,
                ex + maxHalfWidth, cy + maxHeight / 2.0, ez + maxHalfWidth
        );
    }

    @Override
    public void updateRenderState(T entity, CustomEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.setCustomRenderer(this);
        float serverYaw = entity.getYaw(tickDelta);
        float serverPitch = entity.getPitch(tickDelta);
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

        // LimbAnimator-style movement tracking.
        // InteractionEntityMixin enables PositionInterpolator, so getLerpedPos() now
        // returns properly interpolated positions (3-tick lerp, smooth within each tick).
        final Vec3d pos = entity.getLerpedPos(tickDelta);

        if (this.lastAge < 0) {
            this.lastPosX = pos.x;
            this.lastPosY = pos.y;
            this.lastPosZ = pos.z;
            this.lastAge = entity.age;
        }

        final double mx = pos.x - this.lastPosX;
        final double my = pos.y - this.lastPosY;
        final double mz = pos.z - this.lastPosZ;
        this.tickHorizDist += (float) Math.sqrt(mx * mx + mz * mz);
        this.tickVertDist += (float) my;

        this.lastPosX = pos.x;
        this.lastPosY = pos.y;
        this.lastPosZ = pos.z;

        // Per-tick: LimbAnimator 0.4 exponential smoothing
        if (entity.age != this.lastAge) {
            this.limbSpeed += (this.tickHorizDist - this.limbSpeed) * 0.4f;
            this.limbDistance += this.limbSpeed;
            this.limbVerticalSpeed += (this.tickVertDist - this.limbVerticalSpeed) * 0.4f;

            this.tickHorizDist = 0;
            this.tickVertDist = 0;
            this.lastAge = entity.age;
        }

        state.distanceTraveled = this.limbDistance;
        state.setPositionDeltaX(mx);
        state.setPositionDeltaY(my);
        state.setPositionDeltaZ(mz);
        state.setGroundSpeed(this.limbSpeed * 20.0f);
        state.setVerticalSpeed(this.limbVerticalSpeed * 20.0f);

        // Calculate rotation_to_camera for billboard effect
        final var camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        //? if >=1.21.6 {
        final Vec3d cameraPos = camera.getCameraPos();
        //?} else {
        /*final Vec3d cameraPos = camera.getPos();
        *///?}
        final Vec3d camEntityPos = entity.getLerpedPos(tickDelta);
        final double cdx = cameraPos.x - camEntityPos.x;
        final double cdy = cameraPos.y - camEntityPos.y;
        final double cdz = cameraPos.z - camEntityPos.z;
        final double cameraHorizontalDist = Math.sqrt(cdx * cdx + cdz * cdz);
        state.setRotationToCameraX(-(float) Math.toDegrees(Math.atan2(cdy, cameraHorizontalDist)));
        state.setRotationToCameraY(-(float) (Math.toDegrees(Math.atan2(cdx, cdz)) + state.bodyYaw - 180.0));

        state.setDistanceFromCamera(Math.sqrt(cdx * cdx + cdy * cdy + cdz * cdz));
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

    public record Model(String key, String geometry, CustomEntityModel<CustomEntityRenderState> model, Identifier texture, Material material, BedrockRenderController controller,
                        org.oryxel.viabedrockutility.pack.definitions.VisibleBounds visibleBounds) {
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
        boolean anyHidden = !defaultVisible;

        final Map<String, List<ModelPart>> partsByName = entityModel.getPartsByName();
        for (Map.Entry<String, String> entry : pv.entrySet()) {
            if ("*".equals(entry.getKey())) continue;
            List<ModelPart> matchingParts = partsByName.get(entry.getKey());
            if (matchingParts != null) {
                boolean vis = evalVisibility(entry.getValue(), scope);
                if (!vis) anyHidden = true;
                for (ModelPart part : matchingParts) {
                    part.visible = vis;
                }
            }
        }

        // Post-pass: only needed when some parts are hidden.
        // Java MC's ModelPart.visible is hierarchical — if a parent is invisible,
        // all children are hidden too. This bottom-up pass ensures ancestors of
        // visible parts remain visible.
        if (anyHidden) {
            ensureAncestorsVisible(entityModel.getRootPart());
        }
    }

    /**
     * Bottom-up recursive pass: if any descendant has visible=true,
     * this part must also be visible=true to allow rendering traversal
     * to reach the visible descendant.
     */
    private boolean ensureAncestorsVisible(ModelPart part) {
        return ensureAncestorsVisibleImpl(part, 0);
    }

    private boolean ensureAncestorsVisibleImpl(ModelPart part, int depth) {
        if (depth > 200) {
            ViaBedrockUtilityFabric.LOGGER.error("[VBU] ensureAncestorsVisible depth > 200 — likely cycle! bone='{}'",
                    ((IModelPart) ((Object) part)).viaBedrockUtility$getName());
            return false;
        }
        boolean anyChildVisible = false;
        for (ModelPart child : ((IModelPart) ((Object) part)).viaBedrockUtility$getChildren().values()) {
            if (ensureAncestorsVisibleImpl(child, depth + 1)) {
                anyChildVisible = true;
            }
        }
        if (anyChildVisible) {
            part.visible = true;
        }
        return part.visible;
    }

    /**
     * Dumps ModelPart tree hierarchy to log for debugging cyclic references.
     * Uses IdentityHashSet to detect and report cycles.
     */
    private void dumpModelHierarchy(ModelPart part, String indent, int depth) {
        if (depth > 20) {
            ViaBedrockUtilityFabric.LOGGER.error("{}... (truncated at depth 20)", indent);
            return;
        }
        String name = ((IModelPart) ((Object) part)).viaBedrockUtility$getName();
        boolean isVBU = ((IModelPart) ((Object) part)).viaBedrockUtility$isVBUModel();
        Map<String, ModelPart> children = ((IModelPart) ((Object) part)).viaBedrockUtility$getChildren();
        ViaBedrockUtilityFabric.LOGGER.error("{}bone='{}' isVBU={} children={} identity={}",
                indent, name, isVBU, children.size(), System.identityHashCode(part));
        for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
            dumpModelHierarchy(entry.getValue(), indent + "  ", depth + 1);
        }
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
