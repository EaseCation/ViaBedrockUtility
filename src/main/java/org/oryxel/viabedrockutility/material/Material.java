package org.oryxel.viabedrockutility.material;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public record Material(String identifier, String baseIdentifier, Function<Identifier, RenderLayer> function) {
}
