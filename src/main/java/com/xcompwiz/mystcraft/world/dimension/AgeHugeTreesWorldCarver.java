package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyWorldGenMystBigTreeKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.chunk.CarvingMask;

import java.util.Objects;
import java.util.function.Function;

/** CARVERS-stage adapter preserving the legacy MapGenAdvanced 17x17 source contribution model. */
public final class AgeHugeTreesWorldCarver implements AgeTerrainAlteration {
    private final LegacyWorldGenMystBigTreeKernel kernel;

    public AgeHugeTreesWorldCarver(LegacyWorldGenMystBigTreeKernel kernel) {
        this.kernel = Objects.requireNonNull(kernel, "kernel");
    }


    @Override
    public boolean apply(net.minecraft.core.RegistryAccess registries,
                         ChunkAccess targetChunk, ChunkPos sourceChunkPos) {

        ChunkPos target = targetChunk.getPos();
        var buffer = new ChunkAccessLegacyBigTreeBuffer(targetChunk);
        kernel.generateFromSource(
                sourceChunkPos.x(),
                sourceChunkPos.z(),
                target.x(),
                target.z(),
                buffer);
        return true;
    }
}
