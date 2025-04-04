package org.oryxel.viabedrockutility.renderer.model;

import lombok.Getter;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModel;
import org.oryxel.viabedrockutility.animation.animator.Animator;
import org.oryxel.viabedrockutility.renderer.CustomEntityRenderer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CustomEntityModel<T extends CustomEntityRenderer.CustomEntityRenderState> extends EntityModel<T> {
    private final Map<String, Animator> animators = new ConcurrentHashMap<>();

    public CustomEntityModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setAngles(T state) {
        this.animators.forEach((k, v) -> {
            try {
                v.animate(state);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void play(String identifier, Animator animator) {
        this.animators.put(identifier, animator);
    }

    public void reset() {
        this.animators.values().forEach(animator -> animator.stop(true));
        this.animators.clear();
    }

    public void remove(String identifier) {
        this.animators.remove(identifier);
    }
}
