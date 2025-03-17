package org.oryxel.viabedrockpack.manager;

import net.minecraft.util.Identifier;
import org.oryxel.viabedrockpack.entity.renderer.model.CustomEntityModel;

import java.util.HashMap;
import java.util.Map;


public class CustomEntityManager {
    private static final Map<String, Data> entities = new HashMap<>();

    public static void put(final String name, final CustomEntityModel model, Identifier texture) {
        entities.put(name, new Data(name, model, texture));
    }

    public static Data get(final String name) {
        return entities.get(name);
    }

    public record Data(String name, CustomEntityModel model, Identifier texture) {}
}
