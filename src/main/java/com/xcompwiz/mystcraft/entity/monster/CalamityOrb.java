package com.xcompwiz.mystcraft.entity.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.HitResult;

/** Creeper-sized terrain explosion, with entity damage capped at the requested ten points. */
public final class CalamityOrb extends ThrowableProjectile {
    private int lifetime;
    private boolean detonated;
    private static final ExplosionDamageCalculator DAMAGE = new ExplosionDamageCalculator() {
        @Override public boolean shouldDamageEntity(Explosion explosion, Entity entity) { return !(entity instanceof DimensionalMonster); }
        @Override public float getEntityDamageAmount(Explosion explosion, Entity entity, float exposure) {
            double falloff = Math.max(0, 1 - entity.distanceToSqr(explosion.center()) / 36);
            return (float)(10 * exposure * falloff);
        }
    };
    public CalamityOrb(EntityType<? extends CalamityOrb> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected double getDefaultGravity() { return 0; }
    @Override protected float getAirDrag() { return 1; }
    @Override protected boolean canHitEntity(Entity entity) { return !(entity instanceof DimensionalMonster) && super.canHitEntity(entity); }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (++lifetime > 160 || !server.hasChunkAt(blockPosition())) { discard(); return; }
            server.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 1, .08, .08, .08, 0);
        }
    }
    @Override protected void onHit(HitResult hit) { super.onHit(hit); detonate(); }
    public void detonate() {
        if (detonated || !(level() instanceof ServerLevel server)) return;
        detonated = true;
        server.explode(this, damageSources().explosion(this, getOwner()), DAMAGE, position(), 3, false, Level.ExplosionInteraction.MOB);
        discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) { super.addAdditionalSaveData(output); output.putInt("Lifetime", lifetime); }
    @Override protected void readAdditionalSaveData(ValueInput input) { super.readAdditionalSaveData(input); lifetime = input.getIntOr("Lifetime", 0); }
}
