package org.oryxel.viabedrockutility.mixin.impl.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements IModelPart {
    @Shadow public float originX;
    @Shadow public float originY;
    @Shadow public float originZ;

    @Shadow @Final private Map<String, ModelPart> children;

    @Unique private boolean isVBUModel;
    @Unique private boolean neededOffset;

    @Unique private float pivotX;
    @Unique private float pivotY;
    @Unique private float pivotZ;

    @Unique private float customPitch;
    @Unique private float customYaw;
    @Unique private float customRoll;

    @Inject(method = "applyTransform", at = @At("HEAD"))
    public void render(MatrixStack matrices, CallbackInfo ci) {
        matrices.translate(this.pivotX / 16.0F, this.pivotY / 16.0F, this.pivotZ / 16.0F);
        matrices.multiply((new Quaternionf()).rotationZYX(this.customPitch, this.customYaw, this.customRoll));
        matrices.translate(-this.pivotX / 16.0F, -this.pivotY / 16.0F, -this.pivotZ / 16.0F);
    }

    @Inject(method = "applyTransform", at = @At("TAIL"))
    public void renderTail(MatrixStack matrices, CallbackInfo ci) {
        if (!this.isVBUModel || !this.neededOffset) {
            return;
        }

        // Well don't ask me why left/right arm act weirdly, the position and origin being different from the hardcoded one I think.
        matrices.translate(-this.originX / 16.0F, 0, -this.originZ / 16.0F);
    }

    @Inject(method = "getChild", at = @At("HEAD"), cancellable = true)
    private void getChild(String name, CallbackInfoReturnable<ModelPart> cir) {
        if (this.isVBUModel) {
            cir.setReturnValue(this.children.getOrDefault(name, new ModelPart(List.of(), Map.of())));
        }
    }

    @Override
    public void viaBedrockUtility$setNeededOffset(boolean needed) {
        this.neededOffset = needed;
    }

    @Override
    public boolean viaBedrockUtility$isVBUModel() {
        return this.isVBUModel;
    }

    @Override
    public void viaBedrockUtility$setVBUModel() {
        this.isVBUModel = true;
    }

    @Override
    public void viaBedrockUtility$setPivot(float x, float y, float z) {
        this.pivotX = x;
        this.pivotY = y;
        this.pivotZ = z;
    }

    @Override
    public void viaBedrockUtility$setAngles(float pitch, float yaw, float roll) {
        this.customPitch = pitch;
        this.customYaw = yaw;
        this.customRoll = roll;
    }
}
