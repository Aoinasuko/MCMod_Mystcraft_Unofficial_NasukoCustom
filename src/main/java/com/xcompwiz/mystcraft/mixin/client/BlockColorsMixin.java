package com.xcompwiz.mystcraft.mixin.client;

import com.xcompwiz.mystcraft.client.AgeBlockColorHandlers;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Wrap final vanilla/mod tint sources while retaining their layer and property contracts. */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {
    @Unique private static final Map<List<BlockTintSource>, List<BlockTintSource>> mystcraft$wrapped = new WeakHashMap<>();

    @Inject(method = "getTintSources", at = @At("RETURN"), cancellable = true)
    private void mystcraft$ageTints(BlockState state, CallbackInfoReturnable<List<BlockTintSource>> callback) {
        if (!AgeBlockColorHandlers.isGrassTintBlock(state) && !AgeBlockColorHandlers.isFoliageTintBlock(state)) return;
        synchronized (mystcraft$wrapped) {
            callback.setReturnValue(mystcraft$wrapped.computeIfAbsent(callback.getReturnValue(), sources ->
                    sources.stream().map(source -> (BlockTintSource) new BlockTintSource() {
                        public int color(BlockState blockState) { return source.color(blockState); }
                        public int colorInWorld(BlockState blockState, BlockAndTintGetter level, BlockPos pos) {
                            int original = source.colorInWorld(blockState, level, pos);
                            return 0xFF000000 | AgeBlockColorHandlers.finalModelTint(blockState, level, pos, 0, original);
                        }
                        public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>> relevantProperties() {
                            return source.relevantProperties();
                        }
                    }).toList()));
        }
    }
}
