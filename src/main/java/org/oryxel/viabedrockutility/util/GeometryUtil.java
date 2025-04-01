package org.oryxel.viabedrockutility.util;

import com.google.common.collect.Maps;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.math.Direction;
import org.cube.converter.model.element.Cube;
import org.cube.converter.model.element.Parent;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.util.element.UVMap;

import java.util.*;

public final class GeometryUtil {
    public static String HARDCODED_INDICATOR = "viabedrockutility" + "viabedrockutility".hashCode();

    public static Model buildModel(final BedrockGeometryModel geometry, final boolean player, boolean slim) {
        // There are some times when the skin image file is larger than the geometry UV points.
        // In this case, we need to scale UV calls
        // https://github.com/Camotoy/BedrockSkinUtility/issues/9
        final float uvWidth = geometry.getTextureSize().getX();
        final float uvHeight = geometry.getTextureSize().getY();

        final Map<String, PartInfo> stringToPart = new HashMap<>();
        for (final Parent bone : geometry.getParents()) {
            final Map<String, ModelPart> children = Maps.newHashMap();
            final ModelPart part = new ModelPart(List.of(), children);

            part.setOrigin(bone.getPivot().getX(), -bone.getPivot().getY() + 24, bone.getPivot().getZ());
            part.setAngles(bone.getRotation().getX() * MathUtil.DEGREES_TO_RADIANS, bone.getRotation().getY() * MathUtil.DEGREES_TO_RADIANS, bone.getRotation().getZ() * MathUtil.DEGREES_TO_RADIANS);
            // Seems to be important or else the pivot and rotation will be reset.
            part.setDefaultTransform(part.getTransform());

            // Java don't allow individual cubes to have their own rotation therefore, we have to separate each cube into ModelPart to be able to rotate.
            for (final Cube cube : bone.getCubes().values()) {
                final float sizeX = cube.getSize().getX(), sizeY = cube.getSize().getY(), sizeZ = cube.getSize().getZ();
                final float inflate = cube.getInflate();

                final UVMap uvMap = cube.getUvMap().clone();

                final Set<Direction> set = new HashSet<>();
                for (final Direction direction : Direction.values()) {
                    if (uvMap.getMap().containsKey(org.cube.converter.util.element.Direction.values()[direction.ordinal()])) {
                        set.add(direction);
                    }
                }

                // Y have to be flipped for whatever reason, also have to offset down by 1.501 block (which is 24.016 in model size)?
                final ModelPart.Cuboid cuboid = new ModelPart.Cuboid(0, 0, cube.getPosition().getX(), -(cube.getPosition().getY() - 24.016F + sizeY), cube.getPosition().getZ(), sizeX, sizeY, sizeZ, inflate, inflate, inflate, cube.isMirror(), uvWidth, uvHeight, set);
                correctUv(cuboid, set, uvMap, uvWidth, uvHeight, cube.getInflate(), cube.isMirror());

                final ModelPart cubePart = new ModelPart(List.of(cuboid), Map.of(HARDCODED_INDICATOR, new ModelPart(List.of(), Map.of())));
                cubePart.setOrigin(cube.getPivot().getX(), -cube.getPivot().getY() + 24, cube.getPivot().getZ());
                cubePart.setAngles(cube.getRotation().getX() * MathUtil.DEGREES_TO_RADIANS, cube.getRotation().getY() * MathUtil.DEGREES_TO_RADIANS, cube.getRotation().getZ() * MathUtil.DEGREES_TO_RADIANS);
                cubePart.setDefaultTransform(cubePart.getTransform());
                children.put(cube.getParent() + cube.hashCode(), cubePart);
            }

            String parent = bone.getParent();
            String name = bone.getName();
            if (player) {
                switch (name.toLowerCase(Locale.ROOT)) { // Also do this with the overlays? Those are final, though.
                    case "head", "rightarm", "body", "leftarm", "leftleg", "rightleg" -> parent = "root";
                }
            }

            stringToPart.put(adjustFormatting(name), new PartInfo(adjustFormatting(parent), part, children));
        }

        PartInfo root = stringToPart.get("root");
        if (root == null) {
            final Map<String, ModelPart> rootParts = Maps.newHashMap();
            stringToPart.put("root", root = new PartInfo("", new ModelPart(List.of(), rootParts), rootParts));
        } else if (!player) {
            final Map<String, ModelPart> rootParts = Maps.newHashMap();
            root = new PartInfo("", new ModelPart(List.of(), rootParts), rootParts);
        }

        for (Map.Entry<String, PartInfo> entry : stringToPart.entrySet()) {
            entry.getValue().children.put(HARDCODED_INDICATOR, new ModelPart(List.of(), Map.of()));

            if (entry.getValue().parent.isBlank() && entry.getValue().part() != root.part) {
                root.children.put(entry.getKey(), entry.getValue().part());
                continue;
            }

            PartInfo parentPart = stringToPart.get(entry.getValue().parent);
            if (parentPart != null) {
                parentPart.children.put(entry.getKey(), entry.getValue().part);
            }
        }

        if (player) {
            // Not really sure if this still belongs to the player model.
//            root.children.computeIfAbsent("cloak", (string) -> // Required to allow a cape to render
//                    BipedEntityModel.getModelData(Dilation.NONE, 0.0F).getRoot().addChild(string,
//                            ModelPartBuilder.create()
//                                    .uv(0, 0)
//                                    .cuboid(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, Dilation.NONE, 1.0F, 0.5F),
//                            ModelTransform.pivot(0.0F, 0.0F, 0.0F)).createPart(64, 64));

            ensureAvailable(stringToPart, root.children, "head");
            ensureAvailable(stringToPart, root.children, "body");
            ensureAvailable(stringToPart, root.children, "left_arm");
            ensureAvailable(stringToPart, root.children, "right_arm");
            ensureAvailable(stringToPart, root.children, "left_leg");
            ensureAvailable(stringToPart, root.children, "right_leg");

            ensureAvailable(stringToPart, stringToPart.get("head").children, "hat");

            ensureAvailable(stringToPart, stringToPart.get("left_arm").children, "left_sleeve");
            ensureAvailable(stringToPart, stringToPart.get("right_arm").children, "right_sleeve");
            ensureAvailable(stringToPart, stringToPart.get("left_leg").children, "left_pants");
            ensureAvailable(stringToPart, stringToPart.get("right_leg").children, "right_pants");

            ensureAvailable(stringToPart, stringToPart.get("body").children, "jacket");
        }

        return player ? new PlayerEntityModel(root.part(), slim) : new EntityModel<>(root.part()) {};
    }

