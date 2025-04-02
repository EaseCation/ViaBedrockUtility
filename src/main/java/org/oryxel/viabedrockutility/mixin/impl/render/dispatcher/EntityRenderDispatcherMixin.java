package org.oryxel.viabedrockutility.mixin.impl.render.dispatcher;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.oryxel.viabedrockutility.ViaBedrockUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unchecked")
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "getRenderer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SkinTextures;model()Lnet/minecraft/client/util/SkinTextures$Model;"), cancellable = true)
    public <T extends Entity> void getPlayerRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T, ?>> cir) {
        final EntityRenderer<?, ?> renderer = ViaBedrockUtility.getInstance().getPayloadHandler().getCachedPlayerRenderers().get(entity.getUuid());
        if (renderer != null) {
            cir.setReturnValue((EntityRenderer<? super T, ?>) renderer);
        }
    }

    @Inject(method = "getRenderer", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 2), cancellable = true)
    public <T extends Entity> void getRenderer(T entity, CallbackInfoReturnable<EntityRenderer<? super T, ?>> cir) {
        final EntityRenderer<?, ?> renderer = ViaBedrockUtility.getInstance().getPayloadHandler().getCachedRenderers().get(entity.getUuid());
        if (renderer != null) {
            cir.setReturnValue((EntityRenderer<? super T, ?>) renderer);
        }
    }
}