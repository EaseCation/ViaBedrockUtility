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

    @Setter
    private float controllerBlendWeight = 1.0f;

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
     * Inject the per-frame scope into all active animators.
     */
    public void setBaseScope(Scope frameScope) {
        stateAnimators.values().forEach(a -> a.setBaseScope(frameScope));
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
                ViaBedrockUtilityFabric.LOGGER.info("[AnimController] {} | state='{}' | variant={} | animators={} | donePlaying={}",
                        definition.getIdentifier(), currentStateName,
                        variantVal != null ? variantVal.getAsNumber() : "null",
                        stateAnimators.size(),
                        stateAnimators.values().stream().map(Animator::isDonePlaying).toList());
            } catch (Throwable e) {
                ViaBedrockUtilityFabric.LOGGER.info("[AnimController] {} | state='{}' | query error: {}",
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
                        ViaBedrockUtilityFabric.LOGGER.info("[AnimController] {} transition: {} → {}",
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

        // Update blend weights for current state's animators
        stateAnimators.forEach((animId, animator) -> {
            final List<Expression> bwExpr = parsedBlendWeights.get(animId);
            if (bwExpr != null) {
                try {
                    float weight = (float) MoLangEngine.eval(frameScope, bwExpr).getAsNumber();
                    animator.setBlendWeight(weight * controllerBlendWeight);
                } catch (Throwable e) {
                    animator.setBlendWeight(controllerBlendWeight);
                }
            } else {
                animator.setBlendWeight(controllerBlendWeight);
            }
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
     * Apply current state's animations to a model. Called once per frame per model.
     */
    public void animate(Model model, CustomEntityRenderer.CustomEntityRenderState state) {
        if (controllerBlendWeight <= 0) {
            return;
        }

        for (Animator animator : stateAnimators.values()) {
            try {
                animator.animate(model, state);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Transition to a new state: execute on_exit, rebuild animators, execute on_entry.
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
        }

        // Clear old animators
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
     * Pre-parsed transition condition with target state.
     */
    private record ParsedTransition(String targetState, List<Expression> parsedCondition) {}
}
