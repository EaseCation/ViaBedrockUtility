package org.oryxel.viabedrockutility.renderer;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
//? if >=1.21.11 {
import net.minecraft.client.render.RenderLayers;
//?}
//? if >=1.21.9 {
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//?} else {
/*import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
*///?}
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class AnimatedOverlayFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private final CustomPlayerRenderer renderer;

    public AnimatedOverlayFeatureRenderer(CustomPlayerRenderer renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    //? if >=1.21.9 {
    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (renderer.getOverlays().isEmpty()) return;

        PlayerEntityModel mainModel = (PlayerEntityModel) renderer.getModel();
        for (AnimatedSkinOverlay overlay : renderer.getOverlays()) {
            overlay.copyBoneTransformsFrom(mainModel);
            //? if >=1.21.11 {
            RenderLayer renderLayer = RenderLayers.entityTranslucent(overlay.getTextureId(), true);
            //?} else {
            /*RenderLayer renderLayer = RenderLayer.getEntityTranslucent(overlay.getTextureId(), true);
            *///?}
            queue.getBatchingQueue(0).submitModelPart(overlay.getModel().getRootPart(), matrices, renderLayer, light, OverlayTexture.packUv(0, 10), null);
        }
    }
    //?} else {
    /*@Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (renderer.getOverlays().isEmpty()) return;

        PlayerEntityModel mainModel = (PlayerEntityModel) renderer.getModel();
        for (AnimatedSkinOverlay overlay : renderer.getOverlays()) {
            overlay.copyBoneTransformsFrom(mainModel);
            RenderLayer renderLayer = RenderLayer.getEntityTranslucent(overlay.getTextureId(), true);
            VertexConsumer consumer = vertexConsumers.getBuffer(renderLayer);
            overlay.getModel().render(matrices, consumer, light, OverlayTexture.packUv(0, 10));
        }
    }
    *///?}
}
