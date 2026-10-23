package com.xcompwiz.mystcraft.registry;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.worldgen.WritingDeskStructureProcessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Structure-piece saves dispatch through this registry even for directly injected village pools. */
public final class MystStructureProcessors {
    private static final DeferredRegister<MapCodec<? extends StructureProcessor>> PROCESSORS =
            DeferredRegister.create(BuiltInRegistries.STRUCTURE_PROCESSOR, Mystcraft.MOD_ID);
    static { PROCESSORS.register("writing_desk", () -> WritingDeskStructureProcessor.CODEC); }
    private MystStructureProcessors() {}
    public static void register(IEventBus bus) { PROCESSORS.register(bus); }
}
