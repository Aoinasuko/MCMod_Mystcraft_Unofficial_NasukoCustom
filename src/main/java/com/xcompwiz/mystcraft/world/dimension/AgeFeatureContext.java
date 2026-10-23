package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** The per-placement inputs shared by the legacy population helpers. */
public record AgeFeatureContext(WorldGenLevel level, ChunkGenerator chunkGenerator,
                                RandomSource random, BlockPos origin) {}
