package com.xcompwiz.mystcraft.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Aligns with the player's altitude, visibly winds up, then commits to a straight charge. */
public final class DimensionalPterosaur extends Monster {
    public static final int TRACK = 0, WINDUP = 1, CHARGE = 2, RECOVER = 3;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(DimensionalPterosaur.class, EntityDataSerializers.INT);
    private int phaseTicks;
    private Vec3 chargeVelocity = Vec3.ZERO;
    public DimensionalPterosaur(EntityType<? extends DimensionalPterosaur> type, Level level) {
        super(type, level); setNoGravity(true); xpReward = 10;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.ARMOR, 4).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.MOVEMENT_SPEED, .30);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(PHASE, TRACK);
    }
    public int attackPhase() { return entityData.get(PHASE); }
    private void phase(int value) { entityData.set(PHASE, value); phaseTicks = 0; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FlightAttackGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    @Override public void travel(Vec3 input) { travelFlying(input, .2F); }
    @Override public boolean causeFallDamage(double distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) { return false; }
    private void face(Vec3 point) {
        Vec3 delta = point.subtract(position());
        setYRot((float)(Mth.atan2(delta.z, delta.x) * 180 / Math.PI) - 90);
        yBodyRot = getYRot(); setYHeadRot(getYRot());
        setXRot((float)(-Mth.atan2(delta.y, delta.horizontalDistance()) * 180 / Math.PI));
    }
    private void steer(Vec3 point, double speed) {
        Vec3 delta = point.subtract(position());
        Vec3 desired = delta.lengthSqr() < .01 ? Vec3.ZERO : delta.normalize().scale(speed);
        if (horizontalCollision) desired = desired.add(0, .16, 0);
        setDeltaMovement(getDeltaMovement().lerp(desired, .18));
    }
    private final class FlightAttackGoal extends Goal {
        private Vec3 idleTarget = Vec3.ZERO;
        FlightAttackGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return true; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            phaseTicks++;
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || distanceToSqr(target) > 64 * 64) {
                if (attackPhase() != TRACK) phase(TRACK);
                if (tickCount % 70 == 0 || idleTarget == Vec3.ZERO || position().distanceToSqr(idleTarget) < 1) {
                    idleTarget = position().add(random.nextInt(13)-6, random.nextDouble()*2 - .5, random.nextInt(13)-6);
                    idleTarget = new Vec3(idleTarget.x, Mth.clamp(idleTarget.y, level().getMinY()+3, level().getMaxY()-3), idleTarget.z);
                }
                steer(idleTarget, .16); face(idleTarget); return;
            }
            Vec3 aim = target.position().add(0, target.getBbHeight() * .65, 0);
            switch (attackPhase()) {
                case TRACK -> {
                    Vec3 away = position().subtract(aim).multiply(1,0,1);
                    if (away.lengthSqr() < .01) away = new Vec3(1,0,0);
                    Vec3 align = aim.add(away.normalize().scale(6));
                    steer(align, .30); face(aim);
                    if (phaseTicks >= 40 && Math.abs(aim.y-getY()) < 1.2 && distanceToSqr(target) < 14*14
                            && getSensing().hasLineOfSight(target)) {
                        phase(WINDUP); playSound(SoundEvents.PHANTOM_AMBIENT, .8F, .6F);
                        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.PORTAL,
                                getX(), getY()+.5, getZ(), 12, .4,.2,.4, .05);
                    }
                }
                case WINDUP -> {
                    setDeltaMovement(getDeltaMovement().scale(.65)); face(aim);
                    if (phaseTicks >= 25) {
                        chargeVelocity = aim.subtract(position()).normalize().scale(1.15);
                        phase(CHARGE); playSound(SoundEvents.PHANTOM_SWOOP, 1, .75F);
                    }
                }
                case CHARGE -> {
                    setDeltaMovement(chargeVelocity); face(position().add(chargeVelocity));
                    boolean hit = getBoundingBox().inflate(.3).intersects(target.getBoundingBox());
                    if (hit && level() instanceof ServerLevel server) doHurtTarget(server, target);
                    if (hit || horizontalCollision || verticalCollision || phaseTicks >= 18) phase(RECOVER);
                }
                case RECOVER -> {
                    setDeltaMovement(getDeltaMovement().scale(.88).add(0,.025,0));
                    if (phaseTicks >= 30) phase(TRACK);
                }
                default -> phase(TRACK);
            }
        }
    }
}
