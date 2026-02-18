package org.oryxel.viabedrockutility.animation.animator;

import lombok.Setter;
import net.minecraft.client.model.Model;
import org.joml.Vector3f;
import org.oryxel.viabedrockutility.animation.vanilla.AnimationHelper;
import org.oryxel.viabedrockutility.entity.CustomEntityTicker;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.mocha.LayeredScope;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import org.oryxel.viabedrockutility.pack.definitions.AnimationDefinitions;
import org.oryxel.viabedrockutility.renderer.CustomEntityRenderer;
import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Animator {
    private final CustomEntityTicker ticker;
    private final AnimationDefinitions.AnimationData data;

    private long animationStartMS;

    private boolean donePlaying, started, firstPlay;

    private final Vector3f TEMP_VEC = new Vector3f();

    @Setter
    private Scope baseScope;

    @Setter
    private float blendWeight = 1.0f;

    public Animator(CustomEntityTicker ticker, AnimationDefinitions.AnimationData data) {
        this.ticker = ticker;
        this.data = data;

        this.animationStartMS = System.currentTimeMillis();
        this.firstPlay = true;
    }

    public void animate(Model model, CustomEntityRenderer.CustomEntityRenderState state) throws IOException {
        if (this.blendWeight <= 0) {
            return;
        }

        if (this.donePlaying) {
            if (this.data.animation().getLoop().getValue().equals(true)) {
                this.donePlaying = false;
            } else {
                return;
            }
        }

        if (this.baseScope == null) {
            return;
        }

        // baseScope already contains complete query bindings from buildFrameScope().
        // Only overlay animation-specific anim_time/life_time.
        final Scope scope = new LayeredScope(this.baseScope);

        if (!this.started) {
            boolean skipThisTick = true;

            float seconds = (System.currentTimeMillis() - this.animationStartMS) / 1000F;
            double requiredLaunchTime = MoLangEngine.eval(scope, this.firstPlay ? this.data.animation().getStartDelay() : this.data.animation().getLoopDelay()).getAsNumber();
            if (seconds >= requiredLaunchTime) {
                skipThisTick = false;
                this.started = true;
                this.firstPlay = false;

                this.animationStartMS = System.currentTimeMillis();
            }

            if (this.started && this.data.animation().isResetBeforePlay()) {
                ((IModelPart)((Object)model.getRootPart())).viaBedrockUtility$resetEverything();
                this.TEMP_VEC.set(0);
            }

            if (skipThisTick) {
                return;
            }
        }

        float runningTime = AnimationHelper.getRunningSeconds(data.animation(), data.compiled(), System.currentTimeMillis() - this.animationStartMS);

        // Override life_time and anim_time with animation-specific values (not entity lifetime)
        final MutableObjectBinding animQueryBinding = new MutableObjectBinding();
        animQueryBinding.setAllFrom((MutableObjectBinding) this.baseScope.get("query"));
        animQueryBinding.set("anim_time", Value.of(runningTime));
        animQueryBinding.set("life_time", Value.of(runningTime));
        scope.set("query", animQueryBinding);
        scope.set("q", animQueryBinding);

        AnimationHelper.animate(scope, model, data.compiled(), System.currentTimeMillis() - this.animationStartMS, this.blendWeight, TEMP_VEC);

        float runningTimeWithoutLoop = (System.currentTimeMillis() - this.animationStartMS) / 1000F;
        this.tickTimeline(runningTimeWithoutLoop);

        if (data.compiled().lengthInSeconds() > 0 && runningTimeWithoutLoop >= data.compiled().lengthInSeconds()) {
            this.stop(model);
        }
    }

    private void tickTimeline(float runningTime) {
        final Map<Float, List<String>> timeline = this.data.animation().getTimeline();
        if (timeline.isEmpty()) {
            return;
        }

        Float nextTimestamp = null;
        Map.Entry<Float, List<String>> candidate = null;

        for (Map.Entry<Float, List<String>> entry : timeline.entrySet()) {
            float timestamp = entry.getKey();
            if (timestamp > runningTime) {
                nextTimestamp = timestamp;
                break;
            }
            if (!entry.getValue().isEmpty()) {
                candidate = entry;
            }
        }

        if (candidate != null
                && (nextTimestamp == null || nextTimestamp > runningTime)
                && Math.abs(candidate.getKey() - runningTime) < 0.005F) {
            this.ticker.handleAnimationTimeline(candidate.getValue());
        }
    }

    public void stop(Model model) {
        this.stop(model, false);
    }

    public void stop(Model model, boolean forcefully) {
        if (this.data.animation().getLoop().getValue().equals(false) || forcefully) {
            ((IModelPart)((Object)model.getRootPart())).viaBedrockUtility$resetEverything();
        }

        this.animationStartMS = System.currentTimeMillis();

        this.donePlaying = true;
        this.started = false;
    }
}
