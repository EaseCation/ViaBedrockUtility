package org.oryxel.viabedrockutility.blocks.util;

import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.*;
import org.cube.converter.model.element.Cube;
import org.cube.converter.model.element.Parent;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.util.element.Position2V;
import org.cube.converter.util.element.Position3V;
import org.joml.*;

import java.util.*;

import static org.oryxel.viabedrockutility.util.MathUtil.*;

public class BakedGeometryUtil {
    public static BakedGeometry bake(BedrockGeometryModel geometry, SimpleModel model, ModelTextures textures, ErrorCollectingSpriteGetter ecsg) {
        final Map<String, List<RotationCache>> cacheMap = new HashMap<>();
        for (final Parent bone : geometry.getParents()) {
            final List<RotationCache> caches = new ArrayList<>();

            Parent parent = bone;
            while (parent != null) {
                if (!parent.getRotation().isZero()) {
                    caches.add(new RotationCache(parent.getRotation(), parent.getPivot().withJavaOffset()));
                }

                if (parent.getParent().isBlank()) {
                    break;
                }

                boolean found = false;
                for (final Parent p : geometry.getParents()) {
                    if (p.getName().equals(parent.getParent())) {
                        parent = p;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    parent = null;
                }
            }

            Collections.reverse(caches);
            cacheMap.put(bone.getName(), Collections.unmodifiableList(caches));
        }

        String textureId = "coolD";

        BakedGeometry.Builder lv = new BakedGeometry.Builder();
        for (final Parent bone : geometry.getParents()) {
            for (final Cube cube : bone.getCubes().values()) {
                final List<RotationCache> rotations = new ArrayList<>(cacheMap.get(bone.getName()));
                if (!cube.getRotation().isZero()) {
                    rotations.add(new RotationCache(cube.getRotation(), cube.getPivot().withJavaOffset()));
                }

                cube.getUvMap().getMap().forEach((direction, uvs) -> {
                    if (uvs == null) {
                        return;
                    }

                    lv.add(bake(geometry.getTextureSize(), cube, direction, rotations, ecsg.get(textures, textureId, model), textureId));
                });
            }
        }

        return lv.build();
    }

    private static BakedQuad bake(Position2V textureSize, Cube cube, org.cube.converter.util.element.Direction direction, List<RotationCache> rotations, Sprite sprite, String textureId) {
        final Float[] uvs = cube.getUvMap().clone().toJavaPerfaceUV(textureSize.getX(), textureSize.getY()).getMap().get(direction);

        final Vector3f from = new Vector3f(cube.getPosition().getX() + 8, cube.getPosition().getY(), cube.getPosition().getZ() + 8);
        final Vector3f to = from.add(cube.getSize().getX(), cube.getSize().getY(), cube.getSize().getZ(), new Vector3f());

        ModelElementFace.UV uv;
        if (uvs == null) {
            uv = setDefaultUV(from, to, Direction.values()[direction.ordinal()]);
        } else {
            uv = new ModelElementFace.UV(uvs[0], uvs[1], uvs[2], uvs[3]);
        }

        uv = compactUV(sprite, uv);
        Matrix4fc matrix4fc = new Matrix4f();
        int[] is = packVertexData(uv, matrix4fc, sprite, Direction.values()[direction.ordinal()], getPositionMatrix(from, to), rotations);
        Direction direction2 = decodeDirection(is);
        if (rotations.isEmpty()) {
            encodeDirection(is, direction2);
        }

        return new BakedQuad(is, 0, direction2, sprite, false, 0);
    }

    private static float[] getPositionMatrix(Vector3fc from, Vector3fc to) {
        float[] fs = new float[Direction.values().length];
        fs[CubeFace.DirectionIds.WEST] = from.x() / 16.0F;
        fs[CubeFace.DirectionIds.DOWN] = from.y() / 16.0F;
        fs[CubeFace.DirectionIds.NORTH] = from.z() / 16.0F;
        fs[CubeFace.DirectionIds.EAST] = to.x() / 16.0F;
        fs[CubeFace.DirectionIds.UP] = to.y() / 16.0F;
        fs[CubeFace.DirectionIds.SOUTH] = to.z() / 16.0F;
        return fs;
    }

    private static int[] packVertexData(ModelElementFace.UV texture, Matrix4fc matrix4fc, Sprite sprite, Direction facing, float[] fs, List<RotationCache> caches) {
        CubeFace lv = CubeFace.getFace(facing);
        int[] is = new int[32];
        for (int i = 0; i < 4; ++i) {
            packVertexData(is, i, lv, texture, matrix4fc, fs, sprite, caches);
        }
        return is;
    }

    private static void packVertexData(int[] vertices, int cornerIndex, CubeFace cubeFace, ModelElementFace.UV texture, Matrix4fc matrix4fc, float[] fs, Sprite sprite, List<RotationCache> caches) {
        CubeFace.Corner corner = cubeFace.getCorner(cornerIndex);
        Vector3f vector3f = new Vector3f(fs[corner.xSide], fs[corner.ySide], fs[corner.zSide]);

        for (RotationCache cache : caches) {
            rotateVertex(vector3f, cache.pivot(), cache.rotation());
        }

        float f = ModelElementFace.getUValue(texture, AxisRotation.R0, cornerIndex);
        float g = ModelElementFace.getVValue(texture, AxisRotation.R0, cornerIndex);
        float i;
        float h;
        if (MatrixUtil.isIdentity(matrix4fc)) {
            h = f;
            i = g;
        } else {
            Vector3f vector3f2 = matrix4fc.transformPosition(new Vector3f(setCenterBack(f), setCenterBack(g), 0.0F));
            h = setCenterForward(vector3f2.x);
            i = setCenterForward(vector3f2.y);
        }

        packVertexData(vertices, cornerIndex, vector3f, sprite, h, i);
    }

    private static float setCenterBack(float f) {
        return f - 0.5F;
    }

    private static float setCenterForward(float f) {
        return f + 0.5F;
    }

    private static void packVertexData(int[] vertices, int cornerIndex, Vector3f pos, Sprite sprite, float f, float g) {
        int i = cornerIndex * 8;
        vertices[i] = Float.floatToRawIntBits(pos.x());
        vertices[i + 1] = Float.floatToRawIntBits(pos.y());
        vertices[i + 2] = Float.floatToRawIntBits(pos.z());
        vertices[i + 3] = -1;
        vertices[i + 4] = Float.floatToRawIntBits(sprite.getFrameU(f));
        vertices[i + 4 + 1] = Float.floatToRawIntBits(sprite.getFrameV(g));
    }

    private static void encodeDirection(int[] rotationMatrix, Direction direction) {
        int[] is = new int[rotationMatrix.length];
        System.arraycopy(rotationMatrix, 0, is, 0, rotationMatrix.length);
        float[] fs = new float[Direction.values().length];
        fs[CubeFace.DirectionIds.WEST] = 999.0F;
        fs[CubeFace.DirectionIds.DOWN] = 999.0F;
        fs[CubeFace.DirectionIds.NORTH] = 999.0F;
        fs[CubeFace.DirectionIds.EAST] = -999.0F;
        fs[CubeFace.DirectionIds.UP] = -999.0F;
        fs[CubeFace.DirectionIds.SOUTH] = -999.0F;

        for(int i = 0; i < 4; ++i) {
            int j = 8 * i;
            float f = bakeVectorX(is, j);
            float g = bakeVectorY(is, j);
            float h = bakeVectorZ(is, j);
            if (f < fs[CubeFace.DirectionIds.WEST]) {
                fs[CubeFace.DirectionIds.WEST] = f;
            }

            if (g < fs[CubeFace.DirectionIds.DOWN]) {
                fs[CubeFace.DirectionIds.DOWN] = g;
            }

            if (h < fs[CubeFace.DirectionIds.NORTH]) {
                fs[CubeFace.DirectionIds.NORTH] = h;
            }

            if (f > fs[CubeFace.DirectionIds.EAST]) {
                fs[CubeFace.DirectionIds.EAST] = f;
            }

            if (g > fs[CubeFace.DirectionIds.UP]) {
                fs[CubeFace.DirectionIds.UP] = g;
            }

            if (h > fs[CubeFace.DirectionIds.SOUTH]) {
                fs[CubeFace.DirectionIds.SOUTH] = h;
            }
        }

        CubeFace cubeFace = CubeFace.getFace(direction);

        for(int j = 0; j < 4; ++j) {
            int k = 8 * j;
            CubeFace.Corner corner = cubeFace.getCorner(j);
            float h = fs[corner.xSide];
            float l = fs[corner.ySide];
            float m = fs[corner.zSide];
            rotationMatrix[k] = Float.floatToRawIntBits(h);
            rotationMatrix[k + 1] = Float.floatToRawIntBits(l);
            rotationMatrix[k + 2] = Float.floatToRawIntBits(m);

            for(int n = 0; n < 4; ++n) {
                int o = 8 * n;
                float p = bakeVectorX(is, o);
                float q = bakeVectorY(is, o);
                float r = bakeVectorZ(is, o);
                if (MathHelper.approximatelyEquals(h, p) && MathHelper.approximatelyEquals(l, q) && MathHelper.approximatelyEquals(m, r)) {
                    rotationMatrix[k + 4] = is[o + 4];
                    rotationMatrix[k + 4 + 1] = is[o + 4 + 1];
                }
            }
        }

    }

    private static Direction decodeDirection(int[] rotationMatrix) {
        Vector3f vector3f = bakeVectors(rotationMatrix, 0);
        Vector3f vector3f2 = bakeVectors(rotationMatrix, 8);
        Vector3f vector3f3 = bakeVectors(rotationMatrix, 16);
        Vector3f vector3f4 = (new Vector3f(vector3f)).sub(vector3f2);
        Vector3f vector3f5 = (new Vector3f(vector3f3)).sub(vector3f2);
        Vector3f vector3f6 = (new Vector3f(vector3f5)).cross(vector3f4).normalize();
        if (!vector3f6.isFinite()) {
            return Direction.UP;
        } else {
            Direction direction = null;
            float f = 0.0F;

            for(Direction direction2 : Direction.values()) {
                float g = vector3f6.dot(direction2.getFloatVector());
                if (g >= 0.0F && g > f) {
                    f = g;
                    direction = direction2;
                }
            }

            return Objects.requireNonNullElse(direction, Direction.UP);
        }
    }

    private static float bakeVectorX(int[] is, int i) {
        return Float.intBitsToFloat(is[i]);
    }

    private static float bakeVectorY(int[] is, int i) {
        return Float.intBitsToFloat(is[i + 1]);
    }

    private static float bakeVectorZ(int[] is, int i) {
        return Float.intBitsToFloat(is[i + 2]);
    }

    private static Vector3f bakeVectors(int[] is, int i) {
        return new Vector3f(bakeVectorX(is, i), bakeVectorY(is, i), bakeVectorZ(is, i));
    }

    private static ModelElementFace.UV compactUV(Sprite sprite, ModelElementFace.UV uv) {
        float f = uv.minU();
        float g = uv.minV();
        float h = uv.maxU();
        float i = uv.maxV();
        float j = sprite.getUvScaleDelta();
        float k = (f + f + h + h) / 4.0F;
        float l = (g + g + i + i) / 4.0F;
        return new ModelElementFace.UV(MathHelper.lerp(j, f, k), MathHelper.lerp(j, g, l), MathHelper.lerp(j, h, k), MathHelper.lerp(j, i, l));
    }

    private static ModelElementFace.UV setDefaultUV(Vector3fc from, Vector3fc to, Direction facing) {
        ModelElementFace.UV var10000;
        switch (facing) {
            case DOWN -> var10000 = new ModelElementFace.UV(from.x(), 16.0F - to.z(), to.x(), 16.0F - from.z());
            case UP -> var10000 = new ModelElementFace.UV(from.x(), from.z(), to.x(), to.z());
            case NORTH -> var10000 = new ModelElementFace.UV(16.0F - to.x(), 16.0F - to.y(), 16.0F - from.x(), 16.0F - from.y());
            case SOUTH -> var10000 = new ModelElementFace.UV(from.x(), 16.0F - to.y(), to.x(), 16.0F - from.y());
            case WEST -> var10000 = new ModelElementFace.UV(from.z(), 16.0F - to.y(), to.z(), 16.0F - from.y());
            case EAST -> var10000 = new ModelElementFace.UV(16.0F - to.z(), 16.0F - to.y(), 16.0F - from.z(), 16.0F - from.y());
            default -> throw new MatchException(null, null);
        }

        return var10000;
    }

    private static void rotateVertex(Vector3f vertex, Position3V pivot, Position3V rotation) {
        Quaternionf quaternionf = new Quaternionf().rotationXYZ(rotation.getX() * DEGREES_TO_RADIANS, rotation.getY() * DEGREES_TO_RADIANS, rotation.getZ() * DEGREES_TO_RADIANS);
        transformVertex(vertex, new Vector3f(pivot.getX(), pivot.getY(), pivot.getZ()), (new Matrix4f()).rotation(quaternionf));
    }

    private static void transformVertex(Vector3f vertex, Vector3fc vector3fc, Matrix4fc matrix4fc) {
        Vector4f vector4f = matrix4fc.transform(new Vector4f(vertex.x() - vector3fc.x(), vertex.y() - vector3fc.y(), vertex.z() - vector3fc.z(), 1.0f));
        vertex.set(vector4f.x() + vector3fc.x(), vector4f.y() + vector3fc.y(), vector4f.z() + vector3fc.z());
    }

    private record RotationCache(Position3V rotation, Position3V pivot) {
    }
}
