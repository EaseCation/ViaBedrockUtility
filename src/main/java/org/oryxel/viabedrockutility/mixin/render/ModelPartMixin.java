package org.oryxel.viabedrockutility.mixin.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import org.oryxel.viabedrockutility.util.GeometryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin {
    @Shadow public abstract boolean hasChild(String child);

    @Shadow public float originX;
    @Shadow public float originY;
    @Shadow public float originZ;

    @Inject(method = "applyTransform", at = @At("RETURN"))
    public void render(MatrixStack matrices, CallbackInfo ci) {
        if (this.hasChild(GeometryUtil.HARDCODED_INDICATOR)) {
            // If this is our model, translate the pivot back so it in correct position.
            matrices.translate(-this.originX / 16.0F, -this.originY / 16.0F, -this.originZ / 16.0F);
        }
    }
}
