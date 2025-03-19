package org.oryxel.viabedrockutility.mixin.render.feature;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.util.Identifier;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapeFeatureRenderer.class)
public class CapeFeatureRendererMixin {
    @Redirect(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getEntitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"),
            require = 0 // Fail safely if other mods overwrite this
    )
    public RenderLayer solidToTranslucent(final Identifier texture) {
        if (texture.getNamespace().equals(ViaBedrockUtilityFabric.MOD_ID)) {
            // Capes can be translucent in Bedrock
            return RenderLayer.getEntityTranslucent(texture, true);
        }
        return RenderLayer.getEntitySolid(texture);
    }
}