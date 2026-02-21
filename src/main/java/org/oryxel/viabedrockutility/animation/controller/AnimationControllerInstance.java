package org.oryxel.viabedrockutility.animation.controller;

import lombok.Setter;
import net.minecraft.client.model.Model;
import org.oryxel.viabedrockutility.animation.animator.Animator;
import org.oryxel.viabedrockutility.entity.CustomEntityTicker;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.mocha.LayeredScope;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import org.oryxel.viabedrockutility.mocha.OverlayBinding;
import org.oryxel.viabedrockutility.pack.definitions.AnimationDefinitions;
import org.oryxel.viabedrockutility.renderer.CustomEntityRenderer;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import net.minecraft.client.model.ModelPart;
import org.joml.Vector3f;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.util.MathUtil;

import java.io.IOException;
import java.util.*;

/**
 * Runtime instance of a Bedrock Animation Controller (state machine).
 * <p>
 * Each instance maintains:
 * <ul>
 *   <li>The current state name and reference</li>
 *   <li>A set of {@link Animator}s for the current state's animations</li>
 *   <li>Pre-parsed MoLang expressions for transitions and blend weights</li>
 * </ul>
 * <p>
 * Lifecycle per frame:
 * <ol>
 *   <li>{@link #setBaseScope} — inject frame scope into all animators</li>
 *   <li>{@link #tick} — evaluate transitions, switch state if needed, update blend weights</li>
 *   <li>{@link #animate} — apply current state's animations to each model</li>
 * </ol>
 */
public class AnimationControllerInstance {
    private final AnimationController definition;
    private final Map<String, String> entityAnimations; // entity def shortName → full anim identifier
    private final AnimationDefinitions animationDefinitions;
    private final CustomEntityTicker ticker;

    private String currentStateName;
    private AnimationController.State currentState;
    private final Map<String, Animator> stateAnimators = new LinkedHashMap<>();

    // Pre-parsed MoLang for all states' transitions (parsed once at construction)
    private final Map<String, List<ParsedTransition>> parsedTransitionsByState = new HashMap<>();

    // Pre-parsed blend weight expressions for current state's animators (rebuilt on state change)
    private final Map<String, List<Expression>> parsedBlendWeights = new HashMap<>();

    // States that are fading out during a blend_transition cross-fade
    private final List<FadingState> fadingStates = new ArrayList<>();

    @Setter
    private float controllerBlendWeight = 1.0f;

    // Per-tick cached base weights and incoming factor (used by shortest-path two-pass in animate())
    private final Map<String, Float> currentBaseWeights = new HashMap<>();
    private float lastIncomingFactor = 1.0f;

    private int debugTickCounter = 0;
    private long stateEnteredMS;

    public AnimationControllerInstance(
            AnimationController definition,
            Map<String, String> entityAnimations,
            AnimationDefinitions animationDefinitions,
            CustomEntityTicker ticker) {
        this.definition = definition;
        this.entityAnimations = entityAnimations;
        this.animationDefinitions = animationDefinitions;
        this.ticker = ticker;

        preParseAllTransitions();
        enterState(definition.getInitialState(), ticker.getEntityScope());
    }

    /**
     * Pre-parse all transition conditions for all states (done once at construction).
     */
    private void preParseAllTransitions() {
        for (Map.Entry<String, AnimationController.State> entry : definition.getStates().entrySet()) {
            final List<ParsedTransition> parsed = new ArrayList<>();
            for (AnimationController.Transition trans : entry.getValue().getTransitions()) {
                try {
                    parsed.add(new ParsedTransition(trans.targetState(), MoLangEngine.parse(trans.condition())));
                } catch (IOException e) {
                    ViaBedrockUtilityFabric.LOGGER.warn("[AnimController] Failed to parse transition condition '{}' in state '{}'",
                            trans.condition(), entry.getKey(), e);
                }
            }
            parsedTransitionsByState.put(entry.getKey(), parsed);
        }
    }

    /**
     * Inject the per-frame scope into all active animators (including fading ones).
     */
    public void setBaseScope(Scope frameScope) {
        stateAnimators.values().forEach(a -> a.setBaseScope(frameScope));
        for (FadingState fs : fadingStates) {
            fs.animators.values().forEach(a -> a.setBaseScope(frameScope));
        }
    }

