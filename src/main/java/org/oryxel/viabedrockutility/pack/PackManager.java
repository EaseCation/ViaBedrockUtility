package org.oryxel.viabedrockutility.pack;

import lombok.Getter;
import org.oryxel.viabedrockutility.pack.content.Content;
import org.oryxel.viabedrockutility.pack.definitions.*;
import org.oryxel.viabedrockutility.pack.definitions.controller.*;
import org.oryxel.viabedrockutility.pack.processor.TextureProcessor;

import java.util.List;

@Getter
public class PackManager {
    private final List<Content> packs;
    private final RenderControllerDefinitions renderControllerDefinitions;
    private final EntityDefinitions entityDefinitions;
    private final ModelDefinitions modelDefinitions;
    private final MaterialDefinitions materialDefinitions;
    private final AnimationDefinitions animationDefinitions;

    public PackManager(final List<Content> packs) {
        this.packs = packs;

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
