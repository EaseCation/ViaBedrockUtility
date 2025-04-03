package org.oryxel.viabedrockutility.renderer.extra;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.animation.Animation;
import org.oryxel.viabedrockutility.animation.animator.Animator;
import org.oryxel.viabedrockutility.renderer.BaseCustomEntityRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CustomEntityRendererImpl<T extends Entity> extends BaseCustomEntityRenderer<T> {
    private final Map<Model, Animator> animators = new HashMap<>();

    public CustomEntityRendererImpl(List<Model> list, EntityRendererFactory.Context context) {
        super(list, context);
    }

    @Override
    public void onRenderModel(CustomEntityRenderState state, Model model) {
        if (!this.animators.containsKey(model)) {
            return;
        }

        try {
            this.animators.get(model).update(state);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        this.animators.forEach((k, v) -> v.stop());
        this.animators.clear();
    }

    public void play(final UUID uuid, final String geometry, final Animation animation) {
        if (animation == null) {
            System.out.println("null!");
            return;
        }

        boolean found = false;
        for (final Model model : this.getModels()) {
            if (geometry.equals(model.geometry())) {
                this.animators.put(model, new Animator(model.model().getRootPart(), ViaBedrockUtility.getInstance().getPayloadHandler().getCachedScopes().get(uuid), animation));
                found = true;
            }
            System.out.println(geometry + "," + model.geometry());
        }

        if (found) {
            System.out.println("Play successfully!");
        }
    }
}
