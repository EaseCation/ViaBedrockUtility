package org.oryxel.viabedrockutility.material;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;
import static net.minecraft.client.render.RenderLayer.*;

// https://wiki.bedrock.dev/documentation/materials#entity-emissive
// This is all I can find on this, eh so prob not that accurate.
public class VanillaMaterials {
    private static final Map<String, Function<Identifier, RenderLayer>> NAME_TO_MATERIAL = new HashMap<>();

    static {
        // Maybe correct?
        // NAME_TO_MATERIAL.put("alpha_block", RenderLayer.getSolid());
        NAME_TO_MATERIAL.put("alpha_block_color", Util.memoize((identifier -> build("alpha_block_color", 786432, identifier, builder -> of(ITEM_ENTITY_TRANSLUCENT_CULL_PROGRAM)))));
        NAME_TO_MATERIAL.put("banner", Util.memoize(identifier -> RenderLayer.getEntityTranslucent(identifier)));
        NAME_TO_MATERIAL.put("banner_pole", Util.memoize(identifier -> RenderLayer.getEntityTranslucent(identifier)));
        // NAME_TO_MATERIAL.put("beacon_beam", RenderLayer.getSolid());
        NAME_TO_MATERIAL.put("beacon_beam_transparent", Util.memoize(identifier -> RenderLayer.getEntityTranslucent(identifier))); // No idea about this one...
        NAME_TO_MATERIAL.put("charged_creeper", Util.memoize(identifier -> build("charged_creeper", 1536, identifier, builder -> builder.program(ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM).transparency(TRANSLUCENT_TRANSPARENCY).cull(DISABLE_CULLING).writeMaskState(COLOR_MASK).overlay(ENABLE_OVERLAY_COLOR))));
        NAME_TO_MATERIAL.put("conduit_wind", Util.memoize(RenderLayer::getEntityCutout));
        // NAME_TO_MATERIAL.put("entity", RenderLayer.getSolid());
        NAME_TO_MATERIAL.put("entity_alphablend", RenderLayer::getEntityAlpha);
        NAME_TO_MATERIAL.put("entity_alphatest", Util.memoize(identifier -> RenderLayer.getEntityTranslucent(identifier)));

        // Handle this seperately
//        NAME_TO_MATERIAL.put("entity_alphatest_change_color_glint", RenderLayer.getGlintTranslucent());
//        NAME_TO_MATERIAL.put("entity_alphatest_glint", RenderLayer.getGlintTranslucent());
//        NAME_TO_MATERIAL.put("entity_alphatest_glint_item", RenderLayer.getGlintTranslucent());

        NAME_TO_MATERIAL.put("entity_beam", Util.memoize(identifier -> RenderLayer.getEntityTranslucent(identifier)));

        NAME_TO_MATERIAL.put("entity_beam_additive", Util.memoize(identifier -> build("entity_beam_additive", 1536, identifier, builder -> builder.program(ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM).transparency(ADDITIVE_TRANSPARENCY).cull(DISABLE_CULLING))));
        NAME_TO_MATERIAL.put("entity_custom", Util.memoize(identifier -> RenderLayer.getEntityTranslucent(identifier)));

        NAME_TO_MATERIAL.put("entity_dissolve_layer0", Util.memoize(RenderLayer::getItemEntityTranslucentCull));
        NAME_TO_MATERIAL.put("entity_alphatest_change_color", Util.memoize(RenderLayer::getItemEntityTranslucentCull));

        Function<Identifier, RenderLayer> emissiveDefault = Util.memoize((texture) -> {
            MultiPhaseParameters multiPhaseParameters = RenderLayer.MultiPhaseParameters.builder().program(ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM).texture(new RenderPhase.Texture(texture, TriState.FALSE, false)).cull(DISABLE_CULLING).writeMaskState(COLOR_MASK).overlay(ENABLE_OVERLAY_COLOR).build(false);
            return of("entity_emissive", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 1536, true, true, multiPhaseParameters);
        });
        NAME_TO_MATERIAL.put("entity_emissive", emissiveDefault);
        NAME_TO_MATERIAL.put("entity_emissive_alpha", Util.memoize(identifier -> getEntityTranslucentEmissive(identifier)));
        NAME_TO_MATERIAL.put("entity_emissive_alpha_one_sided", Util.memoize(identifier -> getEntityTranslucentEmissive(identifier)));

        NAME_TO_MATERIAL.put("entity_nocull", Util.memoize((texture) -> {
            MultiPhaseParameters multiPhaseParameters = RenderLayer.MultiPhaseParameters.builder().program(ENTITY_SOLID_PROGRAM).texture(new RenderPhase.Texture(texture, TriState.FALSE, false)).transparency(NO_TRANSPARENCY).lightmap(ENABLE_LIGHTMAP).cull(DISABLE_CULLING).overlay(ENABLE_OVERLAY_COLOR).build(true);
            return of("entity_solid", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 1536, true, false, multiPhaseParameters);
        }));

        NAME_TO_MATERIAL.put("guardian_ghost", Util.memoize(identifier -> RenderLayer.getEntityTranslucent(identifier)));
        NAME_TO_MATERIAL.put("item_in_hand", Util.memoize(RenderLayer::getEntitySolid));

        // I'm too lazy to map out the rest, the rest shouldn't be used for entity tho.... I think?
    }

    public static boolean hasMaterial(final String name) {
        return NAME_TO_MATERIAL.containsKey(name);
    }

    public static Function<Identifier, RenderLayer> getFunction(final String name) {
        return NAME_TO_MATERIAL.get(name);
    }

    public static RenderLayer getRenderLayer(final String name, final Identifier identifier) {
        if (!NAME_TO_MATERIAL.containsKey(name)) {
            return getEntitySolid(identifier);
        }
        
        return getFunction(name).apply(identifier);
    }

    public static RenderLayer.MultiPhaseParameters.Builder renderLayerToBuilder(final RenderLayer rawLayer, final Identifier identifier) {
        final RenderLayer.MultiPhaseParameters.Builder builder = RenderLayer.MultiPhaseParameters.builder();
        if (rawLayer instanceof MultiPhase renderLayer) {
            MultiPhaseParameters parameters = renderLayer.phases;
            builder.texture(new RenderPhase.Texture(identifier, TriState.FALSE, false));
            builder.program(parameters.program);
            builder.transparency(parameters.transparency);
            builder.depthTest(parameters.depthTest);
            builder.cull(parameters.cull);
            builder.lightmap(parameters.lightmap);
            builder.overlay(parameters.overlay);
            builder.layering(parameters.layering);
            builder.target(parameters.target);
            builder.texturing(parameters.texturing);
            builder.writeMaskState(parameters.writeMaskState);
            builder.lineWidth(parameters.lineWidth);
            builder.colorLogic(parameters.colorLogic);
        }

        return builder;
    }

    public static RenderLayer build(final String name, int bufferSize, RenderLayer.MultiPhaseParameters.Builder builder) {
        return of(name, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, bufferSize, true, true, builder.build(false));
    }

    private static RenderLayer build(final String name, int bufferSize, Identifier identifier, Consumer<RenderLayer.MultiPhaseParameters.Builder> consumer) {
        final RenderLayer.MultiPhaseParameters.Builder builder = RenderLayer.MultiPhaseParameters.builder();
        builder.texture(new RenderPhase.Texture(identifier, TriState.FALSE, false));
        consumer.accept(builder);

        return of(name, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, bufferSize, true, true, builder.build(false));
    }
}
