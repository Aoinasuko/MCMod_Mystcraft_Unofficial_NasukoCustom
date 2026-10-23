package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Material-writing terrain operations, separate from 26.3's air-only carving mask. */
interface AgeTerrainAlteration {
    boolean apply(RegistryAccess registries, ChunkAccess targetChunk, ChunkPos sourceChunkPos);
}
