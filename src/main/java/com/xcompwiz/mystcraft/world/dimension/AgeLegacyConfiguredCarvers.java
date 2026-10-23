package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind;
import com.xcompwiz.mystcraft.world.worldgen.AgeFeaturePlan;
import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenCavesKernel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.carver.WorldCarver;


import java.util.ArrayList;
import java.util.List;

/** Builds runtime-only configured carvers/adapters for literal legacy terrain-alteration Symbols. */
public final class AgeLegacyConfiguredCarvers {
    private AgeLegacyConfiguredCarvers() {}

    public static List<Holder<WorldCarver>> create(
            RegistryAccess registries,
            AgeFeaturePlan plan,
            long ageSeed) {

        var carverRegistry = registries.lookupOrThrow(Registries.CARVER);
        ArrayList<Holder<WorldCarver>> out = new ArrayList<>();
        for (var entry : plan.entries()) {
            if (entry.kind() == AgeFeatureKind.CAVES) out.add(carverRegistry.getOrThrow(Carvers.CAVE));
            if (entry.kind() == AgeFeatureKind.RAVINES) out.add(carverRegistry.getOrThrow(Carvers.CANYON));
        }
        return List.copyOf(out);
    }

    static List<AgeTerrainAlteration> alterations(AgeFeaturePlan plan, long ageSeed) {
        ArrayList<AgeTerrainAlteration> out = new ArrayList<>();
        for (var entry : plan.entries()) {
            switch (entry.kind()) {
                // CP246: ordinary caves/ravines are engine features, not Mystcraft-only
                // geometry. Use Minecraft 1.21.1's own configured carvers so they honor
                // the modern -64..319 world, aquifers, carving masks and current terrain
                // contracts. Repeated pages still append repeated entries.
                case CAVES -> { }

                case RAVINES -> { }

                case TENDRILS -> {
                    Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                            Identifier.parse(entry.materialBlockId()));
                    out.add(direct(
                            new LegacyMapGenCavesKernel(ageSeed, 15, 18, true),
                            block.defaultBlockState()));
                }

                case SPHERES -> {
                    Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                            Identifier.parse(entry.materialBlockId()));
                    out.add(direct(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenSpheresKernel(ageSeed),
                            block.defaultBlockState()));
                }

                case FLOATING_ISLANDS -> {
                    Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(
                            Identifier.parse(entry.materialBlockId()));
                    var carver = new AgeFloatingIslandsWorldCarver(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenFloatingIslandsKernel(ageSeed),
                            block.defaultBlockState(),
                            entry.biomeLegacySymbol());
                    out.add(carver);
                }

                case SKYLANDS -> {
                    var carver = new AgeSkylandsWorldCarver(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacySkylandsKernel(ageSeed));
                    out.add(carver);
                }

                case HUGE_TREES -> {
                    var carver = new AgeHugeTreesWorldCarver(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacyWorldGenMystBigTreeKernel(ageSeed));
                    out.add(carver);
                }

                default -> {
                }
            }
        }

        return List.copyOf(out);
    }

    private static AgeTerrainAlteration direct(
            com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenAdvancedKernel kernel,
            net.minecraft.world.level.block.state.BlockState replacementState) {
        return new AgeLegacyMapGenWorldCarver(kernel, replacementState);
    }
}
