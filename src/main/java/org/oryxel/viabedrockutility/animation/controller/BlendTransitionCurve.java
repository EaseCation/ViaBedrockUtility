package org.oryxel.viabedrockutility.animation.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a Bedrock animation controller {@code blend_transition} value.
 * <p>
 * Two formats are supported:
 * <ul>
 *   <li><b>Simple float</b>: {@code "blend_transition": 0.15} — linear fade-out over 0.15 seconds</li>
 *   <li><b>Keyframe object</b>: {@code "blend_transition": {"0.0": 1, "0.14": 0.896, ..., "0.7": 0}}
 *       — custom weight curve with linear interpolation between keyframes</li>
 * </ul>
 * Values represent the <b>outgoing</b> state's weight (1.0 = fully visible, 0.0 = fully faded).
 */
public class BlendTransitionCurve {
    public static final BlendTransitionCurve NONE = new BlendTransitionCurve(0, null);

    private final float duration;
    private final TreeMap<Float, Float> keyframes; // null = linear fade

    private BlendTransitionCurve(float duration, TreeMap<Float, Float> keyframes) {
        this.duration = duration;
        this.keyframes = keyframes;
    }

    public static BlendTransitionCurve ofDuration(float duration) {
        if (duration <= 0) return NONE;
        return new BlendTransitionCurve(duration, null);
    }

    public static BlendTransitionCurve ofKeyframes(TreeMap<Float, Float> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) return NONE;
        return new BlendTransitionCurve(keyframes.lastKey(), keyframes);
    }

    /**
     * Parse from a JSON element (either a number or an object).
     */
    public static BlendTransitionCurve parse(JsonElement element) {
        if (element == null || element.isJsonNull()) return NONE;

        if (element.isJsonPrimitive()) {
            return ofDuration(element.getAsFloat());
        }

        if (element.isJsonObject()) {
            final JsonObject obj = element.getAsJsonObject();
            final TreeMap<Float, Float> kf = new TreeMap<>();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                try {
                    kf.put(Float.parseFloat(e.getKey()), e.getValue().getAsFloat());
                } catch (NumberFormatException ignored) {
                }
            }
            return ofKeyframes(kf);
        }

        return NONE;
    }

    public boolean isNone() {
        return duration <= 0;
    }

    public float getDuration() {
        return duration;
    }

    /**
     * Returns the outgoing state's blend weight at the given elapsed time.
     *
     * @param elapsedSeconds seconds since the transition started
     * @return weight in [0, 1]: 1.0 = fully visible, 0.0 = fully faded
     */
    public float getOldStateWeight(float elapsedSeconds) {
        if (isNone()) return 0f;
        if (elapsedSeconds <= 0) return 1f;
        if (elapsedSeconds >= duration) return 0f;

        if (keyframes == null) {
            // Simple linear: 1.0 → 0.0 over duration
            return 1f - (elapsedSeconds / duration);
        }

        // Keyframe interpolation
        final Map.Entry<Float, Float> floor = keyframes.floorEntry(elapsedSeconds);
        final Map.Entry<Float, Float> ceil = keyframes.ceilingEntry(elapsedSeconds);

        if (floor == null && ceil == null) return 0f;
        if (floor == null) return ceil.getValue();
        if (ceil == null) return floor.getValue();
        if (floor.getKey().equals(ceil.getKey())) return floor.getValue();

        final float t = (elapsedSeconds - floor.getKey()) / (ceil.getKey() - floor.getKey());
        return floor.getValue() + (ceil.getValue() - floor.getValue()) * t;
    }
}
