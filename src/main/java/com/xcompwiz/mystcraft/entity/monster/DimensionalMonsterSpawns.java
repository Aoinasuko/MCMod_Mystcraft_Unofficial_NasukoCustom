package com.xcompwiz.mystcraft.entity.monster;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.registry.MystEntities;
import com.xcompwiz.mystcraft.world.DimensionalMonsterPolicy;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class DimensionalMonsterSpawns {
    private DimensionalMonsterSpawns() {}
    public static boolean enabled(ServerLevel level) {
        return pageCount(level) > 0;
    }
    public static int pageCount(ServerLevel level) {
        if (!level.dimension().identifier().getNamespace().equals(Mystcraft.MOD_ID)) return 0;
        return AgeManager.resolveByLevel(level.getServer(), level.dimension())
                .map(age -> DimensionalMonsterPolicy.pageCount(age.symbols())).orElse(0);
    }
    public static int requiredPages(EntityType<?> type) {
        if (type == MystEntities.DIMENSIONAL_CALAMITY.get()) return 3;
        return type == MystEntities.DIMENSIONAL_FAIRY.get() || type == MystEntities.DIMENSIONAL_WORM.get() ? 2 : 1;
    }
    private static final class Entries {
        private static final Weighted<MobSpawnSettings.SpawnerData> PTEROSAUR = new Weighted<>(
                new MobSpawnSettings.SpawnerData(MystEntities.DIMENSIONAL_PTEROSAUR.get(), ConstantInt.of(1)), 8);
        private static final Weighted<MobSpawnSettings.SpawnerData> SERPENT = new Weighted<>(
                new MobSpawnSettings.SpawnerData(MystEntities.DIMENSIONAL_SERPENT.get(), UniformInt.of(1, 2)), 12);
        private static final Weighted<MobSpawnSettings.SpawnerData> FAIRY = new Weighted<>(
                new MobSpawnSettings.SpawnerData(MystEntities.DIMENSIONAL_FAIRY.get(), ConstantInt.of(1)), 5);
        private static final Weighted<MobSpawnSettings.SpawnerData> WORM = new Weighted<>(
                new MobSpawnSettings.SpawnerData(MystEntities.DIMENSIONAL_WORM.get(), ConstantInt.of(1)), 6);
        private static final Weighted<MobSpawnSettings.SpawnerData> CALAMITY = new Weighted<>(
                new MobSpawnSettings.SpawnerData(MystEntities.DIMENSIONAL_CALAMITY.get(), ConstantInt.of(1)), 1);
    }
    @SubscribeEvent
    public static void potentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER || !(event.getLevel() instanceof ServerLevel level)
                || level.getDifficulty() == Difficulty.PEACEFUL) return;
        int pages = pageCount(level);
        if (pages == 0) return;
        // Shared stable entries also satisfy NaturalSpawner's repeated membership checks.
        if (event.getSpawnerDataList().stream().noneMatch(e -> e.value().type() == MystEntities.DIMENSIONAL_PTEROSAUR.get()))
            event.addSpawnerData(Entries.PTEROSAUR);
        if (event.getSpawnerDataList().stream().noneMatch(e -> e.value().type() == MystEntities.DIMENSIONAL_SERPENT.get()))
            event.addSpawnerData(Entries.SERPENT);
        if (pages >= 2) { add(event, Entries.FAIRY); add(event, Entries.WORM); }
        if (pages >= 3) add(event, Entries.CALAMITY);
    }
    private static void add(LevelEvent.PotentialSpawns event, Weighted<MobSpawnSettings.SpawnerData> entry) {
        if (event.getSpawnerDataList().stream().noneMatch(e -> e.value().type() == entry.value().type())) event.addSpawnerData(entry);
    }
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(MystEntities.DIMENSIONAL_PTEROSAUR.get(), DimensionalPterosaur.createAttributes().build());
        event.put(MystEntities.DIMENSIONAL_SERPENT.get(), DimensionalSerpent.createAttributes().build());
        event.put(MystEntities.DIMENSIONAL_FAIRY.get(), DimensionalFairy.createAttributes().build());
        event.put(MystEntities.DIMENSIONAL_WORM.get(), DimensionalWorm.createAttributes().build());
        event.put(MystEntities.DIMENSIONAL_CALAMITY.get(), DimensionalCalamity.createAttributes().build());
    }
    public static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(MystEntities.DIMENSIONAL_PTEROSAUR.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DimensionalMonsterSpawns::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MystEntities.DIMENSIONAL_SERPENT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DimensionalMonsterSpawns::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MystEntities.DIMENSIONAL_FAIRY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DimensionalMonsterSpawns::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MystEntities.DIMENSIONAL_WORM.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DimensionalMonsterSpawns::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MystEntities.DIMENSIONAL_CALAMITY.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DimensionalMonsterSpawns::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    public static boolean canSpawn(EntityType<? extends Mob> type, ServerLevelAccessor level,
            EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION)
                && pageCount(level.getLevel()) < requiredPages(type)) return false;
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
