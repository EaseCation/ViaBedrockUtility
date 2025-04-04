package org.oryxel.viabedrockutility.animation.animator;

import lombok.Setter;
import net.minecraft.client.model.Model;
import org.joml.Vector3f;
import org.oryxel.viabedrockutility.animation.vanilla.AnimationHelper;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import org.oryxel.viabedrockutility.pack.definitions.AnimationDefinitions;
import org.oryxel.viabedrockutility.renderer.CustomEntityRenderer;
import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import java.io.IOException;

public class Animator {
    private final Model model;
    private final AnimationDefinitions.AnimationData data;

    private long animationStartMS;

    private boolean donePlaying, started, firstPlay;

    private final Vector3f TEMP_VEC = new Vector3f();

    @Setter
    private Scope baseScope;

    public Animator(Model model, AnimationDefinitions.AnimationData data) {
        this.model = model;
        this.data = data;

        this.animationStartMS = System.currentTimeMillis();
        this.firstPlay = true;
    }

    public void animate(CustomEntityRenderer.CustomEntityRenderState state) throws IOException {
        if (this.donePlaying) {
            if (this.data.animation().getLoop().getValue().equals(true)) {
                this.donePlaying = false;
            } else {
                return;
            }
        }

        final Scope scope = this.baseScope.copy();

        final MutableObjectBinding queryBinding = new MutableObjectBinding();
        queryBinding.setAllFrom((MutableObjectBinding) this.baseScope.get("query"));

        queryBinding.set("modified_distance_moved", Value.of(state.getDistanceTraveled()));
        queryBinding.set("modified_move_speed", Value.of(0.7F)); // We don't know this value I think? not yet.

        queryBinding.set("body_y_rotation", Value.of(state.getBodyYaw()));
        queryBinding.set("body_x_rotation", Value.of(state.getBodyPitch()));

        scope.set("q", queryBinding);
        scope.set("query", queryBinding);

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

        queryBinding.set("anim_time", Value.of(runningTime));
        queryBinding.set("life_time", Value.of(runningTime));

        AnimationHelper.animate(scope, model, data.compiled(), System.currentTimeMillis() - this.animationStartMS, 1, TEMP_VEC);

        if ((System.currentTimeMillis() - this.animationStartMS) / 1000F >= data.compiled().lengthInSeconds()) {
            System.out.println("Reset since animation length: " + data.compiled().lengthInSeconds());
            this.stop();
        }
    }

    public void stop() {
        this.stop(false);
    }

    public void stop(boolean forcefully) {
        if (this.data.animation().getLoop().getValue().equals(false) || forcefully) {
            ((IModelPart)((Object)model.getRootPart())).viaBedrockUtility$resetEverything();
        }

        this.animationStartMS = System.currentTimeMillis();

        this.donePlaying = true;
        this.started = false;
    }
}
