package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenFloatingIslandsKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.chunk.CarvingMask;

import java.util.Objects;
import java.util.function.Function;

/**
 * Runtime CARVERS-stage adapter for the legacy floating-island geometry.
 *
 * <p>The column mask is handed to `AgeFloatingIslandColumnMasks`, which unions source
 * contributions for the target chunk. 13H-10 consumes that mask for legacy surface/filler
 * and quart-biome replacement.</p>
 */
public final class AgeFloatingIslandsWorldCarver implements AgeTerrainAlteration {
    private final LegacyMapGenFloatingIslandsKernel kernel;
    private final BlockState replacement;
    private final BlockState top;
    private final BlockState filler;
    private final BlockState sandstone;
    private final String biomeLegacySymbol;
    private final AgeFloatingIslandColumnMasks masks = new AgeFloatingIslandColumnMasks();

    public AgeFloatingIslandsWorldCarver(
            LegacyMapGenFloatingIslandsKernel kernel,
            BlockState replacement,
            String biomeLegacySymbol) {
        this.kernel = Objects.requireNonNull(kernel, "kernel");
        this.replacement = Objects.requireNonNull(replacement, "replacement");
        this.biomeLegacySymbol = biomeLegacySymbol;

        var surface = com.xcompwiz.mystcraft.world.worldgen.LegacyBiomeSurfaceRegistry.resolve(
                biomeLegacySymbol);
        this.top = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                net.minecraft.resources.Identifier.parse(surface.topBlockId()))
                .defaultBlockState();
        this.filler = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                net.minecraft.resources.Identifier.parse(surface.fillerBlockId()))
                .defaultBlockState();
        this.sandstone = net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState();
    }


    @Override
    public boolean apply(net.minecraft.core.RegistryAccess registries,
                         ChunkAccess targetChunk, ChunkPos sourceChunkPos) {

        ChunkPos target = targetChunk.getPos();
        var buffer = new ChunkAccessFloatingIslandBuffer(
                targetChunk, replacement, top, filler, sandstone);

        // Legacy MapGen uses one mutable Random and one NoiseGeneratorOctaves instance.
        // Synchronizing preserves that execution model under modern parallel chunk generation.
        synchronized (kernel) {
            kernel.generateFromSource(
                    sourceChunkPos.x(),
                    sourceChunkPos.z(),
                    target.x(),
                    target.z(),
                    buffer);
        }

        masks.mergeAndMaybeComplete(
                target,
                buffer.copyModifiedColumns(),
                biomeLegacySymbol)
                .ifPresent(entry -> AgeFloatingIslandBiomeFinalizer.apply(
                        registries,
                        targetChunk,
                        entry.columns(),
                        entry.biomeLegacySymbol()));

        return true;
    }
}
