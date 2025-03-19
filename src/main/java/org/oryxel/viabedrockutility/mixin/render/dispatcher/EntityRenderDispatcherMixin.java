package org.oryxel.viabedrockutility.mixin.render.dispatcher;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.oryxel.viabedrockutility.renderer.CustomPlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unchecked")
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "getRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SkinTextures;model()Lnet/minecraft/client/util/SkinTextures$Model;"), cancellable = true)
    public <T extends Entity> void getRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T, ?>> cir) {
        final CustomPlayerRenderer skin = ViaBedrockUtility.getInstance().getPayloadHandler().getCachedPlayerRenderers().get(entity.getUuid().toString());
        if (skin != null) {
            cir.setReturnValue((EntityRenderer<? super T, ?>) skin);
        }
    }
}