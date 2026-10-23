package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherRuntimeState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;

import java.util.Objects;

/**
 * Per-Age mutable level data used by runtime-created Mystcraft worlds.
 *
 * <p>Vanilla's ordinary secondary dimensions use {@link DerivedLevelData}, which
 * delegates most state to the Overworld. Mystcraft Ages historically own their
 * spawn and clock, so the values that must not leak into the Overworld are kept
 * locally here. Weather is also detached now so ticking a runtime Age cannot
 * mutate Overworld weather.</p>
 *
 * <p>This object is runtime state. Durable Mystcraft compatibility state remains
 * in {@link AgeRecord}; later lifecycle checkpoints copy these values back to
 * AgeSavedData on save/unload.</p>
 */
public final class MystcraftAgeLevelData extends DerivedLevelData {
    private final String levelName;
    private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
    private final net.minecraft.world.level.saveddata.WeatherData weather = new net.minecraft.world.level.saveddata.WeatherData();

    private BlockPos spawnPos;
    private float spawnAngle;
    private long gameTime;
    private long dayTime;
    private int clearWeatherTime;
    private int thunderTime;
    private int rainTime;
    private boolean thundering;
    private boolean raining;
    private boolean initialized;
    private final AgeWeatherRuntimeState weatherRuntimeState = new AgeWeatherRuntimeState();

    public MystcraftAgeLevelData(
            WorldData worldData,
            ServerLevelData overworldData,
            AgeRecord age,
            BlockPos fallbackSpawn) {
        super(
                Objects.requireNonNull(worldData, "worldData"),
                Objects.requireNonNull(overworldData, "overworldData"));
        Objects.requireNonNull(age, "age");
        this.levelName = age.ageName() == null || age.ageName().isBlank()
                ? "Mystcraft Age " + age.ageUid()
                : age.ageName();
        this.spawnPos = age.spawn() != null ? age.spawn() : Objects.requireNonNull(fallbackSpawn, "fallbackSpawn");
        this.spawnAngle = 0.0F;
        this.dimension = AgeDimensionDefinition.from(age).levelKey();
        this.gameTime = age.worldTime();
        this.dayTime = age.dayTime();
        this.clearWeatherTime = age.clearWeatherTime();
        this.thunderTime = age.thunderTime();
        this.rainTime = age.rainTime();
        this.thundering = age.thundering();
        this.raining = age.raining();
        weather.setClearWeatherTime(clearWeatherTime);
        weather.setRainTime(rainTime);
        weather.setThunderTime(thunderTime);
        weather.setRaining(raining);
        weather.setThundering(thundering);

        // Legacy WeatherControllerBase restored its persisted cycle counters/flags and
        // reconstructed visual strength from the saved raining/thundering booleans.
        // Keep the runtime controller initialized here so the first Age tick resumes
        // that state instead of resetting an already-saved weather cycle to zero.
        this.weatherRuntimeState.setRainCounter(this.rainTime);
        this.weatherRuntimeState.setThunderCounter(this.thunderTime);
        this.weatherRuntimeState.setRaining(this.raining);
        this.weatherRuntimeState.setThundering(this.thundering);
        this.weatherRuntimeState.setRainStrength(this.raining ? 1.0F : 0.0F);
        this.weatherRuntimeState.setThunderStrength(this.thundering ? 1.0F : 0.0F);
        this.weatherRuntimeState.setInitialized(true);

        this.initialized = age.spawn() != null;
    }

    @Override
    public String getLevelName() {
        return levelName;
    }

    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    public float getSpawnAngle() {
        return spawnAngle;
    }

    public void setSpawn(BlockPos pos, float angle) {
        this.spawnPos = Objects.requireNonNull(pos, "pos").immutable();
        this.spawnAngle = angle;
    }

    @Override
    public long getGameTime() {
        return gameTime;
    }

    @Override
    public void setGameTime(long time) {
        this.gameTime = time;
    }

    public long getDayTime() {
        return dayTime;
    }

    public void setDayTime(long time) {
        this.dayTime = time;
    }

    public int getClearWeatherTime() {
        return weather.getClearWeatherTime();
    }

    public void setClearWeatherTime(int time) {
        weather.setClearWeatherTime(time);
    }

    public int getThunderTime() {
        return weather.getThunderTime();
    }

    public void setThunderTime(int time) {
        weather.setThunderTime(time);
    }

    public int getRainTime() {
        return weather.getRainTime();
    }

    public void setRainTime(int time) {
        weather.setRainTime(time);
    }

    public boolean isThundering() {
        return weather.isThundering();
    }

    public void setThundering(boolean value) {
        weather.setThundering(value);
    }

    public boolean isRaining() {
        return weather.isRaining();
    }

    public void setRaining(boolean value) {
        weather.setRaining(value);
    }


    public AgeWeatherRuntimeState weatherRuntimeState() {
        return weatherRuntimeState;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean value) {
        this.initialized = value;
    }

    public net.minecraft.world.level.saveddata.WeatherData weather() { return weather; }

    @Override
    public net.minecraft.world.level.storage.LevelData.RespawnData getRespawnData() {
        return net.minecraft.world.level.storage.LevelData.RespawnData.of(dimension, spawnPos, spawnAngle, 0);
    }

    @Override
    public void setSpawn(net.minecraft.world.level.storage.LevelData.RespawnData spawn) {
        this.spawnPos = spawn.pos();
        this.spawnAngle = spawn.yaw();
    }
}
