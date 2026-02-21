package org.oryxel.viabedrockutility.util;

import org.joml.Vector3f;

public class MathUtil {
    public static final float DEGREES_TO_RADIANS = 0.017453292519943295f;

    /**
     * Normalize an angle in degrees to the range [-180, 180).
     */
    public static float normalizeAngleDeg(float angle) {
        angle = angle % 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Linearly interpolate between two euler rotation vectors (in degrees),
     * taking the shortest path on each axis.
     *
     * @param from source rotation
     * @param to   target rotation
     * @param t    interpolation factor (0 = from, 1 = to)
     * @param dest destination vector (may alias from or to)
     */
    public static void shortestPathLerp(Vector3f from, Vector3f to, float t, Vector3f dest) {
        dest.x = from.x + normalizeAngleDeg(to.x - from.x) * t;
        dest.y = from.y + normalizeAngleDeg(to.y - from.y) * t;
        dest.z = from.z + normalizeAngleDeg(to.z - from.z) * t;
    }
}
