package org.oryxel.viabedrockutility.renderer;

import lombok.Getter;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.mixin.interfaces.ICuboid;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;

@Getter
public class AnimatedSkinOverlay {
    private final PlayerEntityModel model;
    private final Identifier textureId;
    private final int type;
    private final int totalFrames;
    private final float frameVSize;

    private int currentFrame = 0;
    private int tickCounter = 0;

    public AnimatedSkinOverlay(PlayerEntityModel model, Identifier textureId, int type, int totalFrames, int textureHeight, int frameHeight) {
        this.model = model;
        this.textureId = textureId;
        this.type = type;
        this.totalFrames = totalFrames;
        this.frameVSize = (float) frameHeight / textureHeight;
        updateVOffsetOnCuboids();
    }

    public void tick() {
        tickCounter++;
        if (tickCounter >= 2) {
            tickCounter = 0;
            currentFrame = (currentFrame + 1) % totalFrames;
            updateVOffsetOnCuboids();
        }
    }

    public float getCurrentVOffset() {
        return currentFrame * frameVSize;
    }

    public void updateVOffsetOnCuboids() {
        float vOffset = getCurrentVOffset();
        applyVOffsetRecursive(model.getRootPart(), vOffset);
    }

    private static void applyVOffsetRecursive(ModelPart part, float vOffset) {
        for (ModelPart.Cuboid cuboid : ((IModelPart) (Object) part).viaBedrockUtility$getCuboids()) {
            ((ICuboid) (Object) cuboid).viaBedrockUtility$setVOffset(vOffset);
        }
        for (ModelPart child : ((IModelPart) (Object) part).viaBedrockUtility$getChildren().values()) {
            applyVOffsetRecursive(child, vOffset);
        }
    }

    public void copyBoneTransformsFrom(PlayerEntityModel source) {
        copyPartTransform(source.head, model.head);
        copyPartTransform(source.body, model.body);
        copyPartTransform(source.leftArm, model.leftArm);
        copyPartTransform(source.rightArm, model.rightArm);
        copyPartTransform(source.leftLeg, model.leftLeg);
        copyPartTransform(source.rightLeg, model.rightLeg);
    }

    private static void copyPartTransform(ModelPart source, ModelPart target) {
        if (source == null || target == null) return;
        target.pitch = source.pitch;
        target.yaw = source.yaw;
        target.roll = source.roll;
        target.visible = source.visible;
    }
}
