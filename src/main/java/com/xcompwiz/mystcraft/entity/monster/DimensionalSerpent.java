package com.xcompwiz.mystcraft.entity.monster;

import java.util.EnumSet;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A fast ground hunter whose leap can deal damage once, followed by a real cooldown. */
public final class DimensionalSerpent extends Monster {
    private static final EntityDataAccessor<Boolean> POUNCING = SynchedEntityData.defineId(DimensionalSerpent.class, EntityDataSerializers.BOOLEAN);
    private int nextPounce;
    public DimensionalSerpent(EntityType<? extends DimensionalSerpent> type, Level level) { super(type,level); xpReward = 8; }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,6)
                .add(Attributes.ARMOR,0).add(Attributes.MOVEMENT_SPEED,.36).add(Attributes.FOLLOW_RANGE,32);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(POUNCING,false);
    }
    public boolean isPouncing() { return entityData.get(POUNCING); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PounceGoal());
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.25,true));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.9));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level,target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.POISON,200,0),this);
        return hit;
    }
    private final class PounceGoal extends Goal {
        private int ticks;
        private boolean hit;
        PounceGoal() { setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP,Flag.LOOK)); }
        @Override public boolean canUse() {
            var target=getTarget();
            return target!=null && target.isAlive() && onGround() && tickCount>=nextPounce
                    && distanceToSqr(target)>=4 && distanceToSqr(target)<=49 && getSensing().hasLineOfSight(target);
        }
        @Override public boolean canContinueToUse() { return ticks<22 && getTarget()!=null && getTarget().isAlive() && (!onGround() || ticks<3); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() {
            ticks=0;hit=false;entityData.set(POUNCING,true);getNavigation().stop();
            Vec3 delta=getTarget().position().subtract(position()).multiply(1,0,1).normalize().scale(.80);
            setDeltaMovement(delta.x,.40,delta.z);playSound(SoundEvents.SPIDER_AMBIENT,.8F,1.5F);
        }
        @Override public void tick() {
            ticks++;var target=getTarget();
            if (target!=null) {
                getLookControl().setLookAt(target,30,30);
                if (!hit && getBoundingBox().inflate(.25).intersects(target.getBoundingBox()) && level() instanceof ServerLevel server) {
                    hit=true;doHurtTarget(server,target);
                }
            }
        }
        @Override public void stop() { entityData.set(POUNCING,false);nextPounce=tickCount+45; }
    }
}
