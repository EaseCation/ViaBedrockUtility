package org.oryxel.viabedrockutility.pack.definitions;

import lombok.Getter;
import org.oryxel.viabedrockutility.animation.controller.AnimationController;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.pack.PackManager;
import org.oryxel.viabedrockutility.pack.content.Content;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads all animation controller definitions from resource packs.
 * Scans {@code animation_controllers/*.json} files.
 */
@Getter
public class AnimationControllerDefinitions {
    private final Map<String, AnimationController> controllers = new HashMap<>();

    public AnimationControllerDefinitions(final PackManager packManager) {
        for (final Content content : packManager.getPacks()) {
            for (final String path : content.getFilesDeep("animation_controllers/", ".json")) {
                try {
                    for (final AnimationController controller : AnimationController.parse(content.getJson(path))) {
                        this.controllers.put(controller.getIdentifier(), controller);
                    }
                } catch (Throwable e) {
                    ViaBedrockUtilityFabric.LOGGER.warn("Failed to parse animation controller definition {}", path, e);
                }
            }
        }

        if (!this.controllers.isEmpty()) {
            ViaBedrockUtilityFabric.LOGGER.debug("[PackManager] Loaded {} animation controllers", this.controllers.size());
        }
    }
}
