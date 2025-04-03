package org.oryxel.viabedrockutility.mixin.impl.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.util.MathUtil;
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
import java.util.stream.Stream;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements IModelPart {
    @Unique
    public ModelTransform viaBedrockUtility$defaultTransform = ModelTransform.NONE;

    @Shadow public float originX;
    @Shadow public float originZ;

    @Shadow @Final private Map<String, ModelPart> children;

    @Shadow public abstract Stream<ModelPart> traverse();

    @Unique
    private String name = "";

    @Unique private boolean isVBUModel;
    @Unique private boolean neededOffset;

    @Unique private float pivotX;
    @Unique private float pivotY;
    @Unique private float pivotZ;

    @Unique private float customPitch;
    @Unique private float customYaw;
    @Unique private float customRoll;

    @Unique private float offsetX;
    @Unique private float offsetY;
    @Unique private float offsetZ;
    
    @Unique
    private Vector3f defaultOffset = new Vector3f();

    @Unique private boolean alreadySetRotation = false;
    @Unique private boolean alreadySetPivot = false;

    @Inject(method = "applyTransform", at = @At("HEAD"))
    public void render(MatrixStack matrices, CallbackInfo ci) {
        matrices.translate(this.pivotX / 16.0F, this.pivotY / 16.0F, this.pivotZ / 16.0F);
        if (this.customPitch != 0.0F || this.customYaw != 0.0F || this.customRoll != 0.0F) {
            matrices.multiply((new Quaternionf()).rotationXYZ(this.customPitch * MathUtil.DEGREES_TO_RADIANS, this.customYaw * MathUtil.DEGREES_TO_RADIANS, this.customRoll * MathUtil.DEGREES_TO_RADIANS));
        }
        matrices.translate(-this.pivotX / 16.0F, -this.pivotY / 16.0F, -this.pivotZ / 16.0F);

        // Dang so we have to flip Y
        if (this.offsetX != 0 || this.offsetY != 0 || this.offsetZ != 0) {
            matrices.translate(this.offsetX / 16.0F, -(this.offsetY + 24.016) / 16.0F, this.offsetZ / 16.0F);
        }
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
    public String viaBedrockUtility$getName() {
        return this.name;
    }

    @Override
    public void viaBedrockUtility$setName(String name) {
        this.name = name;
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
    public void viaBedrockUtility$resetEverything() {
        this.traverse().toList().forEach(part -> {
            ((IModelPart)((Object)part)).viaBedrockUtility$setAngles(this.viaBedrockUtility$defaultTransform.pitch(),
                    this.viaBedrockUtility$defaultTransform.yaw(), this.viaBedrockUtility$defaultTransform.roll());
            ((IModelPart)((Object)part)).viaBedrockUtility$setPivot(this.viaBedrockUtility$defaultTransform.x(),
                    this.viaBedrockUtility$defaultTransform.y(), this.viaBedrockUtility$defaultTransform.z());
            ((IModelPart)((Object)part)).viaBedrockUtility$setOffset(this.defaultOffset.x, this.defaultOffset.y, this.defaultOffset.z);
        });
    }

    @Override
    public void viaBedrockUtility$setPivot(float x, float y, float z) {
        if (!this.alreadySetPivot) {
            this.viaBedrockUtility$defaultTransform = new ModelTransform(
                    x, y, z,
                    this.viaBedrockUtility$defaultTransform.pitch(), this.viaBedrockUtility$defaultTransform.yaw(),
                    this.viaBedrockUtility$defaultTransform.roll(), 1, 1, 1);
            this.alreadySetPivot = true;
        }

        this.pivotX = x;
        this.pivotY = y;
        this.pivotZ = z;
    }

    @Override
    public void viaBedrockUtility$setOffset(float x, float y, float z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
    }

    @Override
    public void viaBedrockUtility$setAngles(float pitch, float yaw, float roll) {
        if (!this.alreadySetRotation) {
            this.viaBedrockUtility$defaultTransform = new ModelTransform(
                    this.viaBedrockUtility$defaultTransform.x(), this.viaBedrockUtility$defaultTransform.y(), this.viaBedrockUtility$defaultTransform.z(),
                    pitch, yaw, roll, 1, 1, 1);
            this.alreadySetRotation = true;
        }

        this.customPitch = pitch;
        this.customYaw = yaw;
        this.customRoll = roll;
    }
}
