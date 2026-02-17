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
    public static class Interpolations {
        public static final Interpolation LINEAR = (scope, dest, delta, keyframes, start, end, scale) -> {
            Vector3f v1 = eval(scope, keyframes[start].postTarget());
            Vector3f v2 = eval(scope, keyframes[end].preTarget());
            return v1.lerp(v2, delta, dest).mul(scale);
        };
        public static final Interpolation STEP = (scope, dest, delta, keyframes, start, end, scale) -> {
            Vector3f v = eval(scope, keyframes[start].postTarget());
            dest.set(v.x() * scale, v.y() * scale, v.z() * scale);
            return dest;
        };
        public static final Interpolation CUBIC = (scope, dest, delta, keyframes, start, end, scale) -> {
            // Control point availability (Blockbench: skip before_plus/after_plus if neighbor has separate pre/post)
            boolean hasBefore = start > 0 && !keyframes[start].hasSeparatePrePost();
            boolean hasAfter = end < keyframes.length - 1 && !keyframes[end].hasSeparatePrePost();

            Vector3f p1 = eval(scope, keyframes[start].postTarget());
            Vector3f p2 = eval(scope, keyframes[end].preTarget());
            Vector3f p0 = hasBefore ? eval(scope, keyframes[start - 1].postTarget()) : p1;
            Vector3f p3 = hasAfter ? eval(scope, keyframes[end + 1].preTarget()) : p2;

            dest.set(
                    MathHelper.catmullRom(delta, p0.x(), p1.x(), p2.x(), p3.x()) * scale,
                    MathHelper.catmullRom(delta, p0.y(), p1.y(), p2.y(), p3.y()) * scale,
                    MathHelper.catmullRom(delta, p0.z(), p1.z(), p2.z(), p3.z()) * scale
            );
            return dest;
        };
    }

    private static Vector3f eval(Scope scope, String[] molang3) {
        try {
            return new Vector3f((float) MoLangEngine.eval(scope, molang3[0]).getAsNumber(),
                    (float) MoLangEngine.eval(scope, molang3[1]).getAsNumber(),
                    (float) MoLangEngine.eval(scope, molang3[2]).getAsNumber());
        } catch (IOException e) {
            e.printStackTrace();
            return new Vector3f();
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


