package org.oryxel.viabedrockutility.pack.definitions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.material.Material;
import org.oryxel.viabedrockutility.material.VanillaMaterials;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.pack.content.Content;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MaterialDefinitions {
    private final Map<String, Material> NAME_TO_MATERIAL = new HashMap<>();

    public MaterialDefinitions(final PackManager packManager) {
        for (final Content content : packManager.getPacks()) {
            if (!content.contains("materials/entity.material")) {
                continue;
            }

            try {
                final JsonObject object = JsonParser.parseString(content.getString("materials/entity.material")).getAsJsonObject();
                if (!object.has("materials")) {
                    continue;
                }

                final JsonObject materials = object.getAsJsonObject("materials");
                for (final String elementName : materials.keySet()) {
                    final JsonElement element = materials.get(elementName);
                    if (elementName.equals("version") || !element.isJsonObject()) {
                        continue;
                    }

                    final String[] split = elementName.split(":");
                    if (split.length > 2) {
                        continue;
                    }

                    final Function<Identifier, RenderLayer> function;
                    final String identifier = split[0], baseIdentifier = split.length == 1 ? "" : split[1];
                    if (!baseIdentifier.isBlank() && (VanillaMaterials.hasMaterial(baseIdentifier) || NAME_TO_MATERIAL.containsKey(baseIdentifier))) {
                        if (NAME_TO_MATERIAL.containsKey(baseIdentifier)) {
                            final Material material = NAME_TO_MATERIAL.get(baseIdentifier);
                            function = Util.memoize(identifier1 -> {
                                final RenderLayer layer = material.function().apply(identifier1);
                                return VanillaMaterials.build(identifier + baseIdentifier, layer.getExpectedBufferSize(), VanillaMaterials.renderLayerToBuilder(layer, identifier1));
                            });
                        } else {
                            final Function<Identifier, RenderLayer> oldFunction = VanillaMaterials.getFunction(baseIdentifier);
                            function = Util.memoize(identifier1 -> {
                                final RenderLayer layer = oldFunction.apply(identifier1);
                                return VanillaMaterials.build(identifier + baseIdentifier, layer.getExpectedBufferSize(), VanillaMaterials.renderLayerToBuilder(layer, identifier1));
                            });
                        }
                    } else {
                        function = Util.memoize(identifier1 -> {
                            final RenderLayer solid = RenderLayer.getEntitySolid(identifier1);
                            return VanillaMaterials.build(identifier + baseIdentifier, solid.getExpectedBufferSize(), VanillaMaterials.renderLayerToBuilder(solid, identifier1));
                        });
                    }

                    // TODO: Parsing time!

                    NAME_TO_MATERIAL.put(identifier, new Material(identifier, baseIdentifier, function));
                }
            } catch (Throwable e) {
                ViaBedrockUtilityFabric.LOGGER.warn("Failed to parse entity material!");
            }
        }
    }

    public RenderLayer getRenderLayer(final String name, final Identifier identifier) {
        if (!NAME_TO_MATERIAL.containsKey(name)) {
            if (VanillaMaterials.hasMaterial(name)) {
                return VanillaMaterials.getRenderLayer(name, identifier);
            }

            return RenderLayer.getEntitySolid(identifier);
        }

        return NAME_TO_MATERIAL.get(name).function().apply(identifier);
    }
}
