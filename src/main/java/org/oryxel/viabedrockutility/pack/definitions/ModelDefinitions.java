// This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock - Copyright 2025 - 2026
package org.oryxel.viabedrockutility.pack.definitions;

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

    public ModelDefinitions(final PackManager packManager) {
        final Content content = packManager.getContent();

        for (final String modelPath : content.getFilesDeep("models/", ".json")) {
            try {
                for (final BedrockGeometryModel bedrockGeometry : BedrockGeometryParser.parse(content.getString(modelPath))) {
                    if (modelPath.startsWith("models/entity/")) {
                        this.entityModels.put(bedrockGeometry.getIdentifier(), bedrockGeometry);
                    }
                }
            } catch (Throwable e) {
                ViaBedrockUtilityFabric.LOGGER.warn("Failed to parse model definition {}", modelPath);
            }
        }
    }
}
