package com.xcompwiz.mystcraft.entity.monster;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;

/** A visible two-second telegraph; a dead caster cannot finish the summon. */
public final class DimensionalSummoningCircle extends Entity {
    private UUID caster;
    private int species;
    public DimensionalSummoningCircle(EntityType<? extends DimensionalSummoningCircle> type, Level level) { super(type, level); setNoGravity(true); }
    public void configure(DimensionalCalamity caster, int species) { this.caster = caster.getUUID(); this.species = species; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public MoveSimulationType getMoveSimulationType() { return MoveSimulationType.SERVER_AND_CLIENT; }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (caster == null || !(server.getEntity(caster) instanceof DimensionalCalamity owner) || !owner.isAlive()) { discard(); return; }
            if (tickCount % 4 == 0) server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + .1, getZ(), 3, .6, .05, .6, .1);
            if (tickCount >= 40) { owner.finishSummon(position(), species); discard(); }
        }
    }
    @Override public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
