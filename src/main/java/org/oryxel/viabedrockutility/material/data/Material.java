package org.oryxel.viabedrockutility.material.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;

import java.util.*;
import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;
import static org.oryxel.viabedrockutility.util.JsonUtil.*;

// https://wiki.bedrock.dev/visuals/materials
public record Material(String identifier, String baseIdentifier, MaterialInfo info) {
    public static Map<String, Material> parse(final Map<String, Material> existing, final JsonObject base) {
        final Map<String, Material> map = new HashMap<>();

        if (!base.has("materials")) {
            return map;
        }

        final JsonObject materials = base.getAsJsonObject("materials");
        for (final String elementName : materials.keySet()) {
            final JsonElement element = materials.get(elementName);
            if (elementName.equals("version") || !element.isJsonObject()) {
                continue;
            }

            final JsonObject object = element.getAsJsonObject();
            final String[] split = elementName.split(":");
            if (split.length > 2) {
                continue;
            }

            final String identifier = split[0], baseIdentifier = split.length == 1 ? "" : split[1];

            final MaterialInfo material;
            if (!baseIdentifier.isBlank()) {
                final Material parent = map.getOrDefault(baseIdentifier, existing.get(baseIdentifier));
                if (parent == null) {
                    material = MaterialInfo.emptyMaterial();
                } else {
                    material = parent.info.clone();
                }
            } else {
                material = MaterialInfo.emptyMaterial();
            }

            material.parse(object, false);
            map.put(identifier, new Material(identifier, baseIdentifier, material));
        }

        return map;
    }

    @RequiredArgsConstructor
    @ToString
    @Getter
    @Setter
    public static class MaterialInfo {
        protected final Set<String> states, defines;
        protected final Set<String> vertexFields;
        protected String blendSrc = "", blendDst = "", depthFunc = "";

        protected final Map<String, Variant> variants = new HashMap<>();

        private Function<Identifier, RenderLayer> function;

        public void parse(final JsonObject object, boolean ignoreVariants) {
            final Set<String> extraStates = arrayToStringSet(object.getAsJsonArray("+states"));
            final Set<String> extraDefines = arrayToStringSet(object.getAsJsonArray("+defines"));
            final Set<String> removeStates = arrayToStringSet(object.getAsJsonArray("-states"));
            final Set<String> removeDefines = arrayToStringSet(object.getAsJsonArray("-defines"));

            this.states.addAll(extraStates);
            this.defines.addAll(extraDefines);
            this.states.removeAll(removeStates);
            this.defines.removeAll(removeDefines);

            if (object.has("vertexFields")) {
                this.vertexFields.clear();
                this.vertexFields.addAll(vertexFields(object.getAsJsonArray("vertexFields")));
            }

            if (object.has("blendSrc")) {
                this.blendSrc = object.get("blendSrc").getAsString();
            }

            if (object.has("blendDst")) {
                this.blendSrc = object.get("blendDst").getAsString();
            }

            if (object.has("depthFunc")) {
                this.blendSrc = object.get("depthFunc").getAsString();
            }

            if (ignoreVariants) {
                return;
            }

            if (object.has("variants") && object.get("variants").isJsonArray()) {
                final JsonArray jsonVariants = object.getAsJsonArray("variants");
                for (JsonElement variantElement : jsonVariants) {
                    if (!variantElement.isJsonObject()) {
                        continue;
                    }

                    final JsonObject variantObject = variantElement.getAsJsonObject();
                    for (final String variantName : variantObject.keySet()) {
                        if (!variantObject.get(variantName).isJsonObject()) {
                            continue;
                        }

                        final Variant baseVariant = this.variants.getOrDefault(variantName, MaterialInfo.emptyVariant()).clone();
                        this.variants.put(variantName, baseVariant);

                        baseVariant.parse(variantObject.getAsJsonObject(variantName), true);
                    }
                }
            }

            this.variants.forEach((k, v) -> {
                v.states.addAll(extraStates);
                v.defines.addAll(extraDefines);
                v.states.removeAll(removeStates);
                v.defines.removeAll(removeDefines);

                v.vertexFields.addAll(vertexFields);
            });
        }

