package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xcompwiz.mystcraft.entity.monster.DimensionalLaser;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;

/** Solid luminous laser tube and animated concentric summoning sigil, with native synchronization. */
public final class DimensionalSpellRenderer<T extends Entity> extends EntityRenderer<T, DimensionalSpellRenderer.State> {
    public static final class State extends EntityRenderState { Vec3 end = Vec3.ZERO; float time; }
    private final boolean laser;
    public DimensionalSpellRenderer(EntityRendererProvider.Context context, boolean laser) { super(context); this.laser = laser; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(T entity, State state, float partial) {
        super.extractRenderState(entity, state, partial);
        state.end = entity instanceof DimensionalLaser beam ? beam.endOffset() : Vec3.ZERO;
        state.time = entity.tickCount + partial;
    }
    @Override protected AABB getBoundingBoxForCulling(T entity, float partial) {
        return entity instanceof DimensionalLaser beam ? new AABB(entity.position(), entity.position().add(beam.endOffset())).inflate(.3)
                : super.getBoundingBoxForCulling(entity, partial).inflate(1.5);
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        Vec3 end = state.end; float time = state.time;
        collector.submitCustomGeometry(poses, RenderTypes.lightning(), (pose, out) -> {
            if (laser) {
                var forward = end.normalize(); var axis = forward.cross(new Vec3(0, 1, 0));
                if (axis.lengthSqr() < .001) axis = forward.cross(new Vec3(1, 0, 0));
                axis = axis.normalize(); var other = forward.cross(axis).normalize();
                for (int layer = 0; layer < 2; layer++) for (int i = 0; i < 8; i++) {
                    double r = layer == 0 ? .05 : .13;
                    var a = axis.scale(Math.cos(i * Math.PI / 4) * r).add(other.scale(Math.sin(i * Math.PI / 4) * r));
                    var b = axis.scale(Math.cos((i + 1) * Math.PI / 4) * r).add(other.scale(Math.sin((i + 1) * Math.PI / 4) * r));
                    quad(out, pose, a, b, b.add(end), a.add(end), layer == 0 ? 235 : 155, layer == 0 ? 200 : 40, 255, layer == 0 ? 230 : 100);
                }
            } else {
                for (double radius : new double[]{.72, 1.1}) for (int i = 0; i < 64; i++) {
                    double a = i * Math.PI / 32 + time * .025, b = (i + 1) * Math.PI / 32 + time * .025;
                    quad(out, pose, point(a, radius), point(b, radius), point(b, radius + .045), point(a, radius + .045), 190, 65, 255, 210);
                }
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3 - time * .02;
                    Vec3 a = point(angle, 1.05), b = point(angle + Math.PI * 2 / 3, 1.05);
                    Vec3 width = b.subtract(a).normalize().cross(new Vec3(0, 1, 0)).scale(.025);
                    quad(out, pose, a.add(width), b.add(width), b.subtract(width), a.subtract(width), 220, 130, 255, 230);
                }
            }
        });
        super.submit(state, poses, collector, camera);
    }
    private static Vec3 point(double angle, double radius) { return new Vec3(Math.cos(angle) * radius, .02, Math.sin(angle) * radius); }
    private static void quad(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int r, int g, int blue, int alpha) {
        for (Vec3 v : new Vec3[]{a, b, c, d}) out.addVertex(pose, (float)v.x, (float)v.y, (float)v.z).setColor(r, g, blue, alpha);
    }
}
