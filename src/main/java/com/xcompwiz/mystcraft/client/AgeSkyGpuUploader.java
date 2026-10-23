package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.worldgen.AgeSkyGeometry;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** RenderPearl sky pipelines; immutable star meshes stay in bounded GPU storage. */
public final class AgeSkyGpuUploader {
    private static boolean additive;
    private static final RenderPipeline COLOR = pipeline("sky_color", false, false, PrimitiveTopology.QUADS);
    private static final RenderPipeline COLOR_ADD = pipeline("sky_color_add", false, true, PrimitiveTopology.QUADS);
    private static final RenderPipeline TEXTURE = pipeline("sky_texture", true, false, PrimitiveTopology.QUADS);
    private static final RenderPipeline TEXTURE_ADD = pipeline("sky_texture_add", true, true, PrimitiveTopology.QUADS);
    private static final RenderPipeline FAN = pipeline("sky_fan", false, false, PrimitiveTopology.TRIANGLE_FAN);
    private static final IdentityHashMap<Object, CachedMesh> CACHE = new IdentityHashMap<>();
    private record CachedMesh(GpuBuffer buffer, int indices) {}
    private AgeSkyGpuUploader() {}

    private static RenderPipeline pipeline(String name, boolean textured, boolean add, PrimitiveTopology topology) {
        var builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "pipeline/" + name))
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withVertexShader(textured ? "core/position_tex_color" : "core/position_color")
                .withFragmentShader(textured ? "core/position_tex_color" : "core/position_color")
                .withVertexBinding(0, textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(topology)
                .withCull(false)
                .withColorTargetState(new ColorTargetState(add ? BlendFunction.OVERLAY : BlendFunction.TRANSLUCENT));
        if (textured) builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        return builder.build();
    }

    public static void register(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        for (var pipeline : List.of(COLOR, COLOR_ADD, TEXTURE, TEXTURE_ADD, FAN)) event.registerPipeline(pipeline);
    }
    public static void additive(boolean value) { additive = value; }
    public static void clear() {
        CACHE.values().forEach(mesh -> mesh.buffer().close());
        CACHE.clear();
    }

    public static void drawColored(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, float r, float g, float b, float a) {
        drawQuads(quads, pose, null, r, g, b, a, 0, 0, 1, 1, false, quads.size() >= 100);
    }
    public static void drawIntrinsicColored(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, float a) {
        drawQuads(quads, pose, null, 1, 1, 1, a, 0, 0, 1, 1, false, false);
    }
    public static void drawTexturedQuad(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, Identifier texture, float a) {
        drawTexturedRegion(quads, pose, texture, a, 0, 0, 1, 1);
    }
    public static void drawTexturedRegion(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, Identifier texture,
                                          float a, float u0, float v0, float u1, float v1) {
        drawQuads(quads, pose, texture, 1, 1, 1, a, u0, v0, u1, v1, false, false);
    }
    public static void drawTexturedTinted(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, Identifier texture,
                                         float r, float g, float b, float a, float uvMax) {
        drawQuads(quads, pose, texture, r, g, b, a, 0, 0, uvMax, uvMax, false, false);
    }
    public static void drawEndSkyLegacy(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, Identifier texture,
                                       float r, float g, float b, float a) {
        drawQuads(quads, pose, texture, r, g, b, a, 0, 0, 16, 16, true, false);
    }

    private static void drawQuads(List<AgeSkyGeometry.Quad> quads, Matrix4f pose, Identifier texture,
                                  float r, float g, float b, float a, float u0, float v0, float u1, float v1,
                                  boolean endUv, boolean cache) {
        if (quads.isEmpty() || a <= 0) return;
        var pipeline = texture == null ? (additive ? COLOR_ADD : COLOR) : (additive ? TEXTURE_ADD : TEXTURE);
        CachedMesh cached = cache ? CACHE.get(quads) : null;
        if (cached != null) {
            drawBuffer(cached.buffer(), cached.indices(), quads.size() * 4, PrimitiveTopology.QUADS,
                    pipeline, pose, texture, new Vector4f(r, g, b, a));
            return;
        }
        VertexFormat format = texture == null ? DefaultVertexFormat.POSITION_COLOR : DefaultVertexFormat.POSITION_TEX_COLOR;
        try (var memory = ByteBufferBuilder.exactlySized(quads.size() * 4 * format.getVertexSize())) {
            var vertices = new BufferBuilder(memory, PrimitiveTopology.QUADS, format);
            for (var quad : quads) {
                put(vertices, quad.a(), texture != null, u0, v0);
                put(vertices, quad.b(), texture != null, endUv ? u0 : u1, endUv ? v1 : v0);
                put(vertices, quad.c(), texture != null, u1, v1);
                put(vertices, quad.d(), texture != null, endUv ? u1 : u0, endUv ? v0 : v1);
            }
            try (var mesh = vertices.buildOrThrow()) {
                var buffer = RenderSystem.getDevice().createBuffer(() -> "Mystcraft sky mesh", 32, mesh.vertexBuffer());
                drawBuffer(buffer, mesh.drawState().indexCount(), mesh.drawState().vertexCount(), PrimitiveTopology.QUADS,
                        pipeline, pose, texture, new Vector4f(r, g, b, a));
                if (cache) {
                    if (CACHE.size() >= 64) clear();
                    CACHE.put(quads, new CachedMesh(buffer, mesh.drawState().indexCount()));
                } else buffer.close();
            }
        }
    }

    private static void put(BufferBuilder output, AgeSkyGeometry.Vertex vertex, boolean texture, float u, float v) {
        var consumer = output.addVertex(vertex.x(), vertex.y(), vertex.z());
        if (texture) consumer.setUv(u, v);
        consumer.setColor(vertex.r(), vertex.g(), vertex.b(), vertex.a());
    }

    public static void drawSunsetFan(List<AgeSkyGeometry.FanVertex> fan, Matrix4f pose, float r, float g, float b) {
        if (fan.size() < 3) return;
        try (var memory = ByteBufferBuilder.exactlySized(fan.size() * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            var vertices = new BufferBuilder(memory, PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            for (var vertex : fan) vertices.addVertex(vertex.x(), vertex.y(), vertex.z()).setColor(1F, 1F, 1F, vertex.alpha());
            try (var mesh = vertices.buildOrThrow();
                 var buffer = RenderSystem.getDevice().createBuffer(() -> "Mystcraft sunset", 32, mesh.vertexBuffer())) {
                drawBuffer(buffer, 0, fan.size(), PrimitiveTopology.TRIANGLE_FAN, FAN, pose, null, new Vector4f(r, g, b, 1));
            }
        }
    }

    private static void drawBuffer(GpuBuffer buffer, int indices, int vertices, PrimitiveTopology topology,
                                   RenderPipeline pipeline, Matrix4f pose, Identifier texture, Vector4f tint) {
        var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        // Lazy texture uploads and index-buffer growth must finish before opening a pass.
        var image = texture == null ? null : Minecraft.getInstance().getTextureManager().getTexture(texture);
        var transform = RenderSystem.getDynamicUniforms().writeTransform(pose, tint);
        var sequential = topology == PrimitiveTopology.QUADS ? RenderSystem.getSequentialBuffer(topology) : null;
        var indexBuffer = sequential == null ? null : sequential.getBuffer(indices);
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Mystcraft sky", target.getColorTextureView(), Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transform);
            if (texture != null) {
                pass.setUniform("Sampler0", image.getTextureView(), image.getSampler());
            }
            pass.setVertexBuffer(0, buffer.slice());
            if (topology == PrimitiveTopology.QUADS) {
                pass.setIndexBuffer(indexBuffer, sequential.type());
                pass.drawIndexed(indices, 1, 0, 0, 0);
            } else pass.draw(vertices, 1, 0, 0);
        }
    }
}
