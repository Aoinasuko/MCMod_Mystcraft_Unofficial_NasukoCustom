package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.blockentity.*;
import com.xcompwiz.mystcraft.fluid.*;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.*;
import com.xcompwiz.mystcraft.world.agedata.*;
import com.xcompwiz.mystcraft.world.dimension.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Opt-in integration tests, executed by runGameTestServer in the real 26.3 runtime. */
public final class PortGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, "mystcraft");
    public static void register(IEventBus bus) {
        DimensionalMonsterGameTests.register(bus);
        DeskArchivistGameTests.register(bus);
        FUNCTIONS.register("ink_transactions", () -> PortGameTests::ink);
        FUNCTIONS.register("binder_persistence", () -> PortGameTests::binder);
        FUNCTIONS.register("archivist_processor_persistence", () -> PortGameTests::processor);
        for (String terrain : List.of("Normal", "Amplified", "Flat", "Nether", "End", "Void")) {
            FUNCTIONS.register("age_" + terrain.toLowerCase(java.util.Locale.ROOT), () -> h -> age(h, terrain));
        }
        FUNCTIONS.register(bus);
        bus.addListener(PortGameTests::tests);
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("mystcraft", name); }
    private static void tests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("port_validation"));
        for (String name : List.of("ink_transactions", "binder_persistence", "archivist_processor_persistence", "age_normal", "age_amplified", "age_flat", "age_nether", "age_end", "age_void")) {
            event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                    new TestData<>(environment, id("port_empty"), 1200, 0, true)));
        }
    }
    private static void processor(GameTestHelper h) {
        var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var codec = net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType.SINGLE_CODEC;
        var processor = com.xcompwiz.mystcraft.world.worldgen.WritingDeskStructureProcessor.INSTANCE;
        var encoded = codec.encodeStart(ops, processor).getOrThrow();
        h.assertTrue(codec.parse(ops, encoded).getOrThrow() == processor, "Archivist processor codec roundtrip");
        // Exercise the actual house-binding path: direct profession holders cannot be saved in 26.3.
        var pos = h.absolutePos(new BlockPos(1, 1, 1)); var level = h.getLevel();
        var state = MystBlocks.WRITING_DESK.get().defaultBlockState()
                .setValue(com.xcompwiz.mystcraft.block.BlockWritingDesk.FACING, net.minecraft.core.Direction.NORTH);
        level.setBlock(pos, state, 2);
        level.setBlock(pos.north(), state.setValue(com.xcompwiz.mystcraft.block.BlockWritingDesk.IS_FOOT, true), 2);
        for (var lectern : List.of(pos.east(3).above(), pos.east(3).above().north()))
            level.setBlock(lectern, MystBlocks.LECTERN.get().defaultBlockState(), 2);
        var desk = (WritingDeskBlockEntity)level.getBlockEntity(pos);
        for (int i = 0; i < 100; i++) WritingDeskBlockEntity.serverTick(level, pos, state, desk);
        var villagers = level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,
                new net.minecraft.world.phys.AABB(pos).inflate(6),
                v -> v.getVillagerData().profession().is(MystVillagers.ARCHIVIST.getKey()));
        h.assertTrue(!villagers.isEmpty(), "Desk binds its generated Archivist");
        var data = villagers.getFirst().getVillagerData();
        var villagerCodec = net.minecraft.world.entity.npc.villager.VillagerData.CODEC;
        var saved = villagerCodec.encodeStart(ops, data).getOrThrow();
        h.assertTrue(villagerCodec.parse(ops, saved).getOrThrow().profession().is(MystVillagers.ARCHIVIST.getKey()),
                "Generated Archivist profession survives serialization");
        h.succeed();
    }
    private static void ink(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        var level = h.getLevel();
        level.setBlockAndUpdate(pos, MystBlocks.WRITING_DESK.get().defaultBlockState());
        var desk = (WritingDeskBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(desk != null, "Desk block entity must be registered");
        var handler = new WritingDeskInkHandler(desk);
        var ink = FluidResource.of(MystFluids.BLACK_INK.get());
        try (var tx = Transaction.openRoot()) { h.assertValueEqual(handler.insert(ink, 1000, tx), 1000, "insert"); }
        h.assertValueEqual(desk.inkAmount(), 0, "rolled back ink");
        try (var tx = Transaction.openRoot()) { handler.insert(ink, 1000, tx); tx.commit(); }
        h.assertValueEqual(desk.inkAmount(), 1000, "committed ink");
        try (var tx = Transaction.openRoot()) { handler.extract(ink, 250, tx); }
        h.assertValueEqual(desk.inkAmount(), 1000, "rolled back extraction");
        var vial = FluidContainerAccess.of(new ItemStack(MystItems.INK_VIAL.get()));
        h.assertTrue(vial != null, "Vial capability");
        var fluid = new FluidStack(MystFluids.BLACK_INK.get(), 1000);
        h.assertValueEqual(vial.drain(fluid, FluidContainerAccess.FluidAction.SIMULATE).getAmount(), 1000, "simulated drain");
        h.assertTrue(vial.getContainer().is(MystItems.INK_VIAL.get()), "Simulation preserves vial");
        h.assertValueEqual(vial.drain(fluid, FluidContainerAccess.FluidAction.EXECUTE).getAmount(), 1000, "drain");
        h.assertTrue(vial.getContainer().is(Items.GLASS_BOTTLE), "Drain returns bottle");
        var copy = new WritingDeskBlockEntity(pos, desk.getBlockState()); copy.setLevel(level);
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), desk.saveWithoutMetadata(level.registryAccess())));
        h.assertValueEqual(copy.inkAmount(), 1000, "persisted ink");
        h.succeed();
    }
    private static void binder(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(1,1,1));
        level.setBlockAndUpdate(pos, MystBlocks.BOOK_BINDER.get().defaultBlockState());
        var binder = (BookBinderBlockEntity) level.getBlockEntity(pos);
        binder.setItem(0, new ItemStack(Items.LEATHER));
        var page = new ItemStack(MystItems.PAGE.get()); Page.setSymbolId(page, "mystcraft:TerrainNormal");
        binder.insertPage(Page.createLinkPage(), 0, true); binder.insertPage(page, 1, true); binder.setPendingTitle("Nasuko Test");
        var copy = new BookBinderBlockEntity(pos, binder.getBlockState()); copy.setLevel(level);
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), binder.saveWithoutMetadata(level.registryAccess())));
        h.assertValueEqual(copy.getPageCount(), 2, "page count after save");
        h.assertValueEqual(Page.getSymbolId(copy.getPage(1)), "mystcraft:TerrainNormal", "page symbol");
        h.assertValueEqual(copy.getPendingTitle(), "Nasuko Test", "title");
        h.assertTrue(copy.canBuildItem(), "Binder recipe survives serialization");
        var player = (net.minecraft.server.level.ServerPlayer) h.makeMockServerPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var book = copy.createBuiltItemSnapshot(player);
        h.assertTrue(book.is(MystItems.AGEBOOK.get()), "Binder creates Descriptive Book");
        var record = AgeManager.establish(player, book);
        h.assertValueEqual(AgeManager.establish(player, book).uuid(), record.uuid(), "Establishment is idempotent");
        h.succeed();
    }
    private static void age(GameTestHelper h, String terrain) {
        var server = h.getLevel().getServer(); var registry = AgeRegistryData.get(server);
        int uid = registry.allocateUid();
        var record = new AgeRecord(uid, AgeDimensionKeys.levelKeyString(uid), "Port Test", 630025L, UUID.randomUUID());
        record.setSymbols(List.of("mystcraft:Terrain" + terrain)); record.setDayTime(6000); registry.put(record);
        var prepared = AgeRuntimeDimensionLoader.prepare(server, record);
        h.assertTrue(prepared.hasPreparedDefinition(), "Age must prepare");
        var result = AgeRuntimeServerLevelInstaller.install(server, record);
        h.assertTrue(result.usable(), "Age installation: " + result.status());
        var level = (MystcraftRuntimeServerLevel) result.level();
        h.assertValueEqual(level.getSeed(), 630025L, "Age seed");
        h.assertValueEqual(level.getDefaultClockTime(), 6000L, "initial local clock");
        long overworldTime = server.overworld().getDefaultClockTime();
        level.tickAgeClock();
        h.assertValueEqual(server.overworld().getDefaultClockTime(), overworldTime, "Overworld clock isolation");
        h.assertValueEqual(level.getDefaultClockTime(), 6001L, "local clock tick");
        var chunk = level.getChunk(0, 0);
        h.assertTrue(chunk != null, "Age chunk generation");
        var restored = AgeRegistryData.load(registry.save(new CompoundTag(), server.registryAccess()), server.registryAccess()).byUid(uid).orElseThrow();
        h.assertValueEqual(restored.uuid(), record.uuid(), "Age identity roundtrip");
        h.assertTrue(AgeRuntimeWorldManager.unload(server, level.dimension()).unloaded(), "Age unload");
        h.assertTrue(AgeRuntimeDimensionLoader.prepare(server, record).hasPreparedDefinition(), "Age reprepare");
        h.assertTrue(AgeRuntimeServerLevelInstaller.install(server, record).usable(), "Age reload");
        h.succeed();
    }
}