    private static String adjustFormatting(String name) {
        if (name == null) {
            return null;
        }

        return switch (name.toLowerCase(Locale.ROOT)) {
            case "leftarm" -> "left_arm";
            case "rightarm" -> "right_arm";
            case "leftleg" -> "left_leg";
            case "rightleg" -> "right_leg";
            default -> name.toLowerCase(Locale.ROOT);
        };
    }

    private static void ensureAvailable(Map<String, PartInfo> stringToPart, Map<String, ModelPart> children, String name) {
        final Map<String, ModelPart> children1 = Maps.newHashMap();
        final ModelPart part = new ModelPart(Collections.emptyList(), children1);
        stringToPart.putIfAbsent(name, new PartInfo("", part, children1));
        children.putIfAbsent(name, part);
    }

    private record PartInfo(String parent, ModelPart part, Map<String, ModelPart> children) {
    }

    private static void correctUv(final ModelPart.Cuboid cuboid, final Set<Direction> set, final UVMap map, final float uvWidth, final float uvHeight, final float inflate, final boolean mirror) {
        float x = cuboid.minX, y = cuboid.minY, z = cuboid.minZ;
        float f = cuboid.maxX, g = cuboid.maxY, h = cuboid.maxZ;

        x -= inflate;
        y -= inflate;
        z -= inflate;
        f += inflate;
        g += inflate;
        h += inflate;

        if (mirror) {
            float i = f;
            f = x;
            x = i;
        }

        ModelPart.Vertex vertex = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
        ModelPart.Vertex vertex2 = new ModelPart.Vertex(f, y, z, 0.0F, 8.0F);
        ModelPart.Vertex vertex3 = new ModelPart.Vertex(f, g, z, 8.0F, 8.0F);
        ModelPart.Vertex vertex4 = new ModelPart.Vertex(x, g, z, 8.0F, 0.0F);
        ModelPart.Vertex vertex5 = new ModelPart.Vertex(x, y, h, 0.0F, 0.0F);
        ModelPart.Vertex vertex6 = new ModelPart.Vertex(f, y, h, 0.0F, 8.0F);
        ModelPart.Vertex vertex7 = new ModelPart.Vertex(f, g, h, 8.0F, 8.0F);
        ModelPart.Vertex vertex8 = new ModelPart.Vertex(x, g, h, 8.0F, 0.0F);

        final ModelPart.Quad[] sides = cuboid.sides;
        int s = 0;

        if (set.contains(Direction.DOWN)) {
            final Float[] uv = map.getMap().get(org.cube.converter.util.element.Direction.DOWN);
            sides[s++] = new ModelPart.Quad(new ModelPart.Vertex[]{vertex6, vertex5, vertex, vertex2}, uv[0], uv[1], uv[2], uv[3], uvWidth, uvHeight, mirror, Direction.DOWN);
        }

        if (set.contains(Direction.UP)) {
            final Float[] uv = map.getMap().get(org.cube.converter.util.element.Direction.UP);
            sides[s++] = new ModelPart.Quad(new ModelPart.Vertex[]{vertex3, vertex4, vertex8, vertex7}, uv[0], uv[1], uv[2], uv[3], uvWidth, uvHeight, mirror, Direction.UP);
        }

        if (set.contains(Direction.WEST)) {
            final Float[] uv = map.getMap().get(org.cube.converter.util.element.Direction.WEST);
            sides[s++] = new ModelPart.Quad(new ModelPart.Vertex[]{vertex, vertex5, vertex8, vertex4}, uv[0], uv[1], uv[2], uv[3], uvWidth, uvHeight, mirror, Direction.WEST);
        }

        if (set.contains(Direction.NORTH)) {
            final Float[] uv = map.getMap().get(org.cube.converter.util.element.Direction.NORTH);
            sides[s++] = new ModelPart.Quad(new ModelPart.Vertex[]{vertex2, vertex, vertex4, vertex3}, uv[0], uv[1], uv[2], uv[3], uvWidth, uvHeight, mirror, Direction.NORTH);
        }

        if (set.contains(Direction.EAST)) {
            final Float[] uv = map.getMap().get(org.cube.converter.util.element.Direction.EAST);
            sides[s++] = new ModelPart.Quad(new ModelPart.Vertex[]{vertex6, vertex2, vertex3, vertex7}, uv[0], uv[1], uv[2], uv[3], uvWidth, uvHeight, mirror, Direction.EAST);
        }

        if (set.contains(Direction.SOUTH)) {
            final Float[] uv = map.getMap().get(org.cube.converter.util.element.Direction.SOUTH);
            sides[s] = new ModelPart.Quad(new ModelPart.Vertex[]{vertex5, vertex6, vertex7, vertex8}, uv[0], uv[1], uv[2], uv[3], uvWidth, uvHeight, mirror, Direction.SOUTH);
        }
    }
}