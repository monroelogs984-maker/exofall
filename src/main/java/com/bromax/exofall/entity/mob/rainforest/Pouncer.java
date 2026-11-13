package com.bromax.exofall.entity.mob.rainforest;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Rainforest jumping spider. From 3-7 blocks it crouches for 0.75 s, front legs raised and eyes
 * glinting (the tell), then leaps at where you stood when the crouch ended: 7 damage if it lands on
 * you. Sidestep during the leap. 3 s between leaps; bites for 4 meanwhile.
 */
public class Pouncer extends Bug implements Enemy {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Pouncer.class, EntityDataSerializers.INT);
    static final int READY = 0, CROUCH = 1, LEAP = 2;
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack.windup");
    private static final RawAnimation LEAPING = RawAnimation.begin().thenLoop("move.leap");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH, SoundEvents.SPIDER_STEP, 1.3f);

    static final int CROUCH_TICKS = 15, COOLDOWN = 60;
    static final double MIN = 3, MAX = 7;
    static final float LEAP_DAMAGE = 20;

    private int cooldown = 20, leapTicks;
    private boolean leapHit;

    public Pouncer(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(45, 10, 2, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, READY);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
        if (entityData.get(PHASE) != LEAP) return;
        leapTicks++;
        LivingEntity t = getTarget();
        if (!leapHit && t != null && getBoundingBox().inflate(0.3).intersects(t.getBoundingBox())) {
            leapHit = true;
            if (t.hurt(damageSources().mobAttack(this), LEAP_DAMAGE)) triggerAnim("action", "attack");
        }
        if ((onGround() && leapTicks > 3) || leapTicks > 40) {
            entityData.set(PHASE, READY);
            cooldown = COOLDOWN;
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return switch (entityData.get(PHASE)) {
            case CROUCH -> WINDUP;
            case LEAP -> LEAPING;
            default -> super.baseAnimation(state);
        };
    }

    class LeapGoal extends Goal {
        private int crouch;

        LeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || cooldown > 0 || !onGround()) return false;
            double d = distanceTo(t);
            return d >= MIN && d <= MAX && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return crouch > 0 || entityData.get(PHASE) == LEAP;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            crouch = CROUCH_TICKS;
            entityData.set(PHASE, CROUCH);
            getNavigation().stop();
            playSound(SoundEvents.SPIDER_AMBIENT, 1.0f, 1.7f);
        }

        @Override
        public void stop() {
            crouch = 0;
            if (entityData.get(PHASE) == CROUCH) entityData.set(PHASE, READY);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (crouch <= 0) return;
            getNavigation().stop();
            if (t == null) { crouch = 0; return; }
            getLookControl().setLookAt(t, 60, 30);
            if (crouch % 5 == 0 && level() instanceof ServerLevel sl) {   // the tell: eye glint
                Vec3 f = Vec3.directionFromRotation(0, yBodyRot).scale(0.45);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + f.x, getY() + 0.55, getZ() + f.z, 2, 0.12, 0.05, 0.12, 0);
            }
            if (--crouch > 0) return;
            // ballistic hop at where the target stands now: ~11 ticks airborne, air drag 0.91 (~7x the launch speed covered)
            Vec3 d = t.position().subtract(position());
            double h = Math.sqrt(d.x * d.x + d.z * d.z);
            double v = Math.min(1.2, (h + 0.6) / 6.8);   // aim a little past the target so the landing overlaps it
            Vec3 dir = h < 1.0E-3 ? Vec3.ZERO : new Vec3(d.x / h, 0, d.z / h);
            setDeltaMovement(dir.x * v, 0.45 + Math.max(0, d.y) * 0.08, dir.z * v);
            hasImpulse = true;
            leapTicks = 0;
            leapHit = false;
            entityData.set(PHASE, LEAP);
            playSound(SoundEvents.SPIDER_STEP, 1.2f, 1.8f);
        }
    }
}
