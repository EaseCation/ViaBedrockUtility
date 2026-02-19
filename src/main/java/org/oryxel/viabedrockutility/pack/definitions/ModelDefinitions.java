// This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock - Copyright 2025 - 2026
package org.oryxel.viabedrockutility.pack.definitions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.parser.bedrock.geometry.BedrockGeometryParser;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.pack.content.Content;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ModelDefinitions {
    private final Map<String, BedrockGeometryModel> entityModels = new HashMap<>();
    private final Map<String, VisibleBounds> visibleBoundsMap = new HashMap<>();

    public ModelDefinitions(final PackManager packManager) {
        for (final Content content : packManager.getPacks()) {
            for (final String modelPath : content.getFilesDeep("models/", ".json")) {
                try {
                    final String jsonStr = content.getString(modelPath);
                    for (final BedrockGeometryModel bedrockGeometry : BedrockGeometryParser.parse(jsonStr)) {
                        if (modelPath.startsWith("models/entity/")) {
                            this.entityModels.put(bedrockGeometry.getIdentifier(), bedrockGeometry);
                        }
                    }
                    // Extract visible_bounds from raw JSON (CubeConverter doesn't parse these)
                    if (modelPath.startsWith("models/entity/")) {
                        parseVisibleBounds(jsonStr);
                    }
                } catch (Throwable e) {
                    ViaBedrockUtilityFabric.LOGGER.warn("Failed to parse model definition {}", modelPath);
                }
            }
        }
    }

    private void parseVisibleBounds(String jsonStr) {
        try {
            JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
            JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
            if (geometries == null) return;
            for (JsonElement geomElem : geometries) {
                JsonObject geom = geomElem.getAsJsonObject();
                JsonObject desc = geom.getAsJsonObject("description");
                if (desc == null || !desc.has("identifier")) continue;
                String identifier = desc.get("identifier").getAsString();

                float width = desc.has("visible_bounds_width") ? desc.get("visible_bounds_width").getAsFloat() : 1.0f;
                float height = desc.has("visible_bounds_height") ? desc.get("visible_bounds_height").getAsFloat() : 2.0f;
                float ox = 0, oy = 1, oz = 0;
                if (desc.has("visible_bounds_offset")) {
                    JsonArray offset = desc.getAsJsonArray("visible_bounds_offset");
                    if (offset != null && offset.size() >= 3) {
                        ox = offset.get(0).getAsFloat();
                        oy = offset.get(1).getAsFloat();
                        oz = offset.get(2).getAsFloat();
                    }
                }
                this.visibleBoundsMap.put(identifier, new VisibleBounds(width, height, ox, oy, oz));
            }
        } catch (Throwable ignored) {
            // Silently ignore parse errors for visible bounds
        }
    }
}
