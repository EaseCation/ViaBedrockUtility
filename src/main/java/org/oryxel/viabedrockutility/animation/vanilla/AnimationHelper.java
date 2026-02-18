package org.oryxel.viabedrockutility.animation.vanilla;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.oryxel.viabedrockutility.animation.Animation;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import team.unnamed.mocha.runtime.Scope;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AnimationHelper {
    public static void animate(Scope scope, Model model, VBUAnimation animation, long runningTime, float scale, Vector3f tempVec) {
        float g = AnimationHelper.getRunningSeconds(animation, runningTime);
        for (Map.Entry<String, List<AnimateTransformation>> entry : animation.boneAnimations().entrySet()) {
            Optional<ModelPart> optional = getPartByName(model.getParts(), entry.getKey());
            if (optional.isEmpty()) {
                continue;
            }

            final ModelPart part = optional.get();
            List<AnimateTransformation> list = entry.getValue();
            for (AnimateTransformation transformation : list) {
                VBUKeyFrame[] lvs = transformation.keyframes();
                int i = Math.max(0, MathHelper.binarySearch(0, lvs.length, index -> {
                    if (lvs[index] == null) {
                        return false;
                    }

                    return g <= lvs[index].timestamp();
                }) - 1);
                int j = Math.min(lvs.length - 1, i + 1);
                if (lvs[i] == null || lvs[j] == null) {
                    continue;
                }

                VBUKeyFrame lv = lvs[i];
                VBUKeyFrame lv2 = lvs[j];
                float h = g - lv.timestamp();
                float k = j != i ? MathHelper.clamp(h / (lv2.timestamp() - lv.timestamp()), 0.0f, 1.0f) : 1F;

                // Select interpolation type following Blockbench logic:
                // step takes priority, then catmullrom if either side uses it
                AnimateTransformation.Interpolation interp;
                if (lv.interpolation() == AnimateTransformation.Interpolations.STEP) {
                    interp = AnimateTransformation.Interpolations.STEP;
                } else if (lv.interpolation() == AnimateTransformation.Interpolations.CUBIC
                        || lv2.interpolation() == AnimateTransformation.Interpolations.CUBIC) {
                    interp = AnimateTransformation.Interpolations.CUBIC;
                } else {
                    interp = lv2.interpolation();
                }
                interp.apply(scope, tempVec, k, lvs, i, j, scale);
                transformation.target().apply(part, tempVec);
            }
        }
    }

    private static float getRunningSeconds(VBUAnimation animation, long runningTime) {
        float f = (float)runningTime / 1000.0f;
        if (!animation.looping() || animation.lengthInSeconds() <= 0) {
            return f;
        }
        return f % animation.lengthInSeconds();
    }

    public static float getRunningSeconds(Animation animation, VBUAnimation vbu, long runningTime) {
        float f = (float)runningTime / 1000.0f;
        if (!animation.getLoop().getValue().equals(true) || vbu.lengthInSeconds() <= 0) {
            return f;
        }
        return f % vbu.lengthInSeconds();
    }

    private static Optional<ModelPart> getPartByName(List<ModelPart> parts, String name) {
        for (ModelPart part : parts) {
            if (((IModelPart)((Object)part)).viaBedrockUtility$getName().equalsIgnoreCase(name) && part.isEmpty()) {
                return Optional.of(part);
            }
        }

        return Optional.empty();
    }
}