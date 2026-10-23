package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.entity.monster.*;
import com.xcompwiz.mystcraft.instability.*;
import com.xcompwiz.mystcraft.registry.MystEntities;
import com.xcompwiz.mystcraft.world.DimensionalMonsterPolicy;
import com.xcompwiz.mystcraft.world.agedata.*;
import com.xcompwiz.mystcraft.world.dimension.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Opt-in native runtime coverage of page persistence, spawn hooks, combat and movement. */
public final class DimensionalMonsterGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, "mystcraft");
    private static final List<String> NAMES = List.of("monster_pages_and_spawns", "monster_attributes_and_poison", "pterosaur_charge", "serpent_pounce");
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("mystcraft", name); }
    public static void register(IEventBus bus) {
        FUNCTIONS.register(NAMES.get(0), () -> DimensionalMonsterGameTests::pages);
        FUNCTIONS.register(NAMES.get(1), () -> DimensionalMonsterGameTests::combat);
        FUNCTIONS.register(NAMES.get(2), () -> DimensionalMonsterGameTests::pterosaur);
        FUNCTIONS.register(NAMES.get(3), () -> DimensionalMonsterGameTests::serpent);
        FUNCTIONS.register(bus); bus.addListener(DimensionalMonsterGameTests::tests);
    }
    private static void tests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("monster_validation"), new TestEnvironmentDefinition.SetDifficulty(Difficulty.NORMAL));
        for (String name : NAMES) event.registerTest(id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(name)), new TestData<>(environment, id("port_empty"), 400, 0, true)));
    }
    private static void pages(GameTestHelper h) {
        String symbol = DimensionalMonsterPolicy.SYMBOL;
        h.assertValueEqual(DimensionalMonsterPolicy.reduction(List.of()), 0, "no page, no discount");
        h.assertValueEqual(DimensionalMonsterPolicy.reduction(List.of(symbol)), 500, "first page");
        h.assertValueEqual(DimensionalMonsterPolicy.reduction(List.of(symbol, symbol)), 750, "second page halves benefit");
        h.assertValueEqual(DimensionalMonsterPolicy.reduction(List.of(symbol, symbol, symbol)), 875, "third page");
        h.assertValueEqual(DimensionalMonsterPolicy.reduction(Collections.nCopies(100, symbol)), 994, "bounded reward");
        var server = h.getLevel().getServer(); var registry = AgeRegistryData.get(server);
        int uid = registry.allocateUid();
        var record = new AgeRecord(uid, AgeDimensionKeys.levelKeyString(uid), "Monster Page Test", 630025L, UUID.randomUUID());
        record.setSymbols(List.of("mystcraft:TerrainVoid", symbol, symbol)); registry.put(record);
        var restored = AgeRegistryData.load(registry.save(new CompoundTag(), server.registryAccess()), server.registryAccess()).byUid(uid).orElseThrow();
        h.assertValueEqual(DimensionalMonsterPolicy.pageCount(restored.symbols()), 2, "duplicate page persistence");
        var score = LegacyStaticInstabilityCalculator.calculate(restored);
        h.assertValueEqual(Collections.frequency(score.effectiveSymbols(), symbol), 2, "grammar preserves authored pages");
        var counts = new HashMap<String, Integer>(); int ordinaryCost = 0;
        for (String entry : score.effectiveSymbols()) ordinaryCost += LegacySymbolInstability.instabilityModifier(entry, counts.merge(entry, 1, Integer::sum));
        h.assertValueEqual(score.directSymbolInstability(), ordinaryCost - 750, "discount reaches actual instability calculator");
        h.assertTrue(!DimensionalMonsterSpawns.enabled(server.overworld()), "Overworld exclusion");
        h.assertTrue(AgeRuntimeDimensionLoader.prepare(server, record).hasPreparedDefinition(), "prepare monster Age");
        var level = AgeRuntimeServerLevelInstaller.install(server, record).level();
        h.assertTrue(level != null && DimensionalMonsterSpawns.enabled(level), "authored Age enables monsters");
        var zombie = new MobSpawnSettings.SpawnerData(EntityTypes.ZOMBIE, ConstantInt.of(1));
        var list = WeightedList.of(zombie);
        var event = new LevelEvent.PotentialSpawns(level, MobCategory.MONSTER, new BlockPos(8, 64, 8), list);
        NeoForge.EVENT_BUS.post(event);
        h.assertValueEqual(event.getSpawnerDataList().size(), 3, "native event appends two species and preserves vanilla");
        DimensionalMonsterSpawns.potentialSpawns(event);
        h.assertValueEqual(event.getSpawnerDataList().size(), 3, "no duplicate weights");
        var passive = new LevelEvent.PotentialSpawns(level, MobCategory.CREATURE, BlockPos.ZERO, WeightedList.of());
        NeoForge.EVENT_BUS.post(passive);
        h.assertTrue(passive.getSpawnerDataList().isEmpty(), "passive spawn list unchanged");
        record.setSymbols(List.of("mystcraft:TerrainVoid"));
        h.assertTrue(!DimensionalMonsterSpawns.enabled(level), "Age without page excludes monsters");
        var ordinary = new LevelEvent.PotentialSpawns(level, MobCategory.MONSTER, BlockPos.ZERO, list);
        NeoForge.EVENT_BUS.post(ordinary);
        h.assertValueEqual(ordinary.getSpawnerDataList().size(), 1, "ordinary Age spawn list unchanged");
        h.assertTrue(!DimensionalMonsterSpawns.canSpawn(MystEntities.DIMENSIONAL_SERPENT.get(), server.overworld(),
                EntitySpawnReason.NATURAL, new BlockPos(0, 64, 0), server.overworld().getRandom()), "natural spawning cannot escape into Overworld");
        record.setSymbols(List.of("mystcraft:TerrainVoid", symbol));
        var spawn = new BlockPos(8, 64, 8);
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -1; y <= 5; y++) {
            boolean shell = Math.abs(x) == 4 || Math.abs(z) == 4 || y == -1 || y == 5;
            level.setBlock(spawn.offset(x, y, z), (shell ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
        }
        level.setChunkForced(0, 0, true);
        h.startSequence().thenWaitUntil(() -> h.assertValueEqual(level.getBrightness(net.minecraft.world.level.LightLayer.SKY, spawn), 0,
                "chamber skylight must finish propagating"))
            .thenExecute(() -> {
                h.assertTrue(SpawnPlacements.checkSpawnRules(MystEntities.DIMENSIONAL_SERPENT.get(), level, EntitySpawnReason.NATURAL,
                        spawn, net.minecraft.util.RandomSource.create(1)), "registered serpent predicate accepts a dark Age chamber");
                h.assertTrue(SpawnPlacements.checkSpawnRules(MystEntities.DIMENSIONAL_PTEROSAUR.get(), level, EntitySpawnReason.NATURAL,
                        spawn, net.minecraft.util.RandomSource.create(1)), "registered pterosaur predicate accepts a dark Age chamber");
                level.setBlockAndUpdate(spawn.east(), Blocks.TORCH.defaultBlockState());
            })
            .thenWaitUntil(() -> h.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, spawn) > 0,
                    "torch light must finish propagating"))
            .thenExecute(() -> {
                h.assertTrue(!SpawnPlacements.checkSpawnRules(MystEntities.DIMENSIONAL_SERPENT.get(), level, EntitySpawnReason.NATURAL,
                        spawn, net.minecraft.util.RandomSource.create(1)), "torch prevents serpent spawning");
                h.assertTrue(!SpawnPlacements.checkSpawnRules(MystEntities.DIMENSIONAL_PTEROSAUR.get(), level, EntitySpawnReason.NATURAL,
                        spawn, net.minecraft.util.RandomSource.create(1)), "torch prevents pterosaur spawning");
                level.setChunkForced(0, 0, false);
            }).thenSucceed();
    }
    private static void combat(GameTestHelper h) {
        var level = h.getLevel();
        var p = MystEntities.DIMENSIONAL_PTEROSAUR.get().create(level, EntitySpawnReason.TRIGGERED);
        var s = MystEntities.DIMENSIONAL_SERPENT.get().create(level, EntitySpawnReason.TRIGGERED);
        h.assertValueEqual(p.getMaxHealth(), 40F, "pterosaur HP");
        h.assertValueEqual(p.getAttributeValue(Attributes.ATTACK_DAMAGE), 6D, "pterosaur attack");
        h.assertValueEqual(p.getArmorValue(), 4, "pterosaur armor");
        h.assertValueEqual(s.getMaxHealth(), 30F, "serpent HP");
        h.assertValueEqual(s.getAttributeValue(Attributes.ATTACK_DAMAGE), 6D, "serpent attack");
        h.assertValueEqual(s.getArmorValue(), 0, "serpent armor");
        var victim = EntityTypes.COW.create(level, EntitySpawnReason.TRIGGERED);
        h.assertTrue(s.doHurtTarget(level, victim), "serpent hit accepted");
        h.assertValueEqual(victim.getHealth(), victim.getMaxHealth() - 6F, "six direct damage");
        h.assertTrue(victim.hasEffect(MobEffects.POISON), "poison applied");
        h.assertValueEqual(victim.getEffect(MobEffects.POISON).getDuration(), 200, "ten-second poison");
        h.assertValueEqual(victim.getEffect(MobEffects.POISON).getAmplifier(), 0, "poison I");
        h.succeed();
    }
    private static net.minecraft.world.entity.animal.cow.Cow victim(GameTestHelper h, BlockPos pos) {
        var cow = EntityTypes.COW.create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        cow.setNoAi(true); cow.setPersistenceRequired();
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
        cow.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); h.getLevel().addFreshEntity(cow); return cow;
    }
    private static void pterosaur(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(2, 8, 2)); var cow = victim(h, center);
        cow.setNoGravity(true);
        var p = MystEntities.DIMENSIONAL_PTEROSAUR.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        p.setPersistenceRequired(); p.setPos(cow.getX() + 6, cow.getY() + 4, cow.getZ());
        p.setTarget(cow); h.getLevel().addFreshEntity(p);
        boolean[] seen = new boolean[3];
        h.onEachTick(() -> {
            if (p.attackPhase() == DimensionalPterosaur.WINDUP) {
                seen[0] = true;
                h.assertTrue(Math.abs(p.getY() - cow.getY() - cow.getBbHeight() * .65) < 1.3, "align altitude before windup");
            }
            if (p.attackPhase() == DimensionalPterosaur.CHARGE) seen[1] = true;
            if (p.attackPhase() == DimensionalPterosaur.RECOVER) seen[2] = true;
        });
        h.succeedWhen(() -> {
            h.assertTrue(seen[0] && seen[1] && seen[2] && cow.getHealth() < 100, "windup, charge and recovery must hit a live target");
            p.discard(); cow.discard();
        });
    }
    private static void serpent(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(2, 18, 2));
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++)
            h.getLevel().setBlockAndUpdate(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        var cow = victim(h, center);
        var s = MystEntities.DIMENSIONAL_SERPENT.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        s.setPersistenceRequired(); s.setPos(cow.getX() + 4, cow.getY(), cow.getZ()); s.setTarget(cow);
        h.getLevel().addFreshEntity(s);
        boolean[] airborne = new boolean[1];
        h.onEachTick(() -> { if (s.isPouncing() && s.getY() > center.getY() + .15) airborne[0] = true; });
        h.succeedWhen(() -> {
            h.assertTrue(airborne[0] && cow.hasEffect(MobEffects.POISON), "ground pursuit must leap and poison a live target");
            s.discard(); cow.discard();
        });
    }
}
