package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.world.worldgen.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.levelgen.Heightmap;

/** Extracts legacy climate and scroll values into Minecraft's occluded precipitation renderer. */
public final class AgeRainRenderer {
    private AgeRainRenderer() {}
    public static void extract(ClientLevel level, int ticks, float partial,
                               double camX, double camY, double camZ,
                               AgeWeatherPlan plan, int radius, WeatherRenderState output) {
        var columns = AgeRainRenderBridge.columns(ticks, partial, camX, camY, camZ, plan.rainingStrength(), radius);
        for (var column : columns) {
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.x(), column.z());
            int lo = AgeWeatherColumnBoundsLegacyMath.lowerY(column.minY(), surface);
            int hi = AgeWeatherColumnBoundsLegacyMath.upperY(column.maxY(), surface);
            if (hi == lo || AgeLegacyPrecipitationClimateResolver.precipitation(level, column.x(), lo, column.z(), plan)
                    != AgePrecipitationBridge.Type.RAIN) continue;
            int light = LightCoordsUtil.getLightCoords(level, new BlockPos(column.x(),
                    AgeWeatherColumnBoundsLegacyMath.lightSampleY(surface, camY), column.z()));
            light = AgeWeatherPackedLightBridge.rainPacked(light);
            output.rainColumns.add(new WeatherEffectRenderer.ColumnInstance(
                    column.x(), column.z(), lo, hi, 0F, column.scroll(), light));
        }
    }
}
