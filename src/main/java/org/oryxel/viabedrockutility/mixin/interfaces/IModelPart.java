package org.oryxel.viabedrockutility.mixin.interfaces;

import net.minecraft.client.model.ModelPart;
import org.joml.Vector3f;

import java.util.Map;

public interface IModelPart {
    boolean viaBedrockUtility$isVBUModel();
    void viaBedrockUtility$setName(String name);
    String viaBedrockUtility$getName();
    void viaBedrockUtility$resetEverything();
    void viaBedrockUtility$setVBUModel();
    void viaBedrockUtility$setNeededOffset(boolean needed);
    void viaBedrockUtility$setOffset(Vector3f vec3);
    void viaBedrockUtility$setPivot(Vector3f vec3);
    void viaBedrockUtility$setAngles(Vector3f vec3);
    Map<String, ModelPart> viaBedrockUtility$getChildren();
}
