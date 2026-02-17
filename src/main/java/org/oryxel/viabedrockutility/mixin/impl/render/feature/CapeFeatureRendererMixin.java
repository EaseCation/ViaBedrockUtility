package org.oryxel.viabedrockutility.mixin.impl.render.feature;

import net.minecraft.client.render.RenderLayer;
//? if >=1.21.11 {
import net.minecraft.client.render.RenderLayers;
//?}
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {
    //? if >=1.21.11 {
    @Redirect(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayers;entitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"),
            require = 0
    )
    public RenderLayer solidToTranslucent(final Identifier texture) {
        if (texture.getNamespace().equals(ViaBedrockUtilityFabric.MOD_ID)) {
            return RenderLayers.entityTranslucent(texture, true);
        }
        return RenderLayers.entitySolid(texture);
    }
    //?} else if >=1.21.9 {
    /*@Redirect(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getEntitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"),
            require = 0
    )
    public RenderLayer solidToTranslucent(final Identifier texture) {
        if (texture.getNamespace().equals(ViaBedrockUtilityFabric.MOD_ID)) {
            return RenderLayer.getEntityTranslucent(texture, true);
        }
        return RenderLayer.getEntitySolid(texture);
    }
    *///?} else {
    /*@Redirect(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getEntitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"),
            require = 0
    )
    public RenderLayer solidToTranslucent(final Identifier texture) {
        if (texture.getNamespace().equals(ViaBedrockUtilityFabric.MOD_ID)) {
            return RenderLayer.getEntityTranslucent(texture, true);
        }
        return RenderLayer.getEntitySolid(texture);
    }
    *///?}
}
