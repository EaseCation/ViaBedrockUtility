package org.oryxel.viabedrockutility.animation.animator;

import net.minecraft.client.model.ModelPart;
import org.oryxel.viabedrockutility.animation.Animation;
import org.oryxel.viabedrockutility.animation.element.Cube;
import org.oryxel.viabedrockutility.animation.element.timestamp.ComplexTimeStamp;
import org.oryxel.viabedrockutility.animation.element.timestamp.SimpleTimeStamp;
import org.oryxel.viabedrockutility.mixin.interfaces.IModelPart;
import org.oryxel.viabedrockutility.mocha.MoLangEngine;
import org.oryxel.viabedrockutility.renderer.BaseCustomEntityRenderer;
import org.oryxel.viabedrockutility.util.mojangweirdformat.ValueOrValue;
import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Animator {
    private final ModelPart rootModel;
    private final Scope baseScope;
    private final Animation animation;
    private long startMS;
    private boolean started = false, donePlaying = false, firstPlay, shouldUpdateMS;

    private final Set<String> nothingToUpdate = new HashSet<>();

    public Animator(ModelPart rootModel, Scope baseScope, Animation animation) {
        this.rootModel = rootModel;
        this.startMS = System.currentTimeMillis();
        this.animation = animation;

        this.baseScope = baseScope.copy();
        this.firstPlay = true;
    }

    private final Map<String, CubeAnimateData> position = new HashMap<>();
    private final Map<String, CubeAnimateData> rotation = new HashMap<>();
    private final Map<String, CubeAnimateData> scale = new HashMap<>();

    public void update(BaseCustomEntityRenderer.CustomEntityRenderState state) throws IOException {
        List<ModelPart> parts = this.rootModel.traverse().toList();

        boolean runAtleastOnce = false;
        for (Cube cube : this.animation.getCubes()) {
            final ModelPart part = getPartByName(parts, cube.getIdentifier());
            if (part == null || this.nothingToUpdate.contains(cube.getIdentifier() + part.hashCode())) {
                continue;
            }

            runAtleastOnce = true;
        }

        if (!runAtleastOnce) {
            this.stop();
        }

        // Done playing and no loop.
        if (this.donePlaying && this.animation.getLoop().getValue().equals(false)) {
            System.out.println("no looping");
            return;
        }

        final Scope scope = baseScope.copy();

        final MutableObjectBinding queryBinding = new MutableObjectBinding();
        queryBinding.setAllFrom((MutableObjectBinding) this.baseScope.get("query"));

        queryBinding.set("modified_distance_moved", Value.of(state.getDistanceTraveled()));
        queryBinding.set("modified_move_speed", Value.of(0.7F)); // We don't know this value I think? not yet.

        queryBinding.set("body_y_rotation", Value.of(state.getBodyYaw()));
        queryBinding.set("body_x_rotation", Value.of(state.getBodyPitch()));

        scope.set("q", queryBinding);
        scope.set("query", queryBinding);

        if (this.shouldUpdateMS) {
            this.startMS = System.currentTimeMillis();
            this.shouldUpdateMS = false;
        }

        if (!this.started) {
            if (this.firstPlay) {
                if (((float) System.currentTimeMillis() - this.startMS) / 1000L >= MoLangEngine.eval(scope, animation.getStartDelay()).getAsNumber() ) {
                    this.startMS = System.currentTimeMillis();
                    this.started = true;
                    this.firstPlay = false;
                }
            } else {
                if (((float) System.currentTimeMillis() - this.startMS) / 1000L >= MoLangEngine.eval(scope, animation.getLoopDelay()).getAsNumber()) {
                    this.startMS = System.currentTimeMillis();
                    this.started = true;
                    this.donePlaying = false;
                }
            }

            if (this.started && this.animation.isResetBeforePlay()) {
                ((IModelPart)((Object)rootModel)).viaBedrockUtility$resetEverything();
            }
        }

        float animTime = (System.currentTimeMillis() - this.startMS) / 1000f;
        queryBinding.set("anim_time", Value.of(animTime));
        queryBinding.set("life_time", Value.of(animTime));

        if (animation.getAnimationLength() != -1 && animTime >= animation.getAnimationLength()) {
            System.out.println("Reset since animation length: " + animation.getAnimationLength());
            this.stop();
            return;
        }

        for (Cube cube : this.animation.getCubes()) {
            final ModelPart part = getPartByName(parts, cube.getIdentifier());
            if (part == null || this.nothingToUpdate.contains(cube.getIdentifier() + part.hashCode())) {
                continue;
            }

            final String key = cube.getIdentifier() + part.hashCode();

            if (!this.position.containsKey(cube.getIdentifier())) {
                this.position.put(key, new CubeAnimateData());
                this.rotation.put(key, new CubeAnimateData());
                this.scale.put(key, new CubeAnimateData());
            }

            // No fucking idea, the description for this is too vague.
//            if (!cube.getRelativeTo().isBlank() && cube.getRelativeTo().equalsIgnoreCase("entity")) {
//            }

            boolean position = cube.getPosition() == null || update(UpdateType.POSITION, scope, part, this.position.get(key), cube.getPosition().getValue());
            boolean rotation = cube.getRotation() == null || update(UpdateType.ROTATION, scope, part, this.rotation.get(key), cube.getRotation().getValue());
            boolean scale = cube.getScale() == null || update(UpdateType.SCALE, scope, part, this.scale.get(key), cube.getScale().getValue());

            if (position && rotation && scale) {
                this.nothingToUpdate.add(key);
            }
        }
    }

    // Ehm.... shit code incoming, i guess?
    private boolean update(UpdateType type, Scope scope, ModelPart part, CubeAnimateData data, Object object) throws IOException {
        float animTime = (System.currentTimeMillis() - this.startMS) / 1000F;

        final IModelPart iPart = (IModelPart) ((Object)part);

        boolean update = false;
        float valueX = 0, valueY = 0, valueZ = 0;

        boolean done = false;
        if (object instanceof String[] posArray) {
            float x = (float) MoLangEngine.eval(scope, posArray[0]).getAsNumber();
            float y = (float) MoLangEngine.eval(scope, posArray[1]).getAsNumber();
            float z = (float) MoLangEngine.eval(scope, posArray[2]).getAsNumber();
            update = true;
            valueX = x;
            valueY = y;
            valueZ = z;

            done = true;
        } else if (object instanceof Float posNumber) {
            update = true;
            valueX = valueY = valueZ = posNumber;
            done = true;
        } else if (object instanceof String posNumber) {
            float pos = (float) MoLangEngine.eval(scope, posNumber).getAsNumber();
            update = true;
            valueX = valueY = valueZ = pos;
            done = true;
        } else if (object instanceof TreeMap<?, ?> rawMap) {
            final TreeMap<Float, ValueOrValue<?>> map = (TreeMap<Float, ValueOrValue<?>>) rawMap;
            final Queue<Map.Entry<Float, ValueOrValue<?>>> entries = new ConcurrentLinkedQueue<>();
            map.entrySet().forEach(entries::add);

            Map.Entry<Float, ValueOrValue<?>> entry;
            while ((entry = entries.peek()) != null) {
                float timestamp = entry.getKey();
                if (animTime < timestamp) {
                    if (data.available) {
                        data.lerp();
                        update = true;
                        valueX = data.currentX;
                        valueY = data.currentY;
                        valueZ = data.currentZ;
                    }

                    break;
                }

                data.pastTimeFrame = data.currentTimeFrame;
                data.currentTimeFrame = timestamp;
                data.actualTimeFrame = animTime;
                if (data.available) {
                    data.lerp();
                    update = true;
                    valueX = data.currentX;
                    valueY = data.currentY;
                    valueZ = data.currentZ;
                }

                entries.poll();
                // Already play this, don't have to do it twice.
                if (entries.peek() != null && animTime >= timestamp && animTime > entries.peek().getKey()) {
                    continue;
                }

                if (entry.getValue().getValue() instanceof SimpleTimeStamp simple) {
                    data.available = false;

                    float x = (float) MoLangEngine.eval(scope, simple.value()[0]).getAsNumber();
                    float y = (float) MoLangEngine.eval(scope, simple.value()[1]).getAsNumber();
                    float z = (float) MoLangEngine.eval(scope, simple.value()[2]).getAsNumber();
                    update = true;
                    valueX = x;
                    valueY = y;
                    valueZ = z;
                } else if (entry.getValue().getValue() instanceof ComplexTimeStamp complex) {
                    // Tf is pre post.... lerp to the next one using post and pre is the starting frame?
                    if (data.post != null) {
                        data.currentX = (float) MoLangEngine.eval(scope, data.post[0]).getAsNumber();
                        data.currentY = (float) MoLangEngine.eval(scope, data.post[1]).getAsNumber();
                        data.currentZ = (float) MoLangEngine.eval(scope, data.post[2]).getAsNumber();
                    }

                    data.available = true;

                    if (complex.pre() != null) {
                        data.targetX = (float) MoLangEngine.eval(scope, complex.pre()[0]).getAsNumber();
                        data.targetY = (float) MoLangEngine.eval(scope, complex.pre()[1]).getAsNumber();
                        data.targetZ = (float) MoLangEngine.eval(scope, complex.pre()[2]).getAsNumber();
                    }

                    data.post = complex.post();
                    update = true;
                    valueX = data.currentX;
                    valueY = data.currentY;
                    valueZ = data.currentZ;
                }
            }

            if (entries.peek() == null && data.done()) {
                done = true;
            }
        }

        if (update) {
            switch (type) {
                case POSITION -> iPart.viaBedrockUtility$setOffset(valueX, valueY, valueZ);
                case ROTATION -> iPart.viaBedrockUtility$setAngles(valueX, valueY, valueZ);
                case SCALE -> {
                    // We have to flip this (look at BaseCustomEntityRenderer or LivingEntityRenderer)
                    // No need to flip yScale since well the y position itself is already flipped.
                    part.xScale = -valueX;
                    part.yScale = valueY;
                    part.zScale = valueZ;
                }
            }
        }

        return done;
    }

    public void stop() {
        this.started = false;
        this.donePlaying = true;
        this.shouldUpdateMS = true;

        if (!"hold_on_last_frame".equals(this.animation.getLoop().getValue())) {
            ((IModelPart)((Object)rootModel)).viaBedrockUtility$resetEverything();
        }

        this.nothingToUpdate.clear();

        this.position.clear();
        this.rotation.clear();
        this.scale.clear();
    }

    private ModelPart getPartByName(List<ModelPart> parts, String name) {
        for (ModelPart part : parts) {
            if (((IModelPart)((Object)part)).viaBedrockUtility$getName().equals(name) && part.isEmpty()) {
                return part;
            }
        }

        return null;
    }

    private enum UpdateType {
        POSITION, ROTATION, SCALE;
    }

    private static class CubeAnimateData {
        private boolean available;
        private float currentTimeFrame, pastTimeFrame, actualTimeFrame;
        private String type;
        private String[] post;
        private float currentX = Float.MAX_VALUE, currentY, currentZ;
        private float targetX, targetY, targetZ;

        public void lerp() {
            // Ehm todo?
        }

        public boolean done() {
            return actualTimeFrame >= currentTimeFrame;
        }
    }
}
