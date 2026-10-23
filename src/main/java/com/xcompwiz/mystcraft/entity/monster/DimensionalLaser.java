package com.xcompwiz.mystcraft.entity.monster;

import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Short-lived synchronized visual; damage is applied once by its fairy at firing time. */
public final class DimensionalLaser extends Entity {
    private static final EntityDataAccessor<org.joml.Vector3fc> END = SynchedEntityData.defineId(DimensionalLaser.class, EntityDataSerializers.VECTOR3);
    public DimensionalLaser(EntityType<? extends DimensionalLaser> type, Level level) { super(type, level); setNoGravity(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(END, new Vector3f()); }
    public void setEnd(Vec3 end) { entityData.set(END, end.subtract(position()).toVector3f()); }
    public Vec3 endOffset() { return new Vec3(entityData.get(END)); }
    // A tiny effect hitbox must not limit this 48-block beam to vanilla's ~6-block draw range.
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
    @Override public MoveSimulationType getMoveSimulationType() { return MoveSimulationType.SERVER_AND_CLIENT; }
    @Override public void tick() { super.tick(); if (!level().isClientSide() && tickCount >= 8) discard(); }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) {}
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
