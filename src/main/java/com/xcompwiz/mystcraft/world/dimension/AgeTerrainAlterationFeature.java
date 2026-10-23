package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Executes material-writing legacy terrain operations before biome decoration. */
final class AgeTerrainAlterationFeature implements Feature {
    private final List<AgeTerrainAlteration> alterations;

    AgeTerrainAlterationFeature(List<AgeTerrainAlteration> alterations) {
        this.alterations = List.copyOf(alterations);
    }

    @Override
    public MapCodec<? extends Feature> codec() { return MapCodec.unit(this); }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        ChunkPos center = ChunkPos.containing(origin);
        var chunk = level.getChunk(center.x(), center.z());
        boolean changed = false;
        for (int x = center.x() - 8; x <= center.x() + 8; x++) {
            for (int z = center.z() - 8; z <= center.z() + 8; z++) {
                ChunkPos source = new ChunkPos(x, z);
                for (AgeTerrainAlteration alteration : alterations) {
                    changed |= alteration.apply(level.registryAccess(), chunk, source);
                }
            }
        }
        return changed;
    }
}
