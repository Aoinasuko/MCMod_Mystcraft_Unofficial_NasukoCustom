package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.entity.monster.*;
import com.xcompwiz.mystcraft.registry.MystEntities;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Actual AI, block collision, damage, effect, explosion and summon regressions in the 26.3 server. */
final class DimensionalReinforcementGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, "mystcraft");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    private static final Map<net.minecraft.server.level.ServerLevel, Set<net.minecraft.world.level.ChunkPos>> FORCED = new IdentityHashMap<>();
    static {
        TESTS.put("reinforcement_attributes", DimensionalReinforcementGameTests::attributes);
        TESTS.put("fairy_laser_cycle", DimensionalReinforcementGameTests::fairy);
        TESTS.put("worm_charge", h -> worm(h, false));
        TESTS.put("worm_void_barrier", h -> worm(h, true));
        TESTS.put("calamity_volley", DimensionalReinforcementGameTests::volley);
        TESTS.put("calamity_summoning", DimensionalReinforcementGameTests::summoning);
        TESTS.put("calamity_explosion", DimensionalReinforcementGameTests::explosion);
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("mystcraft", name); }
    static void register(IEventBus bus) {
        TESTS.forEach((name, fn) -> FUNCTIONS.register(name, () -> h -> { retainArena(h); fn.accept(h); })); FUNCTIONS.register(bus);
        bus.addListener(DimensionalReinforcementGameTests::tests);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DimensionalReinforcementGameTests::releaseArenas);
    }
    static void retainArena(GameTestHelper h) {
        var level = h.getLevel(); var chunk = net.minecraft.world.level.ChunkPos.containing(h.absolutePos(BlockPos.ZERO));
        var forced = FORCED.computeIfAbsent(level, unused -> new HashSet<>());
        for (int x=-2; x<=2; x++) for (int z=-2; z<=2; z++) {
            var pos = new net.minecraft.world.level.ChunkPos(chunk.x()+x, chunk.z()+z);
            if (!level.getForceLoadedChunks().contains(pos.pack())) { level.setChunkForced(pos.x(), pos.z(), true); forced.add(pos); }
        }
    }
    private static void releaseArenas(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        FORCED.forEach((level, chunks) -> chunks.forEach(pos -> level.setChunkForced(pos.x(), pos.z(), false)));
        FORCED.clear();
    }
    private static void tests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(id("reinforcement_validation"), new TestEnvironmentDefinition.SetDifficulty(Difficulty.NORMAL));
        for (String name : TESTS.keySet()) event.registerTest(id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(name)), new TestData<>(env, id("port_empty"), 500, 0, true)));
    }
    private static void attributes(GameTestHelper h) {
        var f = MystEntities.DIMENSIONAL_FAIRY.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        var w = MystEntities.DIMENSIONAL_WORM.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        var c = MystEntities.DIMENSIONAL_CALAMITY.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        check(h, f, 40, 6, 2); check(h, w, 60, 8, 6); check(h, c, 100, 10, 10);
        var beam = MystEntities.DIMENSIONAL_LASER.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        h.assertTrue(beam.shouldRenderAtSqrDistance(64 * 64), "beam stays visible beyond its tiny physical hitbox");
        h.assertValueEqual(c.getBbHeight(), 3F, "three-metre demon"); h.succeed();
    }
    private static void check(GameTestHelper h, Monster mob, float hp, double damage, int armor) {
        h.assertValueEqual(mob.getMaxHealth(), hp, "HP");
        h.assertValueEqual(mob.getAttributeValue(Attributes.ATTACK_DAMAGE), damage, "attack");
        h.assertValueEqual(mob.getArmorValue(), armor, "armor");
    }
    private static net.minecraft.world.entity.animal.cow.Cow victim(GameTestHelper h, Vec3 pos) {
        var cow = EntityTypes.COW.create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        cow.setNoAi(true); cow.setNoGravity(true); cow.setPersistenceRequired();
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100); cow.setPos(pos);
        h.getLevel().addFreshEntity(cow); return cow;
    }
    private static void fairy(GameTestHelper h) {
        var level = h.getLevel(); var center = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 35, 2)));
        var cow = victim(h, center);
        var fairy = MystEntities.DIMENSIONAL_FAIRY.get().create(level, EntitySpawnReason.TRIGGERED);
        fairy.setPos(center.add(4, 0, 0)); fairy.setPersistenceRequired(); fairy.setTarget(cow); level.addFreshEntity(fairy);
        Vec3[] firstTeleport = new Vec3[1];
        h.startSequence().thenWaitUntil(() -> h.assertValueEqual(fairy.attackPhase(), 1, "teleport starts charge"))
            .thenExecute(() -> {
                firstTeleport[0] = fairy.position();
                var middle = BlockPos.containing(fairy.position().lerp(cow.position(), .5));
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -2; y <= 3; y++)
                    level.setBlock(middle.offset(x,y,z), Blocks.STONE.defaultBlockState(), 2);
                h.assertTrue(!level.clip(new net.minecraft.world.level.ClipContext(fairy.position().add(0,.65,0), cow.getBoundingBox().getCenter(),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, fairy)).getType().equals(HitResult.Type.MISS), "solid wall blocks ordinary raycast");
            }).thenIdle(95).thenExecute(() -> h.assertValueEqual(cow.getHealth(), 100F, "no laser before five-second windup"))
            .thenWaitUntil(() -> h.assertTrue(cow.hasEffect(MobEffects.LEVITATION), "laser hits through wall: phase=" + fairy.attackPhase() + ", target=" + (fairy.getTarget() == cow) + ", hp=" + cow.getHealth() + ", alive=" + cow.isAlive() + ", removed=" + fairy.isRemoved() + ", fairyPos=" + fairy.position() + ", cowPos=" + cow.position()))
            .thenExecute(() -> {
                h.assertValueEqual(cow.getHealth(), 94F, "laser damage is six");
                h.assertValueEqual(cow.getEffect(MobEffects.LEVITATION).getAmplifier(), 2, "levitation III");
                h.assertTrue(cow.getEffect(MobEffects.LEVITATION).getDuration() >= 58, "three-second levitation");
            }).thenIdle(30).thenExecute(() -> h.assertTrue(fairy.position().distanceToSqr(firstTeleport[0]) < .01, "two-second recovery holds position"))
            .thenWaitUntil(() -> h.assertTrue(fairy.position().distanceToSqr(firstTeleport[0]) > 1, "next cycle teleports again"))
            .thenExecute(() -> { fairy.discard(); cow.discard(); }).thenSucceed();
    }
    private static void worm(GameTestHelper h, boolean gap) {
        var level = h.getLevel(); var center = h.absolutePos(new BlockPos(2, gap ? 62 : 52, 2));
        for (int x = -12; x <= 12; x++) for (int z = -5; z <= 5; z++) {
            boolean hole = gap && x >= 0 && x <= 2;
            // Explicit air column prevents underlying flat-world terrain from bridging the test trench.
            for (int y = -5; y < 0; y++) level.setBlock(center.offset(x,y,z), hole || y < -1 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
        }
        var cow = victim(h, Vec3.atBottomCenterOf(center.offset(gap ? 7 : 3, 0, 0)));
        var worm = MystEntities.DIMENSIONAL_WORM.get().create(level, EntitySpawnReason.TRIGGERED);
        worm.setPos(Vec3.atBottomCenterOf(center.offset(-4,0,0))); worm.setPersistenceRequired(); worm.setTarget(cow); level.addFreshEntity(worm);
        h.assertTrue(!worm.hurtServer(level, worm.damageSources().inWall(), 10), "worm rejects suffocation");
        h.assertValueEqual(worm.getHealth(), 60F, "suffocation preserves HP");
        if (gap) {
            h.assertTrue(!worm.canTraverse(worm.position(), cow.position()), "swept path cannot cross groundless trench");
            h.runAfterDelay(150, () -> {
                h.assertTrue(worm.getX() < center.getX(), "charging worm stops before unsupported ground");
                h.assertValueEqual(cow.getHealth(), 100F, "worm cannot attack across the gap");
                worm.discard(); cow.discard(); h.succeed();
            });
        } else h.succeedWhen(() -> {
            h.assertValueEqual(cow.getHealth(), 92F, "burrowing charge deals eight damage");
            h.assertTrue(worm.getY() < center.getY(), "worm is actually embedded in ground");
            worm.discard(); cow.discard();
        });
    }
    private static void volley(GameTestHelper h) {
        var level = h.getLevel(); var center = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 78, 2)));
        var cow = victim(h, center);
        var caster = MystEntities.DIMENSIONAL_CALAMITY.get().create(level, EntitySpawnReason.TRIGGERED);
        caster.setPos(center.add(11, 5, 0)); caster.setPersistenceRequired(); caster.setTarget(cow); level.addFreshEntity(caster);
        h.succeedWhen(() -> {
            var orbs = level.getEntitiesOfClass(CalamityOrb.class, caster.getBoundingBox().inflate(24), e -> e.getOwner() == caster);
            h.assertValueEqual(orbs.size(), 5, "AI fires exactly five explosive projectiles");
            h.assertValueEqual(orbs.stream().map(e -> e.getDeltaMovement().normalize()).distinct().count(), 5L, "five separate radial directions");
            orbs.forEach(Entity::discard); caster.discard(); cow.discard();
        });
    }
    private static void summoning(GameTestHelper h) {
        var level = h.getLevel(); var center = h.absolutePos(new BlockPos(2, 110, 2));
        for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) level.setBlock(center.offset(x,-1,z), Blocks.STONE.defaultBlockState(), 2);
        var caster = MystEntities.DIMENSIONAL_CALAMITY.get().create(level, EntitySpawnReason.TRIGGERED);
        caster.setPos(Vec3.atBottomCenterOf(center.above(3))); caster.setNoAi(true); caster.setPersistenceRequired(); level.addFreshEntity(caster);
        for (int i = 0; i < 5; i++) {
            var s = MystEntities.DIMENSIONAL_SERPENT.get().create(level, EntitySpawnReason.TRIGGERED);
            s.setPos(Vec3.atBottomCenterOf(center.offset(i-3,0,2))); s.setNoAi(true); s.setPersistenceRequired(); level.addFreshEntity(s);
        }
        h.assertValueEqual(caster.nearbyAllies(), 5, "five allies present");
        h.assertValueEqual(caster.beginSummoning(), 3, "five or fewer allies permits three separated circles");
        var area = caster.getBoundingBox().inflate(24);
        h.startSequence().thenIdle(20).thenExecute(() -> h.assertValueEqual(caster.nearbyAllies(), 5, "telegraph precedes spawning"))
            .thenWaitUntil(() -> h.assertValueEqual(caster.nearbyAllies(), 8, "three circles complete into three non-calamity allies"))
            .thenExecute(() -> {
                h.assertValueEqual(level.getEntitiesOfClass(DimensionalCalamity.class, area).size(), 1, "no recursive calamity summons");
                h.assertValueEqual(caster.beginSummoning(), 0, "more than five allies suppresses summoning");
                level.getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof DimensionalMonster && e != caster).forEach(Entity::discard);
                h.assertValueEqual(caster.beginSummoning(), 3, "empty field permits another wave"); caster.discard();
            }).thenIdle(3).thenExecute(() -> h.assertTrue(level.getEntitiesOfClass(DimensionalSummoningCircle.class, area).isEmpty(), "caster death cancels pending circles"))
            .thenSucceed();
    }
    private static void explosion(GameTestHelper h) {
        var level = h.getLevel(); var center = h.absolutePos(new BlockPos(2, 145, 2));
        boolean old = level.getGameRules().get(GameRules.MOB_GRIEFING);
        var cow = victim(h, Vec3.atBottomCenterOf(center.east(2)));
        try {
            level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
            dirt(level, center);
            var orb = MystEntities.CALAMITY_ORB.get().create(level, EntitySpawnReason.TRIGGERED);
            orb.setPos(center.getX()+.5, center.getY()+.25, center.getZ()+.5); level.addFreshEntity(orb); orb.detonate();
            h.assertTrue(cow.getHealth() >= 90 && cow.getHealth() < 100, "explosion damage never exceeds ten");
            h.assertTrue(countDirt(level, center) < 49, "radius-three mob explosion destroys dirt");
            level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer()); dirt(level, center);
            var safe = MystEntities.CALAMITY_ORB.get().create(level, EntitySpawnReason.TRIGGERED);
            safe.setPos(orb.position()); level.addFreshEntity(safe); safe.detonate();
            h.assertValueEqual(countDirt(level, center), 49, "mobGriefing false protects terrain");
        } finally { level.getGameRules().set(GameRules.MOB_GRIEFING, old, level.getServer()); cow.discard(); }
        h.succeed();
    }
    private static void dirt(net.minecraft.server.level.ServerLevel level, BlockPos center) {
        for (int x=-3; x<=3; x++) for(int z=-3; z<=3; z++) level.setBlock(center.offset(x,-1,z), Blocks.DIRT.defaultBlockState(), 2);
    }
    private static int countDirt(net.minecraft.server.level.ServerLevel level, BlockPos center) {
        int count=0;
        for (int x=-3; x<=3; x++) for(int z=-3; z<=3; z++) if(level.getBlockState(center.offset(x,-1,z)).is(Blocks.DIRT)) count++;
        return count;
    }
}
