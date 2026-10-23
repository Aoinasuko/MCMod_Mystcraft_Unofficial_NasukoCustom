package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenAdvancedKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.chunk.CarvingMask;

import java.util.Objects;
import java.util.function.Function;

/**
 * Adapts one legacy MapGen source-chunk contribution to Minecraft 1.21.1's carver pass.
 *
 * <p>Modern ChunkGenerator already walks the same 8-chunk radius (17x17 source chunks)
 * for legacy carvers, so this adapter invokes only one source contribution per callback.
 * The modern RandomSource/config are intentionally ignored: Mystcraft's exact 1.12 seed
 * formula is owned by the compatibility kernel.</p>
 */
public final class AgeLegacyMapGenWorldCarver implements AgeTerrainAlteration {
    private final LegacyMapGenAdvancedKernel kernel;
    private final net.minecraft.world.level.block.state.BlockState replacementState;

    public AgeLegacyMapGenWorldCarver(
            LegacyMapGenAdvancedKernel kernel,
            net.minecraft.world.level.block.state.BlockState replacementState) {
        this.kernel = Objects.requireNonNull(kernel, "kernel");
        this.replacementState = Objects.requireNonNull(replacementState, "replacementState");
    }


    @Override
    public boolean apply(net.minecraft.core.RegistryAccess registries,
                         ChunkAccess targetChunk, ChunkPos sourceChunkPos) {

        ChunkPos target = targetChunk.getPos();
        ChunkAccessLegacyTerrainBuffer buffer =
                new ChunkAccessLegacyTerrainBuffer(targetChunk, replacementState);

        synchronized (kernel) {
            kernel.generateFromSource(
                    sourceChunkPos.x(),
                    sourceChunkPos.z(),
                    target.x(),
                    target.z(),
                    buffer);
        }
        return true;
    }

}
