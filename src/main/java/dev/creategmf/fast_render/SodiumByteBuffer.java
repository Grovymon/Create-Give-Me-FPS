package dev.creategmf.fast_render;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.creategmf.mixin.fast_render.BufferBuilderAccessor;
import dev.engine_room.flywheel.lib.util.ShadersModHelper;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.TemplateMesh;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.mixin.client.accessor.RenderSystemAccessor;

import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.vertices.NormalHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.Math;

@SuppressWarnings("unchecked")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SodiumByteBuffer implements SuperByteBuffer {
    private static final Long2IntMap WORLD_LIGHT_CACHE = new Long2IntOpenHashMap();

    private final TemplateMesh template;
    private final int[] shadeSwapVertices;

    // Vertex Position and Normals
    private final PoseStack transforms = new PoseStack();
    private final boolean invertFakeDiffuseNormal;

    // Vertex Coloring
    private int vertexColor; // aabbggrr
    private boolean disableDiffuse;

    // Vertex Texture Coords
    @Nullable
    private SpriteShiftFunc spriteShiftFunc;

    // Vertex Overlay
    private boolean hasCustomOverlay;
    private int overlay;

    // Vertex Light
    private boolean hasCustomLight;
    private int packedLight;
    private boolean useLevelLight;
    @Nullable
    private BlockAndTintGetter levelWithLight;
    @Nullable
    private Matrix4f lightTransform;

    // Reused objects
    private static final Matrix4f modelMat = new Matrix4f();
    private final Matrix3f normalMat = new Matrix3f();
    private final ShiftOutput shiftOutput = new ShiftOutput();
    private static final Vector3f lightDir0 = new Vector3f();
    private static final Vector3f lightDir1 = new Vector3f();
    private static final Vector3f float3 = new Vector3f();
    private static final Vector3f pos0 = new Vector3f();
    private static final Vector3f pos1 = new Vector3f();
    private static final Vector3f pos2 = new Vector3f();
    private static final Vector3f pos3 = new Vector3f();
    private static final Vector2f uv0 = new Vector2f();
    private static final Vector2f uv1 = new Vector2f();
    private static final Vector2f uv2 = new Vector2f();
    private static final Vector2f uv3 = new Vector2f();

    // Vertex Buffer
    private static final int BUFFER_VERTEX_COUNT = 48;
    private static final MemoryStack STACK = MemoryStack.create();
    private static final int BUFFER_SIZE = BUFFER_VERTEX_COUNT * IrisEntityVertex.STRIDE;
    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, BUFFER_SIZE);
    private static long BUFFER_PTR = SCRATCH_BUFFER;
    private static int BUFFED_VERTEX = 0;

    public SodiumByteBuffer(TemplateMesh template, int[] shadeSwapVertices, boolean invertFakeDiffuseNormal) {
        this.template = template;
        this.shadeSwapVertices = shadeSwapVertices;
        this.invertFakeDiffuseNormal = invertFakeDiffuseNormal;
        reset();
    }

    public SodiumByteBuffer(TemplateMesh template, int[] shadeSwapVertices) {
        this(template, shadeSwapVertices, false);
    }

    public SodiumByteBuffer(TemplateMesh template) {
        this(template, new int[0]);
    }

    public SuperByteBuffer reset() {
        while (!transforms.clear())
            transforms.popPose();
        transforms.pushPose();

        vertexColor = 0xffffffff;
        disableDiffuse = false;
        spriteShiftFunc = null;
        hasCustomOverlay = false;
        overlay = OverlayTexture.NO_OVERLAY;
        hasCustomLight = false;
        packedLight = 0;
        useLevelLight = false;
        levelWithLight = null;
        lightTransform = null;
        return this;
    }

    public boolean isEmpty() {
        return template.isEmpty();
    }

    public PoseStack getTransforms() {
        return transforms;
    }

    @Override
    public SuperByteBuffer scale(float factorX, float factorY, float factorZ) {
        transforms.scale(factorX, factorY, factorZ);
        return this;
    }

    @Override
    public SuperByteBuffer rotate(Quaternionfc quaternion) {
        var last = transforms.last();
        last.pose().rotate(quaternion);
        last.normal().rotate(quaternion);
        return this;
    }

    @Override
    public SuperByteBuffer translate(float x, float y, float z) {
        transforms.translate(x, y, z);
        return this;
    }

    @Override
    public SuperByteBuffer mulPose(Matrix4fc pose) {
        transforms.last()
                .pose()
                .mul(pose);
        return this;
    }

    @Override
    public SuperByteBuffer mulNormal(Matrix3fc normal) {
        transforms.last()
                .normal()
                .mul(normal);
        return this;
    }

    @Override
    public SuperByteBuffer pushPose() {
        transforms.pushPose();
        return this;
    }

    @Override
    public SuperByteBuffer popPose() {
        transforms.popPose();
        return this;
    }

    public SuperByteBuffer color(float r, float g, float b, float a) {
        color((int) (r * 255.0f), (int) (g * 255.0f), (int) (b * 255.0f), (int) (a * 255.0f));
        return this;
    }

    public SuperByteBuffer color(int r, int g, int b, int a) {
        this.vertexColor = (a & 0xff) << 24 | (b & 0xff) << 16 | (g & 0xff) << 8 | (r & 0xff);
        return this;
    }

    public SuperByteBuffer color(int color) {
        this.vertexColor = 0xff000000 | ((color & 0xFF) << 16) | ((color & 0xFF00)) | ((color & 0xFF0000) >>> 16);
        return this;
    }

    public SuperByteBuffer color(Color c) {
        return color(c.getRGB());
    }

    public SuperByteBuffer disableDiffuse() {
        disableDiffuse = true;
        return this;
    }

    public SuperByteBuffer shiftUV(SpriteShiftEntry entry) {
        spriteShiftFunc = (u, v, output) -> {
            output.accept(entry.getTargetU(u), entry.getTargetV(v));
        };
        return this;
    }

    public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollV) {
        return shiftUVScrolling(entry, 0, scrollV);
    }

    public SuperByteBuffer shiftUVScrolling(SpriteShiftEntry entry, float scrollU, float scrollV) {
        spriteShiftFunc = (u, v, output) -> {
            float targetU = u - entry.getOriginal()
                    .getU0() + entry.getTarget()
                    .getU0()
                    + scrollU;
            float targetV = v - entry.getOriginal()
                    .getV0() + entry.getTarget()
                    .getV0()
                    + scrollV;
            output.accept(targetU, targetV);
        };
        return this;
    }

    public SuperByteBuffer shiftUVtoSheet(SpriteShiftEntry entry, float uTarget, float vTarget, int sheetSize) {
        spriteShiftFunc = (u, v, output) -> {
            float targetU = entry.getTarget()
                    .getU((SpriteShiftEntry.getUnInterpolatedU(entry.getOriginal(), u) / sheetSize) + uTarget);
            float targetV = entry.getTarget()
                    .getV((SpriteShiftEntry.getUnInterpolatedV(entry.getOriginal(), v) / sheetSize) + vTarget);
            output.accept(targetU, targetV);
        };
        return this;
    }

    public SuperByteBuffer overlay(int overlay) {
        hasCustomOverlay = true;
        this.overlay = overlay;
        return this;
    }

    public SuperByteBuffer light(int packedLight) {
        hasCustomLight = true;
        this.packedLight = packedLight;
        return this;
    }

    @Override
    public SuperByteBuffer useLevelLight(BlockAndTintGetter level) {
        useLevelLight = true;
        levelWithLight = level;
        return this;
    }

    @Override
    public SuperByteBuffer useLevelLight(BlockAndTintGetter level, Matrix4f lightTransform) {
        useLevelLight = true;
        levelWithLight = level;
        this.lightTransform = lightTransform;
        return this;
    }

    // Adapted from minecraft:shaders/include/light.glsl
    private static float calculateDiffuse(Vector3fc normal, Vector3fc lightDir0, Vector3fc lightDir1) {
        float light0 = Math.max(0.0f, lightDir0.dot(normal));
        float light1 = Math.max(0.0f, lightDir1.dot(normal));
        return Math.min(1.0f, (light0 + light1) * 0.6f + 0.4f);
    }

    public static int getLight(BlockAndTintGetter world, Vector3f lightPos) {
        BlockPos pos = BlockPos.containing(lightPos.x(), lightPos.y(), lightPos.z());
        return WORLD_LIGHT_CACHE.computeIfAbsent(pos.asLong(), $ -> LevelRenderer.getLightColor(world, pos));
    }

    public int getLight(Vector3f lightPos) {
        return getLight(levelWithLight, lightTransform == null ? lightPos:lightPos.mulPosition(lightTransform));
    }

    private static boolean isBufferMax() {
        return BUFFED_VERTEX >= BUFFER_VERTEX_COUNT;
    }

    private static void flush(VertexBufferWriter writer, boolean force, VertexFormat format) {
        if (!force && !isBufferMax()) {
            return;
        }
        if (BUFFED_VERTEX == 0) return;
        STACK.push();
        writer.push(STACK, SCRATCH_BUFFER, BUFFED_VERTEX, format);
        STACK.pop();
        BUFFER_PTR = SCRATCH_BUFFER;
        BUFFED_VERTEX = 0;
    }

    private static boolean isPerspectiveProjection() {
        return RenderSystem.getModelViewMatrix().m32() == 0;
    }

    private static int calcColorSodium(int quadColor, int vertexColor, int unshadedDiffuse, boolean applyDiffuse, boolean shaded, float nx, float ny, float nz) {
        int r = ((((quadColor) & 0xFF) * ((vertexColor) & 0xFF)) + 0xFF) >>> 8;
        int g = ((((quadColor >>> 8) & 0xFF) * ((vertexColor >>> 8) & 0xFF)) + 0xFF) >>> 8;
        int b = ((((quadColor >>> 16) & 0xFF) * ((vertexColor >>> 16) & 0xFF)) + 0xFF) >>> 8;
        int a = ((((quadColor >>> 24) & 0xFF) * ((vertexColor >>> 24) & 0xFF)) + 0xFF) >>> 8;
        if (applyDiffuse) {
            float3.set(nx, ny, nz);
            int factor = shaded ? (int) (255.0F * calculateDiffuse(float3, lightDir0, lightDir1)):unshadedDiffuse;
            r = (r * factor + 255) >>> 8;
            g = (g * factor + 255) >>> 8;
            b = (b * factor + 255) >>> 8;
        }
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int calcColorIris(int quadColor, int vertexColor) {
        int r = ((((quadColor) & 0xFF) * ((vertexColor) & 0xFF)) + 0xFF) >>> 8;
        int g = ((((quadColor >>> 8) & 0xFF) * ((vertexColor >>> 8) & 0xFF)) + 0xFF) >>> 8;
        int b = ((((quadColor >>> 16) & 0xFF) * ((vertexColor >>> 16) & 0xFF)) + 0xFF) >>> 8;
        int a = ((((quadColor >>> 24) & 0xFF) * ((vertexColor >>> 24) & 0xFF)) + 0xFF) >>> 8;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private void IrisRenderShadowInto(PoseStack input, VertexBufferWriter writer, VertexFormat format) {
        modelMat.set(input.last().pose());
        Matrix4f localTransforms = transforms.last().pose();
        modelMat.mul(localTransforms);

        normalMat.set(transforms.last().normal());
        Matrix4f sunMat = IrisCompat.getShadowMV();

        boolean isTerrain = (format == IrisTerrainVertex.FORMAT);

        int vertexCount = template.vertexCount();
        for (int i = 0; i < vertexCount; i += 4) {
            int packedNormal = template.normal(i);
            float unpackedX = NormI8.unpackX(packedNormal);
            float unpackedY = NormI8.unpackY(packedNormal);
            float unpackedZ = NormI8.unpackZ(packedNormal);
            float nx = MatrixHelper.transformNormalX(normalMat, unpackedX, unpackedY, unpackedZ);
            float ny = MatrixHelper.transformNormalY(normalMat, unpackedX, unpackedY, unpackedZ);
            float nz = MatrixHelper.transformNormalZ(normalMat, unpackedX, unpackedY, unpackedZ);

            if (nx * sunMat.m02() + ny * sunMat.m12() + nz * sunMat.m22() >= 0) continue; // backface culling
            pos0.set(template.x(i), template.y(i), template.z(i)).mulPosition(modelMat);
            pos1.set(template.x(i + 1), template.y(i + 1), template.z(i + 1)).mulPosition(modelMat);
            pos2.set(template.x(i + 2), template.y(i + 2), template.z(i + 2)).mulPosition(modelMat);
            pos3.set(template.x(i + 3), template.y(i + 3), template.z(i + 3)).mulPosition(modelMat);

            int normal = NormI8.pack(nx, ny, nz);

            if (spriteShiftFunc != null) {
                spriteShiftFunc.shift(template.u(i), template.v(i), shiftOutput);
                uv0.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 1), template.v(i + 1), shiftOutput);
                uv1.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 2), template.v(i + 2), shiftOutput);
                uv2.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 3), template.v(i + 3), shiftOutput);
                uv3.set(shiftOutput.u, shiftOutput.v);
            } else {
                uv0.set(template.u(i), template.v(i));
                uv1.set(template.u(i + 1), template.v(i + 1));
                uv2.set(template.u(i + 2), template.v(i + 2));
                uv3.set(template.u(i + 3), template.v(i + 3));
            }

            if (isTerrain) { // IrisTerrainVertex.FORMAT
                IrisTerrainVertex.write(BUFFER_PTR, pos0.x, pos0.y, pos0.z, 0xffffffff, uv0.x, uv0.y, 0.5f, 0.5f, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;

                IrisTerrainVertex.write(BUFFER_PTR, pos1.x, pos1.y, pos1.z, 0xffffffff, uv1.x, uv1.y, 0.5f, 0.5f, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;

                IrisTerrainVertex.write(BUFFER_PTR, pos2.x, pos2.y, pos2.z, 0xffffffff, uv2.x, uv2.y, 0.5f, 0.5f, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;

                IrisTerrainVertex.write(BUFFER_PTR, pos3.x, pos3.y, pos3.z, 0xffffffff, uv3.x, uv3.y, 0.5f, 0.5f, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;
            } else { // IrisEntityVertex.FORMAT
                IrisEntityVertex.write(BUFFER_PTR, pos0.x, pos0.y, pos0.z, 0xffffffff, uv0.x, uv0.y, 0.5f, 0.5f, 0xffffffff, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisEntityVertex.STRIDE;

                IrisEntityVertex.write(BUFFER_PTR, pos1.x, pos1.y, pos1.z, 0xffffffff, uv1.x, uv1.y, 0.5f, 0.5f, 0xffffffff, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisEntityVertex.STRIDE;

                IrisEntityVertex.write(BUFFER_PTR, pos2.x, pos2.y, pos2.z, 0xffffffff, uv2.x, uv2.y, 0.5f, 0.5f, 0xffffffff, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisEntityVertex.STRIDE;

                IrisEntityVertex.write(BUFFER_PTR, pos3.x, pos3.y, pos3.z, 0xffffffff, uv3.x, uv3.y, 0.5f, 0.5f, 0xffffffff, 0xf000f0, normal, 0xffffffff);
                BUFFER_PTR += IrisEntityVertex.STRIDE;
            }
            BUFFED_VERTEX += 4;
            flush(writer, false, format);
        }

        flush(writer, true, format);
    }

    private void IrisRenderInto(PoseStack input, VertexBufferWriter writer, VertexFormat format) {
        modelMat.set(input.last().pose());
        Matrix4f localTransforms = transforms.last().pose();
        modelMat.mul(localTransforms);

        normalMat.set(input.last().normal());
        Matrix3f localNormalTransforms = transforms.last().normal();
        normalMat.mul(localNormalTransforms);

        boolean isTerrain = (format == IrisTerrainVertex.FORMAT);
        boolean isPerspectiveProjection = isPerspectiveProjection();

        int vertexCount = template.vertexCount();
        for (int i = 0; i < vertexCount; i += 4) {
            int packedNormal = template.normal(i);
            float unpackedX = NormI8.unpackX(packedNormal);
            float unpackedY = NormI8.unpackY(packedNormal);
            float unpackedZ = NormI8.unpackZ(packedNormal);
            float nx = MatrixHelper.transformNormalX(normalMat, unpackedX, unpackedY, unpackedZ);
            float ny = MatrixHelper.transformNormalY(normalMat, unpackedX, unpackedY, unpackedZ);
            float nz = MatrixHelper.transformNormalZ(normalMat, unpackedX, unpackedY, unpackedZ);

            pos0.set(template.x(i), template.y(i), template.z(i)).mulPosition(modelMat);
            pos1.set(template.x(i + 1), template.y(i + 1), template.z(i + 1)).mulPosition(modelMat);
            pos2.set(template.x(i + 2), template.y(i + 2), template.z(i + 2)).mulPosition(modelMat);

            if (isPerspectiveProjection) { // do backface culling
                float ex = pos1.x - pos0.x, ey = pos1.y - pos0.y, ez = pos1.z - pos0.z;
                float fx = pos2.x - pos0.x, fy = pos2.y - pos0.y, fz = pos2.z - pos0.z;
                float cx = ey * fz - ez * fy;
                float cy = ez * fx - ex * fz;
                float cz = ex * fy - ey * fx;
                if (nx * cx + ny * cy + nz * cz < 0) {
                    cx = -cx; cy = -cy; cz = -cz;
                }
                if (cx * (pos0.x + pos2.x) + cy * (pos0.y + pos2.y) + cz * (pos0.z + pos2.z) > 0) continue;
            }

            pos3.set(template.x(i + 3), template.y(i + 3), template.z(i + 3)).mulPosition(modelMat);

            int normal = NormI8.pack(nx, ny, nz);
            int tangent = NormalHelper.computeTangent(nx, ny, nz, pos0.x, pos0.y, pos0.z, uv0.x, uv0.y, pos1.x, pos1.y, pos1.z, uv1.x, uv1.y, pos2.x, pos2.y, pos2.z, uv2.x, uv2.y);

            if (spriteShiftFunc != null) {
                spriteShiftFunc.shift(template.u(i), template.v(i), shiftOutput);
                uv0.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 1), template.v(i + 1), shiftOutput);
                uv1.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 2), template.v(i + 2), shiftOutput);
                uv2.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 3), template.v(i + 3), shiftOutput);
                uv3.set(shiftOutput.u, shiftOutput.v);
            } else {
                uv0.set(template.u(i), template.v(i));
                uv1.set(template.u(i + 1), template.v(i + 1));
                uv2.set(template.u(i + 2), template.v(i + 2));
                uv3.set(template.u(i + 3), template.v(i + 3));
            }

            float mid_u = (uv0.x + uv1.x + uv2.x + uv3.x) / 4;
            float mid_v = (uv0.y + uv1.y + uv2.y + uv3.y) / 4;

            int color0 = calcColorIris(template.color(i), vertexColor);
            int color1 = calcColorIris(template.color(i + 1), vertexColor);
            int color2 = calcColorIris(template.color(i + 2), vertexColor);
            int color3 = calcColorIris(template.color(i + 3), vertexColor);

            int light0 = template.light(i);
            int light1 = template.light(i + 1);
            int light2 = template.light(i + 2);
            int light3 = template.light(i + 3);
            if (hasCustomLight) {
                light0 = SuperByteBuffer.maxLight(light0, packedLight);
                light1 = SuperByteBuffer.maxLight(light1, packedLight);
                light2 = SuperByteBuffer.maxLight(light2, packedLight);
                light3 = SuperByteBuffer.maxLight(light3, packedLight);
            }

            if (useLevelLight) {
                float3.set(((template.x(i) - .5f) * 15 / 16f) + .5f, (template.y(i) - .5f) * 15 / 16f + .5f, (template.z(i) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light0 = SuperByteBuffer.maxLight(light0, getLight(float3));
                float3.set(((template.x(i + 1) - .5f) * 15 / 16f) + .5f, (template.y(i + 1) - .5f) * 15 / 16f + .5f, (template.z(i + 1) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light1 = SuperByteBuffer.maxLight(light1, getLight(float3));
                float3.set(((template.x(i + 2) - .5f) * 15 / 16f) + .5f, (template.y(i + 2) - .5f) * 15 / 16f + .5f, (template.z(i + 2) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light2 = SuperByteBuffer.maxLight(light2, getLight(float3));
                float3.set(((template.x(i + 3) - .5f) * 15 / 16f) + .5f, (template.y(i + 3) - .5f) * 15 / 16f + .5f, (template.z(i + 3) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light3 = SuperByteBuffer.maxLight(light3, getLight(float3));
            }

            if (isTerrain) { // IrisTerrainVertex.FORMAT
                IrisTerrainVertex.write(BUFFER_PTR, pos0.x, pos0.y, pos0.z, color0, uv0.x, uv0.y, mid_u, mid_v, light0, normal, tangent);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;

                IrisTerrainVertex.write(BUFFER_PTR, pos1.x, pos1.y, pos1.z, color1, uv1.x, uv1.y, mid_u, mid_v, light1, normal, tangent);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;

                IrisTerrainVertex.write(BUFFER_PTR, pos2.x, pos2.y, pos2.z, color2, uv2.x, uv2.y, mid_u, mid_v, light2, normal, tangent);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;

                IrisTerrainVertex.write(BUFFER_PTR, pos3.x, pos3.y, pos3.z, color3, uv3.x, uv3.y, mid_u, mid_v, light3, normal, tangent);
                BUFFER_PTR += IrisTerrainVertex.STRIDE;
            } else { // IrisEntityVertex.FORMAT
                int overlay0, overlay1, overlay2, overlay3;
                if (hasCustomOverlay) {
                    overlay0 = overlay1 = overlay2 = overlay3 = overlay;
                } else {
                    overlay0 = template.overlay(i);
                    overlay1 = template.overlay(i + 1);
                    overlay2 = template.overlay(i + 2);
                    overlay3 = template.overlay(i + 3);
                }
                IrisEntityVertex.write(BUFFER_PTR, pos0.x, pos0.y, pos0.z, color0, uv0.x, uv0.y, mid_u, mid_v, overlay0, light0, normal, tangent);
                BUFFER_PTR += IrisEntityVertex.STRIDE;

                IrisEntityVertex.write(BUFFER_PTR, pos1.x, pos1.y, pos1.z, color1, uv1.x, uv1.y, mid_u, mid_v, overlay1, light1, normal, tangent);
                BUFFER_PTR += IrisEntityVertex.STRIDE;

                IrisEntityVertex.write(BUFFER_PTR, pos2.x, pos2.y, pos2.z, color2, uv2.x, uv2.y, mid_u, mid_v, overlay2, light2, normal, tangent);
                BUFFER_PTR += IrisEntityVertex.STRIDE;

                IrisEntityVertex.write(BUFFER_PTR, pos3.x, pos3.y, pos3.z, color3, uv3.x, uv3.y, mid_u, mid_v, overlay3, light3, normal, tangent);
                BUFFER_PTR += IrisEntityVertex.STRIDE;
            }
            BUFFED_VERTEX += 4;
            flush(writer, false, format);
        }

        flush(writer, true, format);
    }

    private void SodiumRenderInto(PoseStack input, VertexBufferWriter writer, VertexFormat format) {
        modelMat.set(input.last().pose());
        Matrix4f localTransforms = transforms.last().pose();
        modelMat.mul(localTransforms);

        normalMat.set(input.last().normal());
        Matrix3f localNormalTransforms = transforms.last().normal();
        normalMat.mul(localNormalTransforms);

        boolean shaded = true;
        int shadeSwapIndex = 0;
        int nextShadeSwapVertex = shadeSwapIndex < shadeSwapVertices.length ? shadeSwapVertices[shadeSwapIndex]:Integer.MAX_VALUE;
        int unshadedDiffuse = 255;
        boolean applyDiffuse = !disableDiffuse;
        if (applyDiffuse) {
            lightDir0.set(RenderSystemAccessor.catnip$getShaderLightDirections()[0]).normalize();
            lightDir1.set(RenderSystemAccessor.catnip$getShaderLightDirections()[1]).normalize();
            if (shadeSwapVertices.length > 0) {
                // Pretend unshaded faces always point up to get the correct max diffuse value for the current level.
                float3.set(0, invertFakeDiffuseNormal ? -1:1, 0);
                // Don't apply the normal matrix since that would cause upside down objects to be dark.
                unshadedDiffuse = (int) (255 * calculateDiffuse(float3, lightDir0, lightDir1));
            }
        }

        boolean isPerspectiveProjection = isPerspectiveProjection();

        int vertexCount = template.vertexCount();
        for (int i = 0; i < vertexCount; i += 4) {
            if (i >= nextShadeSwapVertex) {
                shaded = !shaded;
                shadeSwapIndex++;
                nextShadeSwapVertex = shadeSwapIndex < shadeSwapVertices.length ? shadeSwapVertices[shadeSwapIndex]:Integer.MAX_VALUE;
            }

            int packedNormal = template.normal(i);
            float unpackedX = NormI8.unpackX(packedNormal);
            float unpackedY = NormI8.unpackY(packedNormal);
            float unpackedZ = NormI8.unpackZ(packedNormal);
            float nx = MatrixHelper.transformNormalX(normalMat, unpackedX, unpackedY, unpackedZ);
            float ny = MatrixHelper.transformNormalY(normalMat, unpackedX, unpackedY, unpackedZ);
            float nz = MatrixHelper.transformNormalZ(normalMat, unpackedX, unpackedY, unpackedZ);

            pos0.set(template.x(i), template.y(i), template.z(i)).mulPosition(modelMat);
            pos1.set(template.x(i + 1), template.y(i + 1), template.z(i + 1)).mulPosition(modelMat);
            pos2.set(template.x(i + 2), template.y(i + 2), template.z(i + 2)).mulPosition(modelMat);

            if (isPerspectiveProjection) { // do backface culling
                float ex = pos1.x - pos0.x, ey = pos1.y - pos0.y, ez = pos1.z - pos0.z;
                float fx = pos2.x - pos0.x, fy = pos2.y - pos0.y, fz = pos2.z - pos0.z;
                float cx = ey * fz - ez * fy;
                float cy = ez * fx - ex * fz;
                float cz = ex * fy - ey * fx;
                if (nx * cx + ny * cy + nz * cz < 0) {
                    cx = -cx; cy = -cy; cz = -cz;
                }
                if (cx * (pos0.x + pos2.x) + cy * (pos0.y + pos2.y) + cz * (pos0.z + pos2.z) > 0) continue;
            }

            int normal = NormI8.pack(nx, ny, nz);
            pos3.set(template.x(i + 3), template.y(i + 3), template.z(i + 3)).mulPosition(modelMat);

            if (spriteShiftFunc != null) {
                spriteShiftFunc.shift(template.u(i), template.v(i), shiftOutput);
                uv0.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 1), template.v(i + 1), shiftOutput);
                uv1.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 2), template.v(i + 2), shiftOutput);
                uv2.set(shiftOutput.u, shiftOutput.v);

                spriteShiftFunc.shift(template.u(i + 3), template.v(i + 3), shiftOutput);
                uv3.set(shiftOutput.u, shiftOutput.v);
            } else {
                uv0.set(template.u(i), template.v(i));
                uv1.set(template.u(i + 1), template.v(i + 1));
                uv2.set(template.u(i + 2), template.v(i + 2));
                uv3.set(template.u(i + 3), template.v(i + 3));
            }

            int color0 = calcColorSodium(template.color(i), vertexColor, unshadedDiffuse, applyDiffuse, shaded, nx, ny, nz);
            int color1 = calcColorSodium(template.color(i + 1), vertexColor, unshadedDiffuse, applyDiffuse, shaded, nx, ny, nz);
            int color2 = calcColorSodium(template.color(i + 2), vertexColor, unshadedDiffuse, applyDiffuse, shaded, nx, ny, nz);
            int color3 = calcColorSodium(template.color(i + 3), vertexColor, unshadedDiffuse, applyDiffuse, shaded, nx, ny, nz);

            int light0 = template.light(i);
            int light1 = template.light(i + 1);
            int light2 = template.light(i + 2);
            int light3 = template.light(i + 3);
            if (hasCustomLight) {
                light0 = SuperByteBuffer.maxLight(light0, packedLight);
                light1 = SuperByteBuffer.maxLight(light1, packedLight);
                light2 = SuperByteBuffer.maxLight(light2, packedLight);
                light3 = SuperByteBuffer.maxLight(light3, packedLight);
            }

            if (useLevelLight) {
                float3.set(((template.x(i) - .5f) * 15 / 16f) + .5f, (template.y(i) - .5f) * 15 / 16f + .5f, (template.z(i) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light0 = SuperByteBuffer.maxLight(light0, getLight(float3));
                float3.set(((template.x(i + 1) - .5f) * 15 / 16f) + .5f, (template.y(i + 1) - .5f) * 15 / 16f + .5f, (template.z(i + 1) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light1 = SuperByteBuffer.maxLight(light1, getLight(float3));
                float3.set(((template.x(i + 2) - .5f) * 15 / 16f) + .5f, (template.y(i + 2) - .5f) * 15 / 16f + .5f, (template.z(i + 2) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light2 = SuperByteBuffer.maxLight(light2, getLight(float3));
                float3.set(((template.x(i + 3) - .5f) * 15 / 16f) + .5f, (template.y(i + 3) - .5f) * 15 / 16f + .5f, (template.z(i + 3) - .5f) * 15 / 16f + .5f).mulPosition(localTransforms);
                light3 = SuperByteBuffer.maxLight(light3, getLight(float3));
            }

            if (format == BlockVertex.FORMAT) { // BlockVertex.FORMAT
                BlockVertex.write(BUFFER_PTR, pos0.x, pos0.y, pos0.z, color0, uv0.x, uv0.y, light0, normal);
                BUFFER_PTR += BlockVertex.STRIDE;

                BlockVertex.write(BUFFER_PTR, pos1.x, pos1.y, pos1.z, color1, uv1.x, uv1.y, light1, normal);
                BUFFER_PTR += BlockVertex.STRIDE;

                BlockVertex.write(BUFFER_PTR, pos2.x, pos2.y, pos2.z, color2, uv2.x, uv2.y, light2, normal);
                BUFFER_PTR += BlockVertex.STRIDE;

                BlockVertex.write(BUFFER_PTR, pos3.x, pos3.y, pos3.z, color3, uv3.x, uv3.y, light3, normal);
                BUFFER_PTR += BlockVertex.STRIDE;
            } else { // EntityVertex.FORMAT
                int overlay0, overlay1, overlay2, overlay3;
                if (hasCustomOverlay) {
                    overlay0 = overlay1 = overlay2 = overlay3 = overlay;
                } else {
                    overlay0 = template.overlay(i);
                    overlay1 = template.overlay(i + 1);
                    overlay2 = template.overlay(i + 2);
                    overlay3 = template.overlay(i + 3);
                }
                EntityVertex.write(BUFFER_PTR, pos0.x, pos0.y, pos0.z, color0, uv0.x, uv0.y, overlay0, light0, normal);
                BUFFER_PTR += EntityVertex.STRIDE;

                EntityVertex.write(BUFFER_PTR, pos1.x, pos1.y, pos1.z, color1, uv1.x, uv1.y, overlay1, light1, normal);
                BUFFER_PTR += EntityVertex.STRIDE;

                EntityVertex.write(BUFFER_PTR, pos2.x, pos2.y, pos2.z, color2, uv2.x, uv2.y, overlay2, light2, normal);
                BUFFER_PTR += EntityVertex.STRIDE;

                EntityVertex.write(BUFFER_PTR, pos3.x, pos3.y, pos3.z, color3, uv3.x, uv3.y, overlay3, light3, normal);
                BUFFER_PTR += EntityVertex.STRIDE;
            }

            BUFFED_VERTEX += 4;
            flush(writer, false, format);
        }

        flush(writer, true, format);
    }

    public void defaultRenderInto(PoseStack input, VertexConsumer builder) {
        Matrix4f modelMat = this.modelMat.set(input.last()
                .pose());
        Matrix4f localTransforms = transforms.last()
                .pose();
        modelMat.mul(localTransforms);

        Matrix3f normalMat = this.normalMat.set(input.last()
                .normal());
        Matrix3f localNormalTransforms = transforms.last()
                .normal();
        normalMat.mul(localNormalTransforms);

        ShiftOutput shiftOutput = this.shiftOutput;

        boolean applyDiffuse = !disableDiffuse && !ShadersModHelper.isShaderPackInUse();
        boolean shaded = true;
        int shadeSwapIndex = 0;
        int nextShadeSwapVertex = shadeSwapIndex < shadeSwapVertices.length ? shadeSwapVertices[shadeSwapIndex]:-1;
        int unshadedDiffuse = 255;
        if (applyDiffuse) {
            lightDir0.set(RenderSystemAccessor.catnip$getShaderLightDirections()[0]).normalize();
            lightDir1.set(RenderSystemAccessor.catnip$getShaderLightDirections()[1]).normalize();
            if (shadeSwapVertices.length > 0) {
                // Pretend unshaded faces always point up to get the correct max diffuse value for the current level.
                float3.set(0, invertFakeDiffuseNormal ? -1:1, 0);
                // Don't apply the normal matrix since that would cause upside down objects to be dark.
                unshadedDiffuse = (int) (255 * calculateDiffuse(float3, lightDir0, lightDir1));
            }
        }

        int vertexCount = template.vertexCount();
        for (int i = 0; i < vertexCount; i++) {
            if (i == nextShadeSwapVertex) {
                shaded = !shaded;
                shadeSwapIndex++;
                nextShadeSwapVertex = shadeSwapIndex < shadeSwapVertices.length ? shadeSwapVertices[shadeSwapIndex]:-1;
            }

            float x = template.x(i);
            float y = template.y(i);
            float z = template.z(i);
            pos0.set(x, y, z);
            pos0.mulPosition(modelMat);

            int light = template.light(i);
            if (hasCustomLight) {
                light = SuperByteBuffer.maxLight(light, packedLight);
            }
            if (useLevelLight) {
                float3.set(((x - .5f) * 15 / 16f) + .5f, (y - .5f) * 15 / 16f + .5f, (z - .5f) * 15 / 16f + .5f);
                light = SuperByteBuffer.maxLight(light, getLight(float3));
            }

            int packedNormal = template.normal(i);
            float normalX = ((byte) (packedNormal & 0xFF)) / 127.0f;
            float normalY = ((byte) ((packedNormal >>> 8) & 0xFF)) / 127.0f;
            float normalZ = ((byte) ((packedNormal >>> 16) & 0xFF)) / 127.0f;
            float3.set(normalX, normalY, normalZ);
            float3.mul(normalMat);

            int quadColor = template.color(i);
            int r = ((((quadColor) & 0xFF) * ((vertexColor) & 0xFF)) + 0xFF) >>> 8;
            int g = ((((quadColor >>> 8) & 0xFF) * ((vertexColor >>> 8) & 0xFF)) + 0xFF) >>> 8;
            int b = ((((quadColor >>> 16) & 0xFF) * ((vertexColor >>> 16) & 0xFF)) + 0xFF) >>> 8;
            int a = ((((quadColor >>> 24) & 0xFF) * ((vertexColor >>> 24) & 0xFF)) + 0xFF) >>> 8;
            if (applyDiffuse) {
                int factor = shaded ? (int) (255.0F * calculateDiffuse(float3, lightDir0, lightDir1)):unshadedDiffuse;
                r = (r * factor + 255) >>> 8;
                g = (g * factor + 255) >>> 8;
                b = (b * factor + 255) >>> 8;
            }
            int color = (a << 24) | (r << 16) | (g << 8) | b;

            float u = template.u(i);
            float v = template.v(i);
            if (spriteShiftFunc != null) {
                spriteShiftFunc.shift(u, v, shiftOutput);
                u = shiftOutput.u;
                v = shiftOutput.v;
            }

            int overlay;
            if (hasCustomOverlay) {
                overlay = this.overlay;
            } else {
                overlay = template.overlay(i);
            }

            builder.addVertex(pos0.x, pos0.y, pos0.z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(float3.x, float3.y, float3.z);
        }
    }

    public boolean renderIntoSodium(PoseStack input, VertexConsumer builder) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(builder);
        if (writer == null) return false;
        if (builder instanceof BufferBuilder bb) {
            VertexFormat bufferFormat = ((BufferBuilderAccessor) bb).create_gmf$getFormat();
            if (bufferFormat == IrisTerrainVertex.FORMAT || bufferFormat == IrisEntityVertex.FORMAT) {
                if (!IrisCompat.isShadowPass()) {
                    IrisRenderInto(input, writer, bufferFormat);
                } else {
                    IrisRenderShadowInto(input, writer, bufferFormat);
                }
                return true;
            } else if (bufferFormat == BlockVertex.FORMAT || bufferFormat == EntityVertex.FORMAT) {
                SodiumRenderInto(input, writer, bufferFormat);
                return true;
            }
        }

        return false;
    }

    @Override
    public void renderInto(PoseStack input, VertexConsumer builder) {
        if (!renderIntoSodium(input, builder)) {
            defaultRenderInto(input, builder);
        }
        reset();
    }
}
