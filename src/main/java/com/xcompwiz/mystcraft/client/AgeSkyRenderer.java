package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xcompwiz.mystcraft.world.worldgen.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FogType;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

/**
 * Modern consumer for AgeSkyRenderBridge.
 *
 * The bridge remains the semantic authority. This renderer owns only GPU-facing geometry.
 * Sun/moon use vanilla textures through the normal sky path when the Age has the ordinary
 * single celestial arrangement; custom/multiple celestial arrangements take this renderer path.
 */
public final class AgeSkyRenderer {
    private static final net.minecraft.resources.Identifier SUN =
        net.minecraft.resources.Identifier.withDefaultNamespace("textures/environment/celestial/sun.png");
    private static final String[] MOONS = {"full_moon", "waning_gibbous", "third_quarter", "waning_crescent", "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"};
    private static final net.minecraft.resources.Identifier END_SKY =
        net.minecraft.resources.Identifier.withDefaultNamespace("textures/environment/end_sky.png");
    private AgeSkyRenderer() {}

    public static boolean render(AgeDimensionSpecialEffects.SkyFrame frame, Matrix4f modelView) {
        if (frame.underwater()) return true;
        var state = frame.sky();
        var snapshot = frame.snapshot();
        long ageSeed = frame.seed();
        float rainStrength = frame.rain(), thunderStrength = frame.thunder();
        AgeSkyGpuUploader.additive(false);
        // Build/cache exact legacy CPU meshes now. GPU upload remains isolated from Symbol semantics.
        // This guarantees the final uploader cannot accidentally change star distributions or rainbow shape.
        LegacySkyMeshCache.prepare(ageSeed, state);
        PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        // Legacy ColorSky/ColorSkyNight: tint the upper sky plane. Natural/UNSET use level sky color.
        var skyColorBeforeWeather = AgeSkyColorBridge.sky(snapshot, frame.vanillaRgb(), frame.temperature(), frame.time(), frame.angle());
        var skyColor=skyColorBeforeWeather;
        // Legacy WorldProviderMyst applied rain then thunder attenuation after ColorSky resolution.
        // Natural sky already arrives weather-adjusted from ClientLevel; only custom ColorSky needs this pass.
        if (AgeSkyColorBridge.requiresLegacyWeatherPass(snapshot))
            skyColor=AgeSkyWeatherTintBridge.apply(skyColor,rainStrength,thunderStrength);
        // CP236 result-fidelity composition: the old upper sky plane was fogged by distance, so
        // ColorFog influenced only the apparent horizon rather than the whole hemisphere.
        // AgeSkyGeometry recreates that narrow horizon transition directly in vertex colours,
        // while terrain fog remains owned by ViewportEvent.ComputeFogColor.
        AgeColor horizonColor = frame.horizon();
        // We render from inside the dome. Vanilla sky rendering normally has face culling enabled,
        // so an outward-wound sphere is invisible from the camera and only the clear/fog colour is seen.
        // Disable culling for this single batch, then restore it immediately.

        AgeSkyGpuUploader.drawIntrinsicColored(
                AgeSkyGeometry.verticalGradientDome(skyColor, horizonColor), pose.last().pose(), 1F);

        // Restore the caller-owned terrain fog state before sun/moon/stars. The dome itself does
        // not depend on shader fog, so no stale fog colour can bleach the sky.
        for (var batch : AgeSkyBatchPlanner.plan(state)) {
            pose.pushPose();
            if(batch.mesh()!=AgeSkyBatchPlanner.Mesh.SUNSET_FAN) {
                pose.rotate(Axis.YP.rotationDegrees(batch.yawDegrees()));
                pose.rotate(Axis.XP.rotationDegrees(batch.pitchDegrees()));
            }
            AgeSkyGpuUploader.additive(batch.additive());
            switch (batch.mesh()) {
                case NORMAL_STARS -> AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.normalStars(), pose.last().pose(),batch.red(),batch.green(),batch.blue(),batch.alpha());
                case TWINKLE_LAYER -> AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.twinkle(batch.symbolSeed(),batch.randomizedPeriod(),batch.randomizedAngle()).get(batch.layer()), pose.last().pose(),batch.red(),batch.green(),batch.blue(),batch.alpha());
                case RAINBOW -> AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.rainbow(), pose.last().pose(),batch.red(),batch.green(),batch.blue(),batch.alpha());
                case SUN_QUAD -> AgeSkyGpuUploader.drawTexturedQuad(AgeSkyGeometry.celestialQuad(batch.size()), pose.last().pose(), SUN, batch.alpha());
                case MOON_QUAD -> {
                    var moon = net.minecraft.resources.Identifier.withDefaultNamespace("textures/environment/celestial/moon/" + MOONS[Math.floorMod(batch.moonPhase(), 8)] + ".png");
                    // Legacy moon reverses U across the quad.
                    AgeSkyGpuUploader.drawTexturedRegion(AgeSkyGeometry.celestialQuad(batch.size()), pose.last().pose(), moon,
                            batch.alpha(), 1F, 1F, 0F, 0F);
                }
                case END_SKY_CUBE -> {
                    var c=state.endSkyColor();
                    AgeSkyGpuUploader.drawEndSkyLegacy(AgeSkyGeometry.endSkyCube(),pose.last().pose(),END_SKY,
                            c.r(),c.g(),c.b(),batch.alpha());
                }
                case SUNSET_FAN -> {
                    // Legacy: rotate 90 X, flip by celestial sine, then -angle around Z.
                    pose.rotate(Axis.XP.rotationDegrees(90F));
                    if(Math.sin(batch.pitchDegrees()*Math.PI/180.0)<0) pose.rotate(Axis.ZP.rotationDegrees(180F));
                    pose.rotate(Axis.ZP.rotationDegrees(-batch.yawDegrees()));
                    AgeSkyGpuUploader.drawSunsetFan(AgeSkyGeometry.sunsetFan(batch.alpha()),pose.last().pose(),batch.red(),batch.green(),batch.blue());
                }
                case HORIZON -> {
                    // 1.12 drew a finite lower sky grid here while fixed-function fog hid its
                    // outer edge. Replaying that mesh in the 1.21.1 shader pipeline exposes the
                    // grid as a large horizontal rectangle/band near the horizon. Modern sky/fog
                    // already owns that transition, so preserve the HORIZON semantic in the plan
                    // but do not submit the obsolete lower plane. VOID below remains explicit.
                }
                case VOID -> {
                    double horizonDst=frame.camera().y-state.horizonHeight();
                    if(horizonDst<0) {
                        pose.translate(0,12,0);
                        AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.lowerSky(),pose.last().pose(),0,0,0,1);
                        float topY=-((float)(horizonDst+65.0));
                        AgeSkyGpuUploader.drawColored(AgeSkyGeometry.voidEnclosure(topY),pose.last().pose(),0,0,0,1);
                    }
                }
                default -> { /* textured sun/moon/end-sky and camera-relative horizon/void handled below */ }
            }
            pose.popPose();
        }


        return true;
    }
}
