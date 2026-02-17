package org.oryxel.viabedrockutility.renderer.model;

import lombok.Getter;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModel;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.renderer.CustomEntityRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class CustomEntityModel<T extends CustomEntityRenderer.CustomEntityRenderState> extends EntityModel<T> {
    private Map<String, List<ModelPart>> partsByName;

    public CustomEntityModel(ModelPart root) {
        super(root);
    }

    /**
     * Returns a lazily-built index of part name → ModelPart list.
     * Used by applyPartVisibility to avoid O(n*m) double loops.
     */
    public Map<String, List<ModelPart>> getPartsByName() {
        if (this.partsByName == null) {
            this.partsByName = new HashMap<>();
            for (ModelPart part : this.getParts()) {
                String name = ((IModelPart) ((Object) part)).viaBedrockUtility$getName();
                if (name != null) {
                    this.partsByName.computeIfAbsent(name, k -> new ArrayList<>(2)).add(part);
                }
            }
        }
        return this.partsByName;
    }
}
