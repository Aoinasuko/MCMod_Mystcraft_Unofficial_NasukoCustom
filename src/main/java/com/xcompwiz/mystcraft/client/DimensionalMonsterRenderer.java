package com.xcompwiz.mystcraft.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.entity.monster.DimensionalPterosaur;
import com.xcompwiz.mystcraft.entity.monster.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;

/** Renders the actual Blender-exported meshes; animations use synchronized attack state. */
public final class DimensionalMonsterRenderer<T extends Entity> extends EntityRenderer<T, DimensionalMonsterRenderer.State> {
    public static final class State extends EntityRenderState {
        float yaw, pitch, time, speed;
        int phase;
        boolean hurt, dead;
    }
    private record Part(String bone, float x, float y, float z, boolean emissive, float[][] vertices) {}
    private final boolean flying;
    private final String species;
    private final Identifier texture;
    private final List<Part> parts;

    public DimensionalMonsterRenderer(EntityRendererProvider.Context context, boolean flying) {
        this(context, flying ? "dimensional_pterosaur" : "dimensional_serpent");
    }
    public DimensionalMonsterRenderer(EntityRendererProvider.Context context, String name) {
        super(context);
        species = name;
        flying = name.equals("dimensional_pterosaur") || name.equals("dimensional_fairy") || name.equals("dimensional_calamity");
        this.shadowRadius = name.equals("dimensional_calamity") ? 1F : flying ? .5F : .7F;
        texture = Identifier.fromNamespaceAndPath("mystcraft", "textures/entity/" + (name.equals("calamity_orb") ? "dimensional_calamity" : name) + ".png");
        parts = load(context, name);
    }
    private static List<Part> load(EntityRendererProvider.Context context, String name) {
        var location = Identifier.fromNamespaceAndPath("mystcraft", "models/entity/" + name + ".json");
        try (var reader = context.getResourceManager().getResourceOrThrow(location).openAsReader()) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported monster mesh format");
            var result = new ArrayList<Part>();
            for (var entry : root.getAsJsonArray("parts")) {
                var part = entry.getAsJsonObject(); var pivot = part.getAsJsonArray("pivot");
                var quads = part.getAsJsonArray("quads");
                float[][] vertices = new float[quads.size() * 4][8]; int index = 0;
                for (var quad : quads) {
                    if (quad.getAsJsonArray().size() != 4) throw new IllegalArgumentException("Mesh must contain quads");
                    for (var vertex : quad.getAsJsonArray()) {
                        for (int i = 0; i < 8; i++) vertices[index][i] = vertex.getAsJsonArray().get(i).getAsFloat();
                        index++;
                    }
                }
                result.add(new Part(part.get("bone").getAsString(), pivot.get(0).getAsFloat(),
                        pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat(), part.get("emissive").getAsBoolean(), vertices));
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Could not load Blender monster mesh " + location, error);
        }
    }
    @Override public State createRenderState() { return new State(); }
    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(T entity, float partial) {
        // Wings and the serpent's decorative tail extend beyond their combat hitboxes.
        return super.getBoundingBoxForCulling(entity, partial).inflate(flying ? 2 : 3);
    }
    @Override public void extractRenderState(T entity, State state, float partial) {
        super.extractRenderState(entity, state, partial);
        state.yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partial, entity.xRotO, entity.getXRot());
        state.time = entity.tickCount + partial;
        state.speed = (float)entity.getDeltaMovement().horizontalDistance();
        state.phase = entity instanceof DimensionalPterosaur p ? p.attackPhase()
                : entity instanceof DimensionalSerpent s && s.isPouncing() ? 1
                : entity instanceof DimensionalFairy f ? f.attackPhase()
                : entity instanceof DimensionalWorm w ? w.attackPhase()
                : entity instanceof DimensionalCalamity c ? c.attackPhase() : 0;
        state.hurt = entity instanceof LivingEntity living && living.hurtTime > 0;
        state.dead = entity instanceof LivingEntity living && living.deathTime > 0;
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        poses.pushPose();
        poses.rotate(Axis.YP.rotationDegrees(180 - state.yaw));
        if (state.dead) poses.rotate(Axis.ZP.rotationDegrees(90));
        if (species.equals("dimensional_pterosaur")) poses.rotate(Axis.XP.rotationDegrees(Mth.clamp(-state.pitch, -35, 35)));
        if (species.equals("calamity_orb")) poses.rotate(Axis.YP.rotationDegrees(state.time * 7));
        for (Part part : parts) {
            poses.pushPose();
            poses.translate(part.x, part.y, part.z);
            animate(part.bone, state, poses);
            // The collector defers execution: capture immutable geometry and light, never the reused state.
            int light = part.emissive || species.equals("calamity_orb") ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
            int green = state.hurt ? 100 : 255;
            collector.submitCustomGeometry(poses, RenderTypes.entityCutout(texture), (pose, out) -> {
                for (float[] v : part.vertices) {
                    out.addVertex(pose, v[0], v[1], v[2]).setColor(255, green, green, 255)
                            .setUv(v[3], v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(pose, v[5], v[6], v[7]);
                }
            });
            poses.popPose();
        }
        poses.popPose();
        super.submit(state, poses, collector, camera);
    }
    private void animate(String bone, State s, PoseStack poses) {
        if (species.equals("dimensional_fairy") || species.equals("dimensional_calamity")) {
            boolean fairy = species.equals("dimensional_fairy");
            float flap = (float)Math.sin(s.time * (fairy ? .95F : .22F)) * (fairy ? .95F : .45F);
            if (bone.equals("wing_left")) poses.rotate(Axis.YP.rotation(flap));
            if (bone.equals("wing_right")) poses.rotate(Axis.YP.rotation(-flap));
            if (bone.startsWith("arm_") && s.phase == 1) poses.rotate(Axis.XP.rotation(-.9F));
            if (bone.startsWith("leg_")) poses.rotate(Axis.XP.rotation((float)Math.sin(s.time * .13F) * .12F));
            if (bone.equals("tail")) poses.rotate(Axis.YP.rotation((float)Math.sin(s.time * .12F) * .18F));
        } else if (species.equals("dimensional_pterosaur")) {
            float flap = (float)Math.sin(s.time * .42F) * .55F;
            if (s.phase == DimensionalPterosaur.WINDUP) flap = .85F + (float)Math.sin(s.time * .7F) * .15F;
            if (s.phase == DimensionalPterosaur.CHARGE) flap = -.2F + flap * .15F;
            if (bone.equals("wing_left")) poses.rotate(Axis.ZP.rotation(flap));
            if (bone.equals("wing_right")) poses.rotate(Axis.ZP.rotation(-flap));
            if (bone.equals("tail")) poses.rotate(Axis.YP.rotation((float)Math.sin(s.time * .12F) * .1F));
            if (bone.equals("jaw") && s.phase != DimensionalPterosaur.TRACK) poses.rotate(Axis.XP.rotation(.25F));
        } else if (bone.startsWith("segment_")) {
            int segment = Integer.parseInt(bone.substring(8));
            float wave = s.time * (.12F + Math.min(s.speed, .4F)) - segment * .55F;
            float amplitude = .02F + Math.min(s.speed, .4F) * .25F;
            poses.translate(Math.sin(wave) * amplitude, 0, 0);
            poses.rotate(Axis.YP.rotation((float)Math.cos(wave) * amplitude * 1.5F));
        } else if (bone.equals("jaw") && s.phase == 1) poses.rotate(Axis.XP.rotation(.35F));
    }
}
