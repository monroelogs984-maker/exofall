package com.bromax.exofall.entity.mob.hive;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.FlyingBug;
import com.bromax.exofall.entity.mob.HiveMob;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Hive soldier. Slow, heavy sting with a long, obvious wind-up: it stops, curls its abdomen
 * forward and buzzes louder for 1.2 s, then stings whatever is still in reach. Back off during the
 * wind-up and it hits air.
 */
public class Stinger extends FlyingBug implements Enemy, HiveMob {
    private static final EntityDataAccessor<Boolean> WINDING = SynchedEntityData.defineId(Stinger.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack.windup");
    private static final BugSounds SOUNDS = new BugSounds(SoundEvents.BEE_LOOP_AGGRESSIVE, SoundEvents.BEE_HURT,
            SoundEvents.BEE_DEATH, null, 0.55f, 0.8f);

    static final int WINDUP_TICKS = 24, COOLDOWN = 40;
    static final double START_RANGE = 3.0, HIT_RANGE = 3.6;

    public Stinger(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return flyingAttrs(70, 28, 4, 0.55);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINDING, false);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new StingGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) HiveMob.alarm(this, source);
        return hurt;
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return entityData.get(WINDING) ? WINDUP : super.baseAnimation(state);
    }

    class StingGoal extends Goal {
        private int windup, cooldown;

        StingGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            windup = 0;
            entityData.set(WINDING, false);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            if (cooldown > 0) cooldown--;
            getLookControl().setLookAt(t, 30, 30);
            double d = distanceTo(t);
            if (windup > 0) {
                setDeltaMovement(getDeltaMovement().scale(0.6));
                if (--windup == 0) {
                    entityData.set(WINDING, false);
                    cooldown = COOLDOWN;
                    if (distanceTo(t) <= HIT_RANGE && hasLineOfSight(t)) {
                        if (doHurtTarget(t)) t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), Stinger.this);
                    } else {
                        triggerAnim("action", "attack");   // stings the air
                    }
                }
                return;
            }
            if (d < START_RANGE && cooldown == 0 && hasLineOfSight(t)) {
                windup = WINDUP_TICKS;
                entityData.set(WINDING, true);
                playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 1.6f, 0.4f);
                return;
            }
            // close in to just over sting range, slightly above the target
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0);
            Vec3 off = position().subtract(to).multiply(1, 0, 1);
            off = off.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : off.normalize();
            Vec3 want = to.add(off.scale(cooldown > 0 ? 4.5 : 2.2)).add(0, 0.6, 0);
            getMoveControl().setWantedPosition(want.x, want.y, want.z, 1.0);
        }
    }
}
