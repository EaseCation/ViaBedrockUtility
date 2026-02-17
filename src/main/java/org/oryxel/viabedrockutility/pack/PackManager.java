package org.oryxel.viabedrockutility.pack;

import lombok.Getter;
import org.oryxel.viabedrockutility.fabric.ViaBedrockUtilityFabric;
import org.oryxel.viabedrockutility.pack.content.Content;
import org.oryxel.viabedrockutility.pack.definitions.*;
import org.oryxel.viabedrockutility.pack.definitions.controller.*;
import org.oryxel.viabedrockutility.pack.processor.TextureProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Getter
public class PackManager {
    private final List<Content> packs;
    private final RenderControllerDefinitions renderControllerDefinitions;
    private final EntityDefinitions entityDefinitions;
    private final ModelDefinitions modelDefinitions;
    private final MaterialDefinitions materialDefinitions;
    private final AnimationDefinitions animationDefinitions;

    public PackManager(final List<Content> customPacks) {
        final List<Content> allPacks = new ArrayList<>();

        // Load vanilla resource pack as base layer
        try (InputStream is = PackManager.class.getResourceAsStream("/assets/viabedrockutility/vanilla_packs/vanilla.mcpack")) {
            if (is != null) {
                allPacks.add(new Content(is.readAllBytes()));
                ViaBedrockUtilityFabric.LOGGER.info("[PackManager] Loaded vanilla resource pack");
            } else {
                ViaBedrockUtilityFabric.LOGGER.warn("[PackManager] Vanilla resource pack not found in mod resources");
            }
        } catch (IOException e) {
            ViaBedrockUtilityFabric.LOGGER.warn("[PackManager] Failed to load vanilla resource pack", e);
        }

        // Custom packs on top (can override vanilla definitions)
        allPacks.addAll(customPacks);
        this.packs = allPacks;

        this.renderControllerDefinitions = new RenderControllerDefinitions(this);
        this.entityDefinitions = new EntityDefinitions(this);
        this.modelDefinitions = new ModelDefinitions(this);
        this.materialDefinitions = new MaterialDefinitions(this);
        this.animationDefinitions = new AnimationDefinitions(this);

        TextureProcessor.process(packs);

        // Test!
//        for (final Content content : packs) {
//            try {
//                Files.write(new File(content.hashCode() + ".zip").toPath(), content.toZip());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
    }
}
