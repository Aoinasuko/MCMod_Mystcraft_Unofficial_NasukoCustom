package com.xcompwiz.mystcraft.entity.monster;

import java.util.EnumSet;
import com.xcompwiz.mystcraft.registry.MystEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Once sighted, the target remains tracked through walls throughout the 100/40-tick cycle. */
public final class DimensionalFairy extends FlyingDimensionalMonster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DimensionalFairy.class, EntityDataSerializers.INT);
    private int phaseTicks;
    public DimensionalFairy(EntityType<? extends DimensionalFairy> type, Level level) { super(type, level); xpReward = 12; }
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.ARMOR, 2).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.MOVEMENT_SPEED, .25);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(PHASE, 0); }
    public int attackPhase() { return entityData.get(PHASE); }
    @Override protected void registerGoals() {
        flyingTargets(true);
        goalSelector.addGoal(0, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return true; }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() {
                var target = getTarget();
                if (target == null || target.level() != level() || !target.isAlive() || distanceToSqr(target) > 64 * 64) {
                    entityData.set(PHASE, 0); phaseTicks = 0; idleFlight(); return;
                }
                if (attackPhase() == 0 || (attackPhase() == 2 && phaseTicks >= 40)) {
                    teleportNear(target); entityData.set(PHASE, 1); phaseTicks = 0;
                } else if (attackPhase() == 1 && phaseTicks >= 100) {
                    fireLaser(target); entityData.set(PHASE, 2); phaseTicks = 0;
                }
                setDeltaMovement(getDeltaMovement().scale(.5)); face(target.getEyePosition());
                if (attackPhase() == 1 && phaseTicks >= 80 && level() instanceof ServerLevel server && tickCount % 4 == 0)
                    server.sendParticles(ParticleTypes.WITCH, getX(), getY() + .6, getZ(), 2, .2, .2, .2, 0);
                phaseTicks++;
            }
        });
    }
    public boolean teleportNear(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) return false;
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2, radius = 6 + random.nextDouble() * 4;
            Vec3 point = target.position().add(Math.cos(angle) * radius, 1.5 + random.nextDouble() * 2, Math.sin(angle) * radius);
            var pos = net.minecraft.core.BlockPos.containing(point);
            var box = getBoundingBox().move(point.subtract(position()));
            if (!server.hasChunkAt(pos) || !server.getWorldBorder().isWithinBounds(pos) || !server.noCollision(this, box)
                    || server.containsAnyLiquid(box) || point.y < server.getMinY() || point.y + getBbHeight() > server.getMaxY()) continue;
            server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + .5, getZ(), 20, .3, .4, .3, .1);
            teleportTo(point.x, point.y, point.z); setDeltaMovement(Vec3.ZERO);
            playSound(SoundEvents.ENDERMAN_TELEPORT, .8F, 1.5F); return true;
        }
        return false;
    }
    public void fireLaser(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 start = position().add(0, .65, 0);
        Vec3 direction = target.getBoundingBox().getCenter().subtract(start).normalize();
        Vec3 end = start.add(direction.scale(48));
        var laser = MystEntities.DIMENSIONAL_LASER.get().create(server, EntitySpawnReason.MOB_SUMMONED);
        laser.setPos(start); laser.setEnd(end); server.addFreshEntity(laser);
        // Deliberately no block raycast: solid walls cannot stop this attack.
        for (var victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(.3),
                e -> e != this && !(e instanceof DimensionalMonster) && e.isAlive() && !e.isSpectator())) {
            if ((victim.getBoundingBox().inflate(.18).contains(start) || victim.getBoundingBox().inflate(.18).clip(start, end).isPresent())
                    && victim.hurtServer(server, damageSources().indirectMagic(this, this), (float)getAttributeValue(Attributes.ATTACK_DAMAGE)))
                victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 2), this);
        }
        playSound(SoundEvents.GUARDIAN_ATTACK, 1, 1.6F);
    }
}
