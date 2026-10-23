package com.xcompwiz.mystcraft.mixin.client;

import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Age lighting at extraction time; render threads consume only the snapshot. */
@Mixin(LightmapRenderStateExtractor.class)
abstract class AgeLightmapMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void mystcraft$lighting(LightmapRenderState state, float partial, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level == null || !state.needsUpdate) return;
        var entry = AgeClientVisualState.get(level.dimension().identifier().toString());
        if (entry == null) return;
        switch (entry.snapshot().lighting()) {
            case BRIGHT -> {
                state.blockFactor *= .75F;
                state.skyFactor *= .75F;
                state.ambientColor = new Vector3f(state.ambientColor).mul(.75F).add(.25F, .25F, .25F);
            }
            case DARK -> {
                state.blockFactor *= .5F;
                state.skyFactor *= .5F;
                state.ambientColor = new Vector3f(state.ambientColor).mul(.5F);
            }
            default -> { }
        }
    }
}
