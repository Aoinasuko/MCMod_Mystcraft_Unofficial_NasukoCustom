package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacySkylandsKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.chunk.CarvingMask;

import java.util.Objects;
import java.util.function.Function;

/**
 * One-shot modern adapter for the legacy Skylands primer filter.
 *
 * <p>ChunkGenerator invokes configured carvers once per source chunk in a 17x17 scan. Skylands
 * was not a MapGen source-contribution algorithm, so only the source==target callback executes
 * the 16x16 column filter. This prevents the same destructive filter from being replayed 289 times.</p>
 */
public final class AgeSkylandsWorldCarver implements AgeTerrainAlteration {
    private final LegacySkylandsKernel kernel;

    public AgeSkylandsWorldCarver(LegacySkylandsKernel kernel) {
        this.kernel = Objects.requireNonNull(kernel, "kernel");
    }


    @Override
    public boolean apply(net.minecraft.core.RegistryAccess registries,
                         ChunkAccess targetChunk, ChunkPos sourceChunkPos) {

        ChunkPos target = targetChunk.getPos();
        if (sourceChunkPos.x() != target.x() || sourceChunkPos.z() != target.z()) return false;

        int[] cutoff = kernel.cutoffHeights(target.x(), target.z());
        int minX = target.getMinBlockX();
        int minZ = target.getMinBlockZ();
        int minY = targetChunk.getMinY();
        int maxY = targetChunk.getMaxY() + 1 - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int legacyHeight = cutoff[(x << 4) | z];
                int height = ModernAgeHeight.legacySampleY(targetChunk, legacyHeight);
                for (int y = minY; y <= maxY; y++) {
                    pos.set(minX + x, y, minZ + z);
                    var state = targetChunk.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (y <= height || !state.getFluidState().isEmpty()) {
                        targetChunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), 0);
                    }
                }
            }
        }
        return true;
    }
}