        public Function<Identifier, RenderLayer> build() {
            return Objects.requireNonNullElseGet(this.function, () -> this.function = Util.memoize(texture -> {
                final RenderLayer.MultiPhaseParameters.Builder builder = RenderLayer.MultiPhaseParameters.builder();
                if (!this.defines.contains("NO_TEXTURE")) {
                    builder.texture(new Texture(texture, TriState.FALSE, false));
                }

                builder.program(ENTITY_SOLID_PROGRAM);
                builder.lightmap(ENABLE_LIGHTMAP);

                if (this.defines.contains("USE_OVERLAY")) {
                    builder.overlay(ENABLE_OVERLAY_COLOR);
                }

                if (this.states.contains("Blending")) {
                    builder.transparency(TRANSLUCENT_TRANSPARENCY);
                }

                if (this.states.contains("DisableCulling")) {
                    builder.cull(DISABLE_CULLING);
                } else {
                    builder.cull(ENABLE_CULLING);
                }

                builder.depthTest(switch (this.depthFunc) {
                    case "Equal" -> EQUAL_DEPTH_TEST;
                    case "Bigger" -> BIGGER_DEPTH_TEST;
                    default -> RenderPhase.LEQUAL_DEPTH_TEST;
                });

//                if (!this.states.contains("DisableDepthWrite")) {
//                }

                if (this.defines.contains("USE_COLOR_MASK")) {
                    builder.writeMaskState(COLOR_MASK);
                }

                if (this.defines.contains("ALPHA_TEST")) {
                    builder.program(ENTITY_ALPHA_PROGRAM);
                }

                if (this.defines.contains("USE_EMISSIVE")) {
                    builder.program(ENTITY_TRANSLUCENT_EMISSIVE_PROGRAM);
                }

                if (!this.blendSrc.isBlank() && !this.blendDst.isBlank()) {
                    final GlStateManager.SrcFactor srcFactor = switch (this.blendSrc) {
                        case "SourceAlpha" -> GlStateManager.SrcFactor.SRC_ALPHA;
                        case "SourceColor" -> GlStateManager.SrcFactor.SRC_COLOR;
                        case "ConstantAlpha" -> GlStateManager.SrcFactor.CONSTANT_ALPHA;
                        case "ConstantColor" -> GlStateManager.SrcFactor.CONSTANT_COLOR;
                        case "DstAlpha" -> GlStateManager.SrcFactor.DST_ALPHA;
                        case "DstColor" -> GlStateManager.SrcFactor.DST_COLOR;
                        case "OneMinusConstantAlpha" -> GlStateManager.SrcFactor.ONE_MINUS_CONSTANT_ALPHA;
                        case "OneMinusConstantColor" -> GlStateManager.SrcFactor.ONE_MINUS_CONSTANT_COLOR;
                        case "OneMinusDstAlpha" -> GlStateManager.SrcFactor.ONE_MINUS_DST_ALPHA;
                        case "OneMinusDstColor" -> GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR;
                        case "OneMinusSrcAlpha" -> GlStateManager.SrcFactor.ONE_MINUS_SRC_ALPHA;
                        case "OneMinusSrcColor" -> GlStateManager.SrcFactor.ONE_MINUS_SRC_COLOR;
                        case "SourceAlphaSaturate" -> GlStateManager.SrcFactor.SRC_ALPHA_SATURATE;
                        case "Zero" -> GlStateManager.SrcFactor.ZERO;
                        default -> GlStateManager.SrcFactor.ONE;
                    };

                    final GlStateManager.DstFactor dstFactor = switch (this.blendDst) {
                        case "SourceAlpha" -> GlStateManager.DstFactor.SRC_ALPHA;
                        case "SourceColor" -> GlStateManager.DstFactor.SRC_COLOR;
                        case "ConstantAlpha" -> GlStateManager.DstFactor.CONSTANT_ALPHA;
                        case "ConstantColor" -> GlStateManager.DstFactor.CONSTANT_COLOR;
                        case "DstAlpha" -> GlStateManager.DstFactor.DST_ALPHA;
                        case "DstColor" -> GlStateManager.DstFactor.DST_COLOR;
                        case "OneMinusConstantAlpha" -> GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA;
                        case "OneMinusConstantColor" -> GlStateManager.DstFactor.ONE_MINUS_CONSTANT_COLOR;
                        case "OneMinusDstAlpha" -> GlStateManager.DstFactor.ONE_MINUS_DST_ALPHA;
                        case "OneMinusDstColor" -> GlStateManager.DstFactor.ONE_MINUS_DST_COLOR;
                        case "OneMinusSrcAlpha" -> GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA;
                        case "OneMinusSrcColor" -> GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR;
                        case "Zero" -> GlStateManager.DstFactor.ZERO;
                        default -> GlStateManager.DstFactor.ONE;
                    };

                    builder.transparency(new Transparency(this.blendSrc + "_" + this.blendDst, () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(srcFactor, dstFactor);
                    }, () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }));
                }

                final VertexFormat vertexFormat;
                if (!this.vertexFields.isEmpty()) {
                    VertexFormat.Builder vertexBuilder = VertexFormat.builder();
                    //         POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL = VertexFormat.
                    //         builder().add("Position", VertexFormatElement.POSITION).add("Color", VertexFormatElement.COLOR)
                    //         .add("UV0", VertexFormatElement.UV_0).add("UV1", VertexFormatElement.UV_1)
                    //         .add("UV2", VertexFormatElement.UV_2).add("Normal", VertexFormatElement.NORMAL).skip(1).build();

                    if (this.vertexFields.contains("Position")) {
                        vertexBuilder.add("Position", VertexFormatElement.POSITION);
                    }
                    if (this.vertexFields.contains("Color")) {
                        vertexBuilder.add("Color", VertexFormatElement.COLOR);
                    }
                    if (this.vertexFields.contains("UV")) {
                        vertexBuilder.add("UV", VertexFormatElement.UV);
                    }
                    if (this.vertexFields.contains("UV0")) {
                        vertexBuilder.add("UV0", VertexFormatElement.UV_0);
                    }
                    if (this.vertexFields.contains("BoneId0")) {
                        // No idea, bold ass assumption
                        vertexBuilder.add("UV1", VertexFormatElement.UV_1);
                        vertexBuilder.add("UV2", VertexFormatElement.UV_2);
                    } else {
                        if (this.vertexFields.contains("UV1")) {
                            vertexBuilder.add("UV1", VertexFormatElement.UV_1);
                        }
                        if (this.vertexFields.contains("UV2")) {
                            vertexBuilder.add("UV2", VertexFormatElement.UV_2);
                        }
                    }
                    if (this.vertexFields.contains("Normal")) {
                        vertexBuilder.add("Normal", VertexFormatElement.NORMAL).skip(1);
                    }
                    vertexFormat = vertexBuilder.build();
                } else {
                    vertexFormat = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
                }

                return RenderLayer.of("custom", vertexFormat, this.defines.contains("LINE_STRIP") ? VertexFormat.DrawMode.LINE_STRIP : VertexFormat.DrawMode.QUADS, 1536, true, true, builder.build(false));
            }));

        }

        public static MaterialInfo emptyMaterial() {
            return new MaterialInfo(new HashSet<>(), new HashSet<>(), new HashSet<>());
        }

        public static Variant emptyVariant() {
            return new Variant(new HashSet<>(), new HashSet<>(), new HashSet<>());
        }

        public static class Variant extends MaterialInfo {
            public Variant(Set<String> states, Set<String> defines, Set<String> vertexFields) {
                super(states, defines, vertexFields);
            }

            @Override
            public Variant clone() {
                final Variant info = new Variant(new HashSet<>(states), new HashSet<>(defines), new HashSet<>(vertexFields));
                info.setBlendDst(blendDst);
                info.setBlendSrc(blendSrc);
                info.setDepthFunc(depthFunc);
                return info;
            }
        }

        @Override
        public MaterialInfo clone() {
            final MaterialInfo info = new MaterialInfo(new HashSet<>(states), new HashSet<>(defines), new HashSet<>(vertexFields));
            info.setBlendDst(blendDst);
            info.setBlendSrc(blendSrc);
            info.setDepthFunc(depthFunc);
            variants.forEach((k, v) -> {
                info.variants.put(k, v.clone());
            });

            return info;
        }
    }
}
