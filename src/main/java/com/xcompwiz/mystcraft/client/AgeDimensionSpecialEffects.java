package com.xcompwiz.mystcraft.client;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import com.xcompwiz.mystcraft.world.worldgen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Extracts immutable Age environment state for the 26.3 render thread. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public final class AgeDimensionSpecialEffects implements CustomSkyboxRenderer {
    private static final AgeDimensionSpecialEffects INSTANCE = new AgeDimensionSpecialEffects();
    private static final ContextKey<SkyFrame> FRAME = new ContextKey<>(Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "sky_frame"));

    public record SkyFrame(AgeVisualSnapshot snapshot, AgeSkyRenderBridge.RenderState sky,
                           Vec3 camera, boolean underwater, int vanillaRgb, float temperature,
                           long time, float angle, float rain, float thunder, AgeColor horizon, long seed) {}

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        var level = event.getLevel();
        var entry = AgeClientVisualState.get(level.dimension().identifier().toString());
        if (entry == null) return;
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var snapshot = entry.snapshot();
        var output = event.getRenderState();
        var camera = event.getCamera();
        var position = camera.position();
        long time = level.getDefaultClockTime();
        float angle = (time % 24000L + partial) / 24000F;
        float rain = level.getRainLevel(partial), thunder = level.getThunderLevel(partial);
        var plan = new AgeSkyPlan(snapshot.celestials(), snapshot.drawHorizon(), snapshot.drawVoid(),
                snapshot.horizonHeight(), snapshot.cloudHeight(), snapshot.endSkyBackground(), snapshot.endSkyGradient());
        var sky = AgeSkyRenderBridge.resolve(plan, time, partial, rain, output.skyRenderState.starBrightness, entry.ageSeed());
        var color = output.skyRenderState.skyColor;
        int vanillaRgb = color == null ? 0x78A7FF : ARGB.colorFromFloat(1, color.x(), color.y(), color.z()) & 0xFFFFFF;
        BlockPos pos = BlockPos.containing(position);
        var weather = AgeWeatherResolver.resolveMode(snapshot.weather());
        float temperature = AgeLegacyPrecipitationClimateResolver.controlledTemperature(level, pos.getX(), pos.getY(), pos.getZ(), weather);
        var fog = snapshot.colors().get("fog");
        AgeColor horizon = fog != null && !fog.isEmpty() ? AgeFogColorBridge.resolve(fog, time / 12000F, angle)
                : AgeVanillaFogColorCache.get(level.dimension().identifier().toString());
        if (horizon == null) horizon = AgeFogColorBridge.resolve(java.util.List.of(), time / 12000F, angle);
        output.setRenderData(FRAME, new SkyFrame(snapshot, sky, position, camera.getFluidInCamera() == FogType.WATER,
                vanillaRgb, temperature, time, angle, rain, thunder, horizon, entry.ageSeed()));
        output.customSkyboxRenderer = INSTANCE;
        AgeCloudRenderer.extract(level, snapshot, output, partial);
        if (AgePrecipitationHookBridge.suppressVanilla(weather)) output.weatherRenderState.reset();
        else if (AgePrecipitationHookBridge.needsCustomRainRenderer(weather) || AgePrecipitationHookBridge.needsCustomSnowRenderer(weather)) {
            var precipitation = output.weatherRenderState;
            precipitation.reset();
            int radius = AgeWeatherRenderDistanceContract.radius(Minecraft.getInstance().options.graphicsPreset().get() != net.minecraft.client.GraphicsPreset.FAST);
            precipitation.radius = radius;
            precipitation.intensity = weather.rainingStrength();
            AgeRainRenderer.extract(level, (int) level.getGameTime(), partial, position.x, position.y, position.z, weather, radius, precipitation);
            AgeSnowRenderer.extract(level, (int) level.getGameTime(), partial, position.x, position.y, position.z, weather, radius, precipitation);
        }
    }

    @Override
    public boolean renderSky(LevelRenderState output, SkyRenderState sky, Matrix4fc view, GpuBufferSlice fog) {
        var frame = output.getRenderData(FRAME);
        if (frame == null) return false;
        return AgeSkyRenderer.render(frame, new Matrix4f(view));
    }
}
