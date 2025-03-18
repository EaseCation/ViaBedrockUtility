package org.oryxel.viabedrockutility.util;

import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.Direction;
import org.cube.converter.model.element.Cube;
import org.cube.converter.model.element.Parent;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.util.element.UVMap;
import org.oryxel.viabedrockutility.entity.renderer.model.CustomEntityModel;

import java.util.*;

public final class GeometryUtil {
    public static String HARDCODED_INDICATOR = "viabedrockutility" + "viabedrockutility".hashCode();

    public static CustomEntityModel buildCustomModel(final BedrockGeometryModel geometry) {
        // There are some times when the skin image file is larger than the geometry UV points.
        // In this case, we need to scale UV calls
        // https://github.com/Camotoy/BedrockSkinUtility/issues/9
        final float uvWidth = geometry.getTextureSize().getX();
        final float uvHeight = geometry.getTextureSize().getY();

        final Map<String, PartInfo> stringToPart = new HashMap<>();
        for (final Parent bone : geometry.getParents()) {
            final Map<String, ModelPart> children = new HashMap<>();
            final ModelPart part = new ModelPart(List.of(), children);

            part.setPivot(bone.getPivot().getX(), -bone.getPivot().getY() + 24, bone.getPivot().getZ());
            part.setAngles(bone.getRotation().getX() * MathUtil.DEGREES_TO_RADIANS, bone.getRotation().getY() * MathUtil.DEGREES_TO_RADIANS, bone.getRotation().getZ() * MathUtil.DEGREES_TO_RADIANS);
            // Seems to be important or else the pivot and rotation will be reset.
            part.setDefaultTransform(part.getTransform());

            // Java don't allow individual cubes to have their own rotation therefore, we have to separate each cube into ModelPart to be able to rotate.
            for (final Cube cube : bone.getCubes().values()) {
                final float sizeX = cube.getSize().getX(), sizeY = cube.getSize().getY(), sizeZ = cube.getSize().getZ();
                final float inflate = cube.getInflate() + 1.0E-3F;

                final UVMap uvMap = cube.getUvMap().clone();

                final Set<Direction> set = new HashSet<>();
                for (final Direction direction : Direction.values()) {
                    if (uvMap.getMap().containsKey(org.cube.converter.util.element.Direction.values()[direction.ordinal()])) {
                        set.add(direction);
                    }
                }

                // Y have to be flipped for whatever reason, also have to offset down by 1.5 block (which is 24 in model size)?
                final ModelPart.Cuboid cuboid = new ModelPart.Cuboid(0, 0, cube.getPosition().getX(), -(cube.getPosition().getY() - 24 + sizeY), cube.getPosition().getZ(), sizeX, sizeY, sizeZ, inflate, inflate, inflate, cube.isMirror(), uvWidth, uvHeight, set);
                correctUv(cuboid, set, uvMap, uvWidth, uvHeight, cube.isMirror());

                final ModelPart cubePart = new ModelPart(List.of(cuboid), Map.of(HARDCODED_INDICATOR, new ModelPart(List.of(), Map.of())));
                cubePart.setPivot(cube.getPivot().getX(), -cube.getPivot().getY() + 24, cube.getPivot().getZ());
                cubePart.setAngles(cube.getRotation().getX() * MathUtil.DEGREES_TO_RADIANS, cube.getRotation().getY() * MathUtil.DEGREES_TO_RADIANS, cube.getRotation().getZ() * MathUtil.DEGREES_TO_RADIANS);
                cubePart.setDefaultTransform(cubePart.getTransform());
                children.put(cube.getParent() + cube.hashCode(), cubePart);
            }

            stringToPart.put(bone.getName(), new PartInfo(bone.getParent(), part, children));
        }

        final Map<String, ModelPart> rootParts = new HashMap<>();

        for (Map.Entry<String, PartInfo> entry : stringToPart.entrySet()) {
            entry.getValue().children.put(HARDCODED_INDICATOR, new ModelPart(List.of(), Map.of()));

            if (entry.getValue().parent.isBlank()) {
                rootParts.put(entry.getKey(), entry.getValue().part());
                continue;
            }

            PartInfo parentPart = stringToPart.get(entry.getValue().parent);
            if (parentPart != null) {
                parentPart.children.put(entry.getKey(), entry.getValue().part);
            }
        }

        return new CustomEntityModel(new ModelPart(List.of(), rootParts));
    }

    private record PartInfo(String parent, ModelPart part, Map<String, ModelPart> children) {
    }

    private static void correctUv(final ModelPart.Cuboid cuboid, final Set<Direction> set, final UVMap map, final float uvWidth, final float uvHeight, final boolean mirror) {
        float x = cuboid.minX, y = cuboid.minY, z = cuboid.minZ;

        float f = cuboid.maxX, g = cuboid.maxY, h = cuboid.maxZ;
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