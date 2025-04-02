package org.oryxel.viabedrockutility.mixin.interfaces;

public interface IModelPart {
    boolean viaBedrockUtility$isVBUModel();
    void viaBedrockUtility$setVBUModel();
    void viaBedrockUtility$setNeededOffset(boolean needed);
    void viaBedrockUtility$setPivot(float x, float y, float z);
    void viaBedrockUtility$setAngles(float pitch, float yaw, float roll);
}
