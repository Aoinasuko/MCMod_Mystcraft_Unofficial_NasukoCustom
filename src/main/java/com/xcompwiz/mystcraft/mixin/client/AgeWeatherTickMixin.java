package com.xcompwiz.mystcraft.mixin.client;

import com.xcompwiz.mystcraft.client.AgePrecipitationTicker;
import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import com.xcompwiz.mystcraft.world.worldgen.AgePrecipitationHookBridge;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class AgeWeatherTickMixin {
    @Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
    private void mystcraft$precipitation(CallbackInfo ci) {
        var level = (ClientLevel) (Object) this;
        var entry = AgeClientVisualState.get(level.dimension().identifier().toString());
        if (entry == null) return;
        var weather = AgeWeatherResolver.resolveMode(entry.snapshot().weather());
        if (AgePrecipitationHookBridge.suppressVanilla(weather)) ci.cancel();
        else if (AgePrecipitationHookBridge.needsCustomRainRenderer(weather) || AgePrecipitationHookBridge.needsCustomSnowRenderer(weather)) {
            AgePrecipitationTicker.tick(level, (int) level.getGameTime(), Minecraft.getInstance().gameRenderer.mainCamera(), weather);
            ci.cancel();
        }
    }
}
