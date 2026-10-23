package com.xcompwiz.mystcraft.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Phases into solid terrain, but every swept movement step must have continuous solid support. */
public final class DimensionalWorm extends Monster implements DimensionalMonster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DimensionalWorm.class, EntityDataSerializers.INT);
    private int timer, cooldown;
    private Vec3 charge = Vec3.ZERO;
    public DimensionalWorm(EntityType<? extends DimensionalWorm> type, Level level) {
        super(type, level); noPhysics = true; setNoGravity(true); xpReward = 16;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.ARMOR, 6).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.MOVEMENT_SPEED, .25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(PHASE, 0); }
    public int attackPhase() { return entityData.get(PHASE); }
    private void phase(int phase) { entityData.set(PHASE, phase); timer = 0; }
    @Override protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Ground hunter; once acquired, its target need not remain in sight.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        goalSelector.addGoal(0, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return true; }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() {
                timer++; if (cooldown > 0) cooldown--;
                var target = getTarget();
                if (target == null || target.level() != level() || !target.isAlive() || distanceToSqr(target) > 48 * 48) {
                    phase(0); setDeltaMovement(Vec3.ZERO); return;
                }
                var delta = target.position().subtract(position()).multiply(1, 0, 1);
                var direction = delta.normalize();
                if (attackPhase() != 2) {
                    setYRot((float)(Mth.atan2(delta.z, delta.x) * 180 / Math.PI) - 90);
                    yBodyRot = getYRot(); setYHeadRot(getYRot());
                }
                if (attackPhase() == 0) {
                    setDeltaMovement(direction.scale(.15));
                    if (cooldown == 0 && delta.lengthSqr() <= 14 * 14) { phase(1); playSound(SoundEvents.RAVAGER_ROAR, .7F, .65F); }
                } else if (attackPhase() == 1) {
                    setDeltaMovement(Vec3.ZERO);
                    if (timer >= 20) { charge = direction.scale(.7); phase(2); }
                } else {
                    setDeltaMovement(charge);
                    if (getBoundingBox().inflate(.2).intersects(target.getBoundingBox()) && level() instanceof ServerLevel server) {
                        doHurtTarget(server, target); phase(0); cooldown = 45; setDeltaMovement(Vec3.ZERO);
                    } else if (timer >= 26) { phase(0); cooldown = 45; }
                }
            }
        });
    }
    /** Surface near this worm, including when its feet are embedded 0.55 blocks into it. */
    public double supportTop(double x, double z, double referenceY) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        for (int y = Mth.floor(referenceY + 1.5); y >= Mth.floor(referenceY - 2); y--) {
            var p = new BlockPos(bx, y, bz);
            if (!level().hasChunkAt(p) || !level().getWorldBorder().isWithinBounds(p)) return Double.NaN;
            var state = level().getBlockState(p);
            if (state.getFluidState().isEmpty() && !state.getCollisionShape(level(), p).isEmpty()
                    && level().getBlockState(p.above()).getCollisionShape(level(), p.above()).isEmpty()) return y + 1;
        }
        return Double.NaN;
    }
    public boolean canTraverse(Vec3 from, Vec3 to) {
        double top = supportTop(from.x, from.z, from.y);
        if (Double.isNaN(top)) return false;
        int steps = Math.max(1, (int)Math.ceil(from.distanceTo(to) / .2));
        for (int i = 1; i <= steps; i++) {
            var p = from.lerp(to, (double)i / steps);
            double next = supportTop(p.x, p.z, top - .55);
            if (Double.isNaN(next) || Math.abs(next - top) > 1.05) return false;
            for (double dx : new double[]{-.45, .45}) for (double dz : new double[]{-.45, .45}) {
                double edge = supportTop(p.x + dx, p.z + dz, next - .55);
                if (Double.isNaN(edge) || Math.abs(edge - next) > 1.05) return false;
            }
            top = next;
        }
        return true;
    }
    @Override public void travel(Vec3 input) {
        if (level().isClientSide()) return; // Native entity interpolation supplies the client position.
        var next = position().add(getDeltaMovement().multiply(1, 0, 1));
        if (canTraverse(position(), next)) {
            double top = supportTop(next.x, next.z, getY());
            setPos(next.x, top - .55, next.z);
            if (level() instanceof ServerLevel server && tickCount % 4 == 0 && getDeltaMovement().horizontalDistanceSqr() > .001) {
                var ground = level().getBlockState(BlockPos.containing(next.x, top - .1, next.z));
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), next.x, top + .05, next.z, 4, .6, .1, .6, .05);
            }
        } else { setDeltaMovement(Vec3.ZERO); if (attackPhase() == 2) { phase(0); cooldown = 35; } }
    }
    @Override public boolean isInWall() { return false; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return !source.is(DamageTypes.IN_WALL) && super.hurtServer(level, source, amount);
    }
    @Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }
}
