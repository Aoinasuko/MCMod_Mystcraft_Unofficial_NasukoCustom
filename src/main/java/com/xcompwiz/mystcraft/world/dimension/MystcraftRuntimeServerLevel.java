package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Runtime ServerLevel whose world-generation seed is the durable Mystcraft Age seed.
 *
 * <p>Vanilla ServerLevel#getSeed() delegates to the server-wide WorldOptions seed, which is
 * correct for ordinary dimensions because they all share one world seed. Mystcraft Ages do not:
 * each Age persists its own seed. WorldGenRegion snapshots ServerLevel#getSeed() when a generation
 * region is created, so merely seeding the BiomeManager/RandomState is not sufficient.</p>
 *
 * <p>The construction ThreadLocal covers the narrow interval in which the ServerLevel superclass
 * can call virtual getSeed() before this subclass's fields are initialized. Runtime Age creation is
 * server-thread-only, so there is one construction seed per creating thread and no global mutable
 * seed singleton.</p>
 */
public final class MystcraftRuntimeServerLevel extends ServerLevel {
    private static final ThreadLocal<Long> CONSTRUCTION_SEED = new ThreadLocal<>();

    private net.minecraft.world.clock.ServerClockManager ageClocks;
    private long mystcraftAgeSeed;
    private boolean mystcraftAgeSeedReady;

    private MystcraftRuntimeServerLevel(
            MinecraftServer server,
            Executor executor,
            LevelStorageSource.LevelStorageAccess storageAccess,
            ServerLevelData levelData,
            ResourceKey<Level> dimension,
            LevelStem levelStem,
            boolean debug,
            long biomeZoomSeed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            long ageSeed) {
        super(server, executor, storageAccess, levelData, dimension, levelStem, debug, biomeZoomSeed, customSpawners, tickTime);
        this.mystcraftAgeSeed = ageSeed;
        this.mystcraftAgeSeedReady = true;
        var data = (MystcraftAgeLevelData) levelData;
        var ops = registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var states = new java.util.HashMap<net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock>, net.minecraft.world.clock.ClockState>();
        registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK).listElements()
                .forEach(clock -> states.put(clock, new net.minecraft.world.clock.ClockState(data.getDayTime(), 0, 1, false)));
        var encoded = net.minecraft.world.clock.PackedClockStates.CODEC.encodeStart(ops,
                new net.minecraft.world.clock.PackedClockStates(states)).getOrThrow();
        ageClocks = net.minecraft.world.clock.ServerClockManager.TYPE.codec().parse(ops, encoded).getOrThrow();
        ageClocks.init(server);
    }

    public static MystcraftRuntimeServerLevel create(
            MinecraftServer server,
            Executor executor,
            LevelStorageSource.LevelStorageAccess storageAccess,
            ServerLevelData levelData,
            ResourceKey<Level> dimension,
            LevelStem levelStem,
            boolean debug,
            long biomeZoomSeed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            long ageSeed) {
        Objects.requireNonNull(server, "server");
        if (CONSTRUCTION_SEED.get() != null) {
            throw new IllegalStateException("Nested Mystcraft runtime ServerLevel construction");
        }
        CONSTRUCTION_SEED.set(ageSeed);
        try {
            return new MystcraftRuntimeServerLevel(server, executor, storageAccess, levelData,
                    dimension, levelStem, debug, biomeZoomSeed, customSpawners,
                    tickTime, ageSeed);
        } finally {
            CONSTRUCTION_SEED.remove();
        }
    }

    @Override
    public long getSeed() {
        if (mystcraftAgeSeedReady) {
            return mystcraftAgeSeed;
        }
        Long constructing = CONSTRUCTION_SEED.get();
        return constructing != null ? constructing : super.getSeed();
    }

    @Override
    public net.minecraft.world.clock.ServerClockManager clockManager() {
        return ageClocks != null ? ageClocks : super.clockManager();
    }

    @Override
    public net.minecraft.world.level.saveddata.WeatherData getWeatherData() {
        return getLevelData() instanceof MystcraftAgeLevelData data ? data.weather() : super.getWeatherData();
    }

    @Override
    public net.minecraft.world.level.storage.LevelData.RespawnData getRespawnData() {
        return getLevelData().getRespawnData();
    }

    @Override
    public void setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData spawn) {
        ((MystcraftAgeLevelData) getLevelData()).setSpawn(spawn);
    }

    public void tickAgeClock() {
        ageClocks.tick();
        ((MystcraftAgeLevelData) getLevelData()).setDayTime(getDefaultClockTime());
    }
}
