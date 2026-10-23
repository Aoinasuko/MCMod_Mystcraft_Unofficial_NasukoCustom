package com.xcompwiz.mystcraft.entity.monster;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

abstract class FlyingDimensionalMonster extends Monster implements DimensionalMonster {
    private Vec3 idle;
    protected FlyingDimensionalMonster(EntityType<? extends FlyingDimensionalMonster> type, Level level) {
        super(type, level); setNoGravity(true);
    }
    protected final void flyingTargets(boolean rememberBehindWalls) {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override public boolean canContinueToUse() {
                if (!rememberBehindWalls) return super.canContinueToUse();
                var target = getTarget();
                return target != null && target.level() == level() && target.isAlive() && FlyingDimensionalMonster.this.canAttack(target)
                        && distanceToSqr(target) <= 64 * 64;
            }
        });
    }
    protected final void face(Vec3 point) {
        var delta = point.subtract(position());
        setYRot((float)(Mth.atan2(delta.z, delta.x) * 180 / Math.PI) - 90);
        yBodyRot = getYRot(); setYHeadRot(getYRot());
    }
    protected final void steer(Vec3 point, double speed) {
        var delta = point.subtract(position());
        var desired = delta.lengthSqr() < .02 ? Vec3.ZERO : delta.normalize().scale(speed);
        if (horizontalCollision) desired = desired.add(0, .2, 0);
        setDeltaMovement(getDeltaMovement().lerp(desired, .15)); face(point);
    }
    protected final void idleFlight() {
        if (idle == null || tickCount % 80 == 0 || position().distanceToSqr(idle) < 1)
            idle = position().add(random.nextInt(9) - 4, random.nextDouble() * 2 - .5, random.nextInt(9) - 4);
        steer(idle, .12);
    }
    @Override public void travel(Vec3 input) { travelFlying(input, .2F); }
    @Override public boolean causeFallDamage(double distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) { return false; }
}
