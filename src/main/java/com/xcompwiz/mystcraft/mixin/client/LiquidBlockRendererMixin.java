package com.xcompwiz.mystcraft.mixin.client;

import com.xcompwiz.mystcraft.client.AgeBlockColorHandlers;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Decorates the water tint without replacing other mods' fluid models. */
@Mixin(FluidTintSources.class)
public abstract class LiquidBlockRendererMixin {
    @Inject(method = "water", at = @At("RETURN"), cancellable = true, remap = false)
    private static void mystcraft$waterTint(CallbackInfoReturnable<FluidTintSource> callback) {
        FluidTintSource original = callback.getReturnValue();
        callback.setReturnValue(new FluidTintSource() {
            public int color(FluidState state) { return original.color(state); }
            public int color(BlockState state) { return original.color(state); }
            public int colorInWorld(FluidState fluid, BlockState block, BlockAndTintGetter level, BlockPos pos) {
                return AgeBlockColorHandlers.waterFluidTint(original.colorInWorld(fluid, block, level, pos), level, pos);
            }
        });
    }
}
