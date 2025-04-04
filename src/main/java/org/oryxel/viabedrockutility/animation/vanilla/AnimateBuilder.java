package org.oryxel.viabedrockutility.animation.vanilla;

import org.oryxel.viabedrockutility.animation.Animation;
import org.oryxel.viabedrockutility.animation.element.Cube;
import org.oryxel.viabedrockutility.animation.element.timestamp.ComplexTimeStamp;
import org.oryxel.viabedrockutility.animation.element.timestamp.SimpleTimeStamp;
import org.oryxel.viabedrockutility.util.mojangweirdformat.ValueOrValue;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.oryxel.viabedrockutility.animation.vanilla.AnimateTransformation.Targets.*;
import static org.oryxel.viabedrockutility.animation.vanilla.AnimateTransformation.*;

public class AnimateBuilder {
    public static VBUAnimation build(final Animation animation) {
        float length = animation.getAnimationLength();
        if (length == -1) {
            float largestTimeStamp = 1;
            for (Cube cube : animation.getCubes()) {
                if (cube.getPosition() != null) {
                    if (cube.getPosition().getValue() instanceof TreeMap<?, ?> rawMap) {
                        final TreeMap<Float, ValueOrValue<?>> map = (TreeMap<Float, ValueOrValue<?>>) rawMap;
                        for (Float v : map.keySet()) {
                            if (largestTimeStamp < v) {
                                largestTimeStamp = v;
                            }
                        }
                    }
                }

                if (cube.getRotation() != null) {
                    if (cube.getRotation().getValue() instanceof TreeMap<?, ?> rawMap) {
                        final TreeMap<Float, ValueOrValue<?>> map = (TreeMap<Float, ValueOrValue<?>>) rawMap;
                        for (Float v : map.keySet()) {
                            if (largestTimeStamp < v) {
                                largestTimeStamp = v;
                            }
                        }
                    }
                }

                if (cube.getScale() != null) {
                    if (cube.getScale().getValue() instanceof TreeMap<?, ?> rawMap) {
                        final TreeMap<Float, ValueOrValue<?>> map = (TreeMap<Float, ValueOrValue<?>>) rawMap;
                        for (Float v : map.keySet()) {
                            if (largestTimeStamp < v) {
                                largestTimeStamp = v;
                            }
                        }
                    }
                }
            }

            if (largestTimeStamp != 1) {
                largestTimeStamp += 0.01F;
            }
            length = largestTimeStamp;
        }

        final VBUAnimation.Builder builder = VBUAnimation.Builder.create(length);
        for (Cube cube : animation.getCubes()) {
            if (cube.getPosition() != null) {
                build(builder, cube.getIdentifier(), OFFSET, cube.getPosition().getValue());
            }
            if (cube.getRotation() != null) {
                build(builder, cube.getIdentifier(), ROTATE, cube.getRotation().getValue());
            }
            if (cube.getScale() != null) {
                build(builder, cube.getIdentifier(), SCALE, cube.getScale().getValue());
            }
        }

        if (animation.getLoop().getValue().equals(true)) {
            builder.looping();
        }

        return builder.build();
    }

    private static void build(final VBUAnimation.Builder builder, final String name, final Target target, final Object object) {
        if (object instanceof TreeMap<?, ?> rawMap) {
            if (rawMap.isEmpty()) {
                return;
            }

            VBUKeyFrame[] frames = new VBUKeyFrame[rawMap.size()];
            final TreeMap<Float, ValueOrValue<?>> map = (TreeMap<Float, ValueOrValue<?>>) rawMap;
            final Queue<Map.Entry<Float, ValueOrValue<?>>> entries = new ConcurrentLinkedQueue<>();
            map.entrySet().forEach(entries::add);

            int index = 0;
            String[] past = null;
            Map.Entry<Float, ValueOrValue<?>> entry;
            while ((entry = entries.peek()) != null) {
                float timestamp = entry.getKey();

                if (entry.getValue().getValue() instanceof SimpleTimeStamp simple) {
                    frames[index] = (new VBUKeyFrame(timestamp, simple.value(), Interpolations.CUBIC));
                } else if (entry.getValue().getValue() instanceof ComplexTimeStamp complex) {
                    // Tf is pre post.... lerp to the next one using post and pre is the starting frame or whatever.

                    Interpolation interpolation = complex.lerpMode().equalsIgnoreCase("catmullrom") ? Interpolations.CUBIC : Interpolations.LINEAR;

                    if (past != null) {
                        frames[index] = (new VBUKeyFrame(timestamp, past, interpolation));
                        past = null;
                    }

                    if (complex.pre() != null) {
                        frames[index] = (new VBUKeyFrame(timestamp, complex.pre(), interpolation));
                    }

                    if (complex.post() != null) {
                        past = complex.post();
                    }
                }

                entries.poll();

                index++;
            }

            // In case if any frames is null for whatever reason, which does happen.
            int availableFrames = rawMap.size();
            for (VBUKeyFrame frame : frames) {
                if (frame == null) {
                    availableFrames--;
                }
            }

            if (availableFrames < 1) {
                return;
            }

            VBUKeyFrame[] newFrames = new VBUKeyFrame[availableFrames];

            int n = 0;
            for (VBUKeyFrame oldFrame : frames) {
                if (oldFrame != null) {
                    newFrames[n] = oldFrame;
                    n++;
                }
            }

            builder.addBoneAnimation(name, new AnimateTransformation(target, newFrames));
        } else {
            builder.addBoneAnimation(name, new AnimateTransformation(target, new VBUKeyFrame[] {new VBUKeyFrame(0, get(object), AnimateTransformation.Interpolations.CUBIC)}));
        }
    }

    private static String[] get(final Object object) {
        if (object instanceof String[] array) {
            return array;
        } else if (object instanceof Float pos) {
            return new String[] {String.valueOf(pos), String.valueOf(pos), String.valueOf(pos)};
        } else if (object instanceof String pos) {
            return new String[] {pos, pos, pos};
        }

        return new String[] {"0", "0", "0"};
    }
}
