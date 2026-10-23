package com.xcompwiz.mystcraft.entity.monster;

import java.util.*;
import com.xcompwiz.mystcraft.registry.MystEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Rare three-metre flying demon: a five-way explosive volley and telegraphed reinforcements. */
public final class DimensionalCalamity extends FlyingDimensionalMonster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DimensionalCalamity.class, EntityDataSerializers.INT);
    private int attackTicks, summonCooldown = 80;
    public DimensionalCalamity(EntityType<? extends DimensionalCalamity> type, Level level) { super(type, level); xpReward = 35; }
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.ARMOR, 10).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.MOVEMENT_SPEED, .28)
                .add(Attributes.KNOCKBACK_RESISTANCE, .75);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(PHASE, 0); }
    public int attackPhase() { return entityData.get(PHASE); }
    @Override protected void registerGoals() {
        flyingTargets(false);
        goalSelector.addGoal(0, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return true; }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() {
                var target = getTarget();
                if (target == null || target.level() != level() || !target.isAlive() || distanceToSqr(target) > 64 * 64) {
                    entityData.set(PHASE, 0); attackTicks = 0; idleFlight(); return;
                }
                var away = position().subtract(target.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < .01) away = new Vec3(1, 0, 0);
                steer(target.position().add(away.normalize().scale(11)).add(0, 5, 0), .22);
                face(target.getEyePosition()); attackTicks++;
                entityData.set(PHASE, attackTicks <= 40 ? 1 : 0);
                if (attackTicks == 40) fireVolley(target);
                if (attackTicks >= 140) attackTicks = 0;
                if (--summonCooldown <= 0) { beginSummoning(); summonCooldown = 300; }
            }
        });
    }
    public void fireVolley(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) return;
        var start = position().add(0, 1.7, 0);
        var base = target.getBoundingBox().getCenter().subtract(start).normalize();
        for (int i = -2; i <= 2; i++) {
            var orb = MystEntities.CALAMITY_ORB.get().create(server, EntitySpawnReason.MOB_SUMMONED);
            orb.setOwner(this); orb.setPos(start); orb.setDeltaMovement(base.yRot((float)Math.toRadians(i * 12)).scale(.65));
            server.addFreshEntity(orb);
        }
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, .65F);
    }
    public int nearbyAllies() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(24),
                e -> e.isAlive() && e instanceof DimensionalMonster && !(e instanceof DimensionalCalamity)).size();
    }
    public int beginSummoning() {
        if (!(level() instanceof ServerLevel server) || server.getDifficulty() == Difficulty.PEACEFUL || nearbyAllies() > 5) return 0;
        // Avoid overlapping waves from this caster; each circle can complete exactly once.
        if (!server.getEntitiesOfClass(DimensionalSummoningCircle.class, getBoundingBox().inflate(24)).isEmpty()) return 0;
        int count = 0; double baseAngle = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 3; i++) {
            double angle = baseAngle + i * Math.PI * 2 / 3 + random.nextDouble() * .2, radius = 5 + random.nextDouble() * 3;
            var center = getTarget() == null ? position() : getTarget().position();
            int x = net.minecraft.util.Mth.floor(center.x + Math.cos(angle) * radius), z = net.minecraft.util.Mth.floor(center.z + Math.sin(angle) * radius);
            for (int y = net.minecraft.util.Mth.floor(center.y + 5); y >= center.y - 12; y--) {
                var floor = new BlockPos(x, y, z);
                if (!server.hasChunkAt(floor) || !server.getWorldBorder().isWithinBounds(floor)) break;
                var state = server.getBlockState(floor);
                if (state.getFluidState().isEmpty() && state.isSolidRender() && server.getBlockState(floor.above()).isAir()
                        && server.getBlockState(floor.above(2)).isAir()) {
                    var circle = MystEntities.SUMMONING_CIRCLE.get().create(server, EntitySpawnReason.MOB_SUMMONED);
                    circle.setPos(x + .5, y + 1.03, z + .5); circle.configure(DimensionalCalamity.this, random.nextInt(4));
                    server.addFreshEntity(circle); count++; break;
                }
            }
        }
        if (count > 0) playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.2F, .6F);
        return count;
    }
    public void finishSummon(Vec3 point, int species) {
        if (!(level() instanceof ServerLevel server) || !isAlive() || server.getDifficulty() == Difficulty.PEACEFUL || nearbyAllies() >= 8) return;
        var type = switch (species) {
            case 0 -> MystEntities.DIMENSIONAL_PTEROSAUR.get();
            case 1 -> MystEntities.DIMENSIONAL_SERPENT.get();
            case 2 -> MystEntities.DIMENSIONAL_FAIRY.get();
            default -> MystEntities.DIMENSIONAL_WORM.get();
        };
        var mob = type.create(server, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return;
        mob.setPos(point);
        if (!server.noCollision(mob, mob.getBoundingBox()) || server.containsAnyLiquid(mob.getBoundingBox())
                || server.getBlockState(BlockPos.containing(point).below()).getCollisionShape(server, BlockPos.containing(point).below()).isEmpty()) return;
        mob.finalizeSpawn(server, server.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        mob.setTarget(getTarget()); server.addFreshEntity(mob);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y + .5, point.z, 20, .4, .5, .4, .05);
    }
}
