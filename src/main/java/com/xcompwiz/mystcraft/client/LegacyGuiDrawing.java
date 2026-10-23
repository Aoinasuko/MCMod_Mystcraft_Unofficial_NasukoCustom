package com.xcompwiz.mystcraft.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Copies legacy texture crops and tint into the deferred GUI render state. */
final class LegacyGuiDrawing {
    private static final Map<GuiGraphicsExtractor, Integer> TINTS = new WeakHashMap<>();

    private LegacyGuiDrawing() {}

    static void setColor(GuiGraphicsExtractor graphics, float red, float green, float blue, float alpha) {
        int color = ARGB.colorFromFloat(alpha, red, green, blue);
        if (color == -1) TINTS.remove(graphics);
        else TINTS.put(graphics, color);
    }

    static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                     float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                textureWidth, textureHeight, TINTS.getOrDefault(graphics, -1));
    }

    static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
                     int width, int height, float u, float v, int sourceWidth, int sourceHeight,
                     int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                sourceWidth, sourceHeight, textureWidth, textureHeight, TINTS.getOrDefault(graphics, -1));
    }
}