    /**
     * Evaluate transitions and update blend weights. Called once per frame.
     */
    public void tick(Scope frameScope) {
        if (controllerBlendWeight <= 0 || currentState == null) {
            return;
        }

        // Build a transition scope with controller-specific animation finished queries
        final Scope transitionScope = buildTransitionScope(frameScope);

        // Debug: log current state and key query values
        if (debugTickCounter++ % 60 == 0) { // Log once per ~1 second (60fps)
            try {
                final Value variantVal = ((MutableObjectBinding) frameScope.get("query")).get("variant");
                ViaBedrockUtilityFabric.LOGGER.debug("[AnimController] {} | state='{}' | variant={} | animators={} | donePlaying={}",
                        definition.getIdentifier(), currentStateName,
                        variantVal != null ? variantVal.getAsNumber() : "null",
                        stateAnimators.size(),
                        stateAnimators.values().stream().map(Animator::isDonePlaying).toList());
            } catch (Throwable e) {
                ViaBedrockUtilityFabric.LOGGER.debug("[AnimController] {} | state='{}' | query error: {}",
                        definition.getIdentifier(), currentStateName, e.getMessage());
            }
        }

        // Evaluate transitions (first match wins)
        final List<ParsedTransition> transitions = parsedTransitionsByState.get(currentStateName);
        if (transitions != null) {
            for (ParsedTransition trans : transitions) {
                try {
                    final Value result = MoLangEngine.eval(transitionScope, trans.parsedCondition());
                    if (result.getAsBoolean()) {
                        ViaBedrockUtilityFabric.LOGGER.debug("[AnimController] {} transition: {} → {}",
                                definition.getIdentifier(), currentStateName, trans.targetState());
                        enterState(trans.targetState(), transitionScope);
                        break;
                    }
                } catch (Throwable e) {
                    ViaBedrockUtilityFabric.LOGGER.warn("[AnimController] {} transition eval error in state '{}' → '{}': {}",
                            definition.getIdentifier(), currentStateName, trans.targetState(), e.getMessage());
                }
            }
        }

        // ── 1. Update fading (outgoing) states ──
        final float totalFadingWeight = tickFadingStates(frameScope);

        // ── 2. Compute incoming factor (ensures total weight = 1.0 during cross-fade) ──
        final float incomingFactor = Math.max(0, 1.0f - totalFadingWeight);

        // ── 3. Update current state's animator weights ──
        this.lastIncomingFactor = incomingFactor;
        currentBaseWeights.clear();
        stateAnimators.forEach((animId, animator) -> {
            float base = evalBlendWeight(parsedBlendWeights, animId, frameScope);
            currentBaseWeights.put(animId, base);
            animator.setBlendWeight(base * incomingFactor * controllerBlendWeight);
        });
    }

    /**
     * Build a scope that overlays animation-finished queries on top of the frame scope.
     * These queries are per-controller, reflecting the current state's animators.
     */
    private Scope buildTransitionScope(Scope frameScope) {
        boolean anyFinished = false;
        boolean allFinished = true;

        if (stateAnimators.isEmpty()) {
            // No animations in this state — treat as immediately finished
            anyFinished = true;
        } else {
            for (Animator animator : stateAnimators.values()) {
                if (animator.isDonePlaying()) {
                    anyFinished = true;
                } else {
                    allFinished = false;
                }
            }
        }

        final LayeredScope scope = new LayeredScope(frameScope);
        final OverlayBinding queryBinding = new OverlayBinding(
                (MutableObjectBinding) frameScope.get("query"));
        queryBinding.set("any_animation_finished", Value.of(anyFinished ? 1.0 : 0.0));
        queryBinding.set("all_animations_finished", Value.of(allFinished ? 1.0 : 0.0));

        // query.anim_time in controller context = time spent in current state (seconds)
        final float stateTime = (System.currentTimeMillis() - stateEnteredMS) / 1000f;
        queryBinding.set("anim_time", Value.of(stateTime));

        scope.set("query", queryBinding);
        scope.set("q", queryBinding);
        return scope;
    }

