package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.world.worldgen.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.ARGB;

/** Supplies Age-specific height and color to Minecraft's native cloud renderer. */
public final class AgeCloudRenderer {
    private AgeCloudRenderer() {}
    public static void extract(ClientLevel level, AgeVisualSnapshot snapshot, LevelRenderState output, float partial) {
        output.cloudHeight = snapshot.cloudHeight();
        var colors = snapshot.colors().get("cloud");
        if (colors != null && !colors.isEmpty()) {
            AgeColor color = AgeCloudColorBridge.resolve(snapshot, level.getDefaultClockTime(),
                    (level.getDefaultClockTime() % 24000L + partial) / 24000F,
                    level.getRainLevel(partial), level.getThunderLevel(partial));
            output.cloudColor = ARGB.colorFromFloat(1, color.r(), color.g(), color.b());
        }
    }
}
