package org.oryxel.viabedrockutility.animation;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.oryxel.viabedrockutility.animation.element.Cube;
import org.oryxel.viabedrockutility.util.mojangweirdformat.ValueOrValue;

import java.util.ArrayList;
import java.util.List;

// https://bedrock.dev/docs/stable/Schemas
// https://learn.microsoft.com/en-us/minecraft/creator/documents/animations/animationsoverview?
@RequiredArgsConstructor
@ToString
@Getter
@Setter
public class Animation {
    private final String identifier;
    private ValueOrValue<?> loop;
    private String startDelay = "", loopDelay = "";
    private String timePassExpression = ""; // anim_time_update
    private boolean resetBeforePlay; // override_previous_animation
    private float animationLength = -1;
    private final List<Cube> cubes = new ArrayList<>();

    public static List<Animation> parse(final JsonObject object) {
        final JsonObject animationsList = object.getAsJsonObject("animations");
        if (animationsList == null || animationsList.isEmpty()) {
            return List.of();
        }

        final List<Animation> animations = new ArrayList<>();
        for (String identifier : animationsList.keySet()) {
            if (!animationsList.get(identifier).isJsonObject()) {
                continue;
            }

            final Animation animation = new Animation(identifier);

            JsonObject animationObject = animationsList.getAsJsonObject(identifier);
            if (animationObject.has("loop")) {
                final JsonPrimitive loopElement = animationObject.getAsJsonPrimitive("loop");
                animation.setLoop(new ValueOrValue<>(loopElement.isBoolean() ? loopElement.getAsBoolean() : loopElement.getAsString()));
            } else {
                animation.setLoop(new ValueOrValue<>(false));
            }

            if (animationObject.has("start_delay")) {
                animation.setStartDelay(animationObject.get("start_delay").getAsString());
            }

            if (animationObject.has("loop_delay")) {
                animation.setLoopDelay(animationObject.get("loop_delay").getAsString());
            }

            if (animationObject.has("anim_time_update")) {
                animation.setTimePassExpression(animationObject.get("anim_time_update").getAsString());
            }

            if (animationObject.has("override_previous_animation")) {
                animation.setResetBeforePlay(animationObject.get("override_previous_animation").getAsBoolean());
            }

            if (animationObject.has("animation_length")) {
                animation.setAnimationLength(animationObject.get("animation_length").getAsFloat());
            }

            if (!animationObject.has("bones")) {
                animations.add(animation);
                continue;
            }

            animation.getCubes().addAll(Cube.parse(animationObject.getAsJsonObject("bones")));
            animations.add(animation);
        }

        return animations;
    }
}