    /**
     * Apply current state's animations (and any fading states) to a model. Called once per frame per model.
     */
    public void animate(Model model, CustomEntityRenderer.CustomEntityRenderState state) {
        if (controllerBlendWeight <= 0) {
            return;
        }

        // Find the most recent shortest-path fading state
        FadingState shortestPathFs = null;
        for (int i = fadingStates.size() - 1; i >= 0; i--) {
            if (fadingStates.get(i).blendViaShortestPath) {
                shortestPathFs = fadingStates.get(i);
                break;
            }
        }

        // Apply non-shortest-path fading states normally (additive)
        for (FadingState fs : fadingStates) {
            if (fs == shortestPathFs) continue;
            applyAnimators(fs.animators.values(), model, state);
        }

        if (shortestPathFs != null) {
            animateWithShortestPath(model, state, shortestPathFs);
        } else {
            applyAnimators(stateAnimators.values(), model, state);
        }
    }

    private void applyAnimators(Collection<Animator> animators, Model model,
                                CustomEntityRenderer.CustomEntityRenderState state) {
        for (Animator animator : animators) {
            try {
                animator.animate(model, state);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Two-pass blending with shortest rotation path for a fading state cross-fade.
     * <p>
     * Algorithm:
     * <ol>
     *   <li>Save per-bone state (rotation, offset, scale)</li>
     *   <li>Apply outgoing animators at base weight → capture per-bone deltas</li>
     *   <li>Restore bones, apply incoming animators at base weight → capture per-bone deltas</li>
     *   <li>Blend rotation via shortest path, offset/scale linearly</li>
     *   <li>Apply blended result × controllerBlendWeight</li>
     * </ol>
     */
    private void animateWithShortestPath(Model model,
                                         CustomEntityRenderer.CustomEntityRenderState renderState,
                                         FadingState outgoing) {
        @SuppressWarnings("unchecked")
        final List<ModelPart> parts = (List<ModelPart>) model.getParts();

        // Save current bone state (includes contributions from non-SP fading states applied earlier)
        final Map<ModelPart, BoneSnapshot> saved = new IdentityHashMap<>(parts.size());
        for (ModelPart part : parts) {
            saved.put(part, BoneSnapshot.capture(part));
        }

        // --- Pass 1: Outgoing at base weight ---
        setAnimatorWeights(outgoing.animators, outgoing.baseWeights, 1.0f);
        applyAnimators(outgoing.animators.values(), model, renderState);

        // Capture outgoing result, then restore bones for pass 2
        final Map<ModelPart, BoneSnapshot> afterOut = new IdentityHashMap<>(parts.size());
        for (ModelPart part : parts) {
            afterOut.put(part, BoneSnapshot.capture(part));
            saved.get(part).restore(part);
        }

        // Restore outgoing animator weights
        setAnimatorWeights(outgoing.animators, outgoing.baseWeights,
                outgoing.getCurrentWeight() * controllerBlendWeight);

        // --- Pass 2: Incoming at base weight ---
        setAnimatorWeights(stateAnimators, currentBaseWeights, 1.0f);
        applyAnimators(stateAnimators.values(), model, renderState);

        // --- Blend outgoing/incoming deltas and apply ---
        final float inFactor = lastIncomingFactor;
        for (ModelPart part : parts) {
            final BoneSnapshot s = saved.get(part);
            final BoneSnapshot out = afterOut.get(part);
            final IModelPart ip = (IModelPart) ((Object) part);
            final Vector3f rot = ip.viaBedrockUtility$getRotation();
            final Vector3f off = ip.viaBedrockUtility$getOffset();

            // Outgoing delta
            float outRx = out.rx - s.rx, outRy = out.ry - s.ry, outRz = out.rz - s.rz;
            float outOx = out.ox - s.ox, outOy = out.oy - s.oy, outOz = out.oz - s.oz;
            float outSx = out.sx - s.sx, outSy = out.sy - s.sy, outSz = out.sz - s.sz;

            // Incoming delta
            float inRx = rot.x - s.rx, inRy = rot.y - s.ry, inRz = rot.z - s.rz;
            float inOx = off.x - s.ox, inOy = off.y - s.oy, inOz = off.z - s.oz;
            float inSx = part.xScale - s.sx, inSy = part.yScale - s.sy, inSz = part.zScale - s.sz;

            // Rotation: shortest path lerp
            float bRx = outRx + MathUtil.normalizeAngleDeg(inRx - outRx) * inFactor;
            float bRy = outRy + MathUtil.normalizeAngleDeg(inRy - outRy) * inFactor;
            float bRz = outRz + MathUtil.normalizeAngleDeg(inRz - outRz) * inFactor;

            // Offset & scale: linear lerp
            float bOx = outOx + (inOx - outOx) * inFactor;
            float bOy = outOy + (inOy - outOy) * inFactor;
            float bOz = outOz + (inOz - outOz) * inFactor;
            float bSx = outSx + (inSx - outSx) * inFactor;
            float bSy = outSy + (inSy - outSy) * inFactor;
            float bSz = outSz + (inSz - outSz) * inFactor;

            // Apply: saved + blended_delta × controllerBlendWeight
            rot.set(s.rx + bRx * controllerBlendWeight,
                    s.ry + bRy * controllerBlendWeight,
                    s.rz + bRz * controllerBlendWeight);
            off.set(s.ox + bOx * controllerBlendWeight,
                    s.oy + bOy * controllerBlendWeight,
                    s.oz + bOz * controllerBlendWeight);
            part.xScale = s.sx + bSx * controllerBlendWeight;
            part.yScale = s.sy + bSy * controllerBlendWeight;
            part.zScale = s.sz + bSz * controllerBlendWeight;
        }

        // Restore incoming animator weights
        setAnimatorWeights(stateAnimators, currentBaseWeights,
                lastIncomingFactor * controllerBlendWeight);
    }

    /**
     * Set each animator's blendWeight to baseWeight × factor.
     */
    private void setAnimatorWeights(Map<String, Animator> animators,
                                    Map<String, Float> baseWeights, float factor) {
        animators.forEach((animId, animator) -> {
            Float base = baseWeights.get(animId);
            animator.setBlendWeight((base != null ? base : 1.0f) * factor);
        });
    }

    /**
     * Transition to a new state: execute on_exit, move old animators to fading list
     * (if blend_transition is set), rebuild animators, execute on_entry.
     */
    private void enterState(String stateName, Scope scope) {
        final AnimationController.State newState = definition.getStates().get(stateName);
        if (newState == null) {
            ViaBedrockUtilityFabric.LOGGER.warn("[AnimController] State '{}' not found in controller '{}'",
                    stateName, definition.getIdentifier());
            return;
        }

        // Execute on_exit of old state
        if (currentState != null) {
            executeScripts(currentState.getOnExit(), scope);

            // If old state has blend_transition, keep its animators alive for cross-fade
            final BlendTransitionCurve curve = currentState.getBlendTransitionCurve();
            if (!curve.isNone() && !stateAnimators.isEmpty()) {
                fadingStates.add(new FadingState(
                        new LinkedHashMap<>(stateAnimators),
                        new HashMap<>(parsedBlendWeights),
                        curve,
                        System.currentTimeMillis(),
                        currentState.isBlendViaShortestPath()
                ));
            }
        }

        // Clear current state animators (old ones are now in fadingStates if applicable)
        stateAnimators.clear();
        parsedBlendWeights.clear();

        // Switch state
        currentStateName = stateName;
        currentState = newState;
        stateEnteredMS = System.currentTimeMillis();

        // Create animators for new state's animations
        for (AnimationController.StateAnimation sa : currentState.getAnimations()) {
            final String animId = entityAnimations.get(sa.shortName());
            if (animId == null) {
                ViaBedrockUtilityFabric.LOGGER.debug("[AnimController] Animation short name '{}' not found in entity animations map",
                        sa.shortName());
                continue;
            }

            final AnimationDefinitions.AnimationData animData = animationDefinitions.getAnimations().get(animId);
            if (animData == null) {
                ViaBedrockUtilityFabric.LOGGER.debug("[AnimController] Animation '{}' ({}) not found in AnimationDefinitions",
                        sa.shortName(), animId);
                continue;
            }

            final Animator animator = new Animator(ticker, animData);
            stateAnimators.put(animData.animation().getIdentifier(), animator);

            // Pre-parse blend weight expression
            if (sa.blendWeightExpression() != null && !sa.blendWeightExpression().isBlank()) {
                try {
                    parsedBlendWeights.put(animData.animation().getIdentifier(),
                            MoLangEngine.parse(sa.blendWeightExpression()));
                } catch (IOException e) {
                    ViaBedrockUtilityFabric.LOGGER.warn("[AnimController] Failed to parse blend weight '{}' for animation '{}'",
                            sa.blendWeightExpression(), sa.shortName(), e);
                }
            }
        }

        // Execute on_entry of new state
        executeScripts(currentState.getOnEntry(), scope);
    }

    private void executeScripts(List<String> scripts, Scope scope) {
        for (String expr : scripts) {
            try {
                MoLangEngine.eval(scope, expr);
            } catch (Throwable e) {
                ViaBedrockUtilityFabric.LOGGER.debug("[AnimController] Failed to execute script: {}", expr, e);
            }
        }
    }

    /**
     * Evaluate a blend weight expression for an animator, returning 1.0 if no expression exists.
     */
    private float evalBlendWeight(Map<String, List<Expression>> blendWeightMap,
                                  String animId, Scope frameScope) {
        final List<Expression> expr = blendWeightMap.get(animId);
        if (expr == null) return 1.0f;
        try {
            return (float) MoLangEngine.eval(frameScope, expr).getAsNumber();
        } catch (Throwable e) {
            return 1.0f;
        }
    }

    /**
     * Update all fading (outgoing) states' blend weights, remove finished ones.
     * @return total fading weight (used to compute incoming state's weight)
     */
    private float tickFadingStates(Scope frameScope) {
        float total = 0;
        final Iterator<FadingState> it = fadingStates.iterator();
        while (it.hasNext()) {
            final FadingState fs = it.next();
            if (fs.isFinished()) {
                it.remove();
                continue;
            }
            final float fadeWeight = fs.getCurrentWeight();
            total += fadeWeight;
            fs.baseWeights.clear();
            fs.animators.forEach((animId, animator) -> {
                float base = evalBlendWeight(fs.blendWeights, animId, frameScope);
                fs.baseWeights.put(animId, base);
                animator.setBlendWeight(base * fadeWeight * controllerBlendWeight);
            });
        }
        return total;
    }

    /**
     * Pre-parsed transition condition with target state.
     */
    private record ParsedTransition(String targetState, List<Expression> parsedCondition) {}

    /**
     * Snapshot of a state's animators that are fading out during a blend_transition cross-fade.
     */
    private static final class FadingState {
        final Map<String, Animator> animators;
        final Map<String, List<Expression>> blendWeights;
        final BlendTransitionCurve curve;
        final long fadeStartMS;
        final boolean blendViaShortestPath;
        /** Per-animator base weights, computed each tick for two-pass shortest-path blending. */
        final Map<String, Float> baseWeights = new HashMap<>();

        FadingState(Map<String, Animator> animators,
                    Map<String, List<Expression>> blendWeights,
                    BlendTransitionCurve curve, long fadeStartMS,
                    boolean blendViaShortestPath) {
            this.animators = animators;
            this.blendWeights = blendWeights;
            this.curve = curve;
            this.fadeStartMS = fadeStartMS;
            this.blendViaShortestPath = blendViaShortestPath;
        }

        float getElapsed() {
            return (System.currentTimeMillis() - fadeStartMS) / 1000f;
        }

        float getCurrentWeight() {
            return curve.getOldStateWeight(getElapsed());
        }

        boolean isFinished() {
            return getCurrentWeight() <= 0;
        }
    }

    /**
     * Immutable snapshot of a bone's transform state for two-pass blending.
     */
    private record BoneSnapshot(float rx, float ry, float rz,
                                float ox, float oy, float oz,
                                float sx, float sy, float sz) {
        static BoneSnapshot capture(ModelPart part) {
            final IModelPart ip = (IModelPart) ((Object) part);
            final Vector3f rot = ip.viaBedrockUtility$getRotation();
            final Vector3f off = ip.viaBedrockUtility$getOffset();
            return new BoneSnapshot(rot.x, rot.y, rot.z, off.x, off.y, off.z,
                    part.xScale, part.yScale, part.zScale);
        }

        void restore(ModelPart part) {
            final IModelPart ip = (IModelPart) ((Object) part);
            ip.viaBedrockUtility$getRotation().set(rx, ry, rz);
            ip.viaBedrockUtility$getOffset().set(ox, oy, oz);
            part.xScale = sx;
            part.yScale = sy;
            part.zScale = sz;
        }
    }
}
