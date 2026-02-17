package org.oryxel.viabedrockutility.animation.vanilla;

import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import team.unnamed.mocha.runtime.Scope;

import java.io.IOException;

// Taken from vanilla Transformation.
public record AnimateTransformation(Target target, VBUKeyFrame[] keyframes) {
    // Static temp vectors for interpolation (render thread is single-threaded)
    private static final Vector3f TEMP_V0 = new Vector3f();
    private static final Vector3f TEMP_V1 = new Vector3f();
    private static final Vector3f TEMP_V2 = new Vector3f();
    private static final Vector3f TEMP_V3 = new Vector3f();

    public static class Interpolations {
        public static final Interpolation LINEAR = (scope, dest, delta, keyframes, start, end, scale) -> {
            eval(scope, keyframes[start].postTarget(), TEMP_V1);
            eval(scope, keyframes[end].preTarget(), TEMP_V2);
            return TEMP_V1.lerp(TEMP_V2, delta, dest).mul(scale);
        };
        public static final Interpolation STEP = (scope, dest, delta, keyframes, start, end, scale) -> {
            eval(scope, keyframes[start].postTarget(), dest);
            dest.mul(scale);
            return dest;
        };
        public static final Interpolation CUBIC = (scope, dest, delta, keyframes, start, end, scale) -> {
            // Control point availability (Blockbench: skip before_plus/after_plus if neighbor has separate pre/post)
            boolean hasBefore = start > 0 && !keyframes[start].hasSeparatePrePost();
            boolean hasAfter = end < keyframes.length - 1 && !keyframes[end].hasSeparatePrePost();

            eval(scope, keyframes[start].postTarget(), TEMP_V1);
            eval(scope, keyframes[end].preTarget(), TEMP_V2);
            if (hasBefore) {
                eval(scope, keyframes[start - 1].postTarget(), TEMP_V0);
            } else {
                TEMP_V0.set(TEMP_V1);
            }
            if (hasAfter) {
                eval(scope, keyframes[end + 1].preTarget(), TEMP_V3);
            } else {
                TEMP_V3.set(TEMP_V2);
            }

            dest.set(
                    MathHelper.catmullRom(delta, TEMP_V0.x(), TEMP_V1.x(), TEMP_V2.x(), TEMP_V3.x()) * scale,
                    MathHelper.catmullRom(delta, TEMP_V0.y(), TEMP_V1.y(), TEMP_V2.y(), TEMP_V3.y()) * scale,
                    MathHelper.catmullRom(delta, TEMP_V0.z(), TEMP_V1.z(), TEMP_V2.z(), TEMP_V3.z()) * scale
            );
            return dest;
        };
    }

    private static void eval(Scope scope, String[] molang3, Vector3f dest) {
        try {
            dest.set(
                    (float) MoLangEngine.eval(scope, molang3[0]).getAsNumber(),
                    (float) MoLangEngine.eval(scope, molang3[1]).getAsNumber(),
                    (float) MoLangEngine.eval(scope, molang3[2]).getAsNumber()
            );
        } catch (IOException e) {
            e.printStackTrace();
            dest.set(0, 0, 0);
        }
    }

    public static class Targets {
        public static final Target OFFSET = (part, vec3) -> ((IModelPart)((Object)part)).viaBedrockUtility$setOffset(vec3);
        public static final Target ROTATE = (part, vec3) -> ((IModelPart)((Object)part)).viaBedrockUtility$setAngles(vec3);
        public static final Target SCALE = (part, vec3) -> {
            part.xScale = vec3.x;
            part.yScale = vec3.y;
            part.zScale = vec3.z;
        };
    }

    public interface Target {
        void apply(ModelPart var1, Vector3f var2);
    }

    public interface Interpolation {
        Vector3f apply(Scope scope, Vector3f var1, float var2, VBUKeyFrame[] var3, int var4, int var5, float var6);
    }
}
