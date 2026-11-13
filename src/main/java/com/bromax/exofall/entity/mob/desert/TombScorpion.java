package com.bromax.exofall.entity.mob.desert;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.registry.ModEffects;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Desert tomb guardian. Pincers grab (5 damage, Slowness I 1 s). Every 3 s, when you are within 3.4
 * blocks, it stops and raises its glowing turquoise sting for 0.75 s (the tell, with a dry rattle),
 * then strikes up to 3.8 blocks away: 8 damage and Sandbound (-10% speed) for 3 s. Back off out of
 * the tail's reach during the tell.
 */
public class TombScorpion extends Bug implements Enemy {
    private static final EntityDataAccessor<Boolean> WINDING = SynchedEntityData.defineId(TombScorpion.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack.windup");
    private static final RawAnimation STING = RawAnimation.begin().thenPlay("attack.sting");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH, SoundEvents.SPIDER_STEP, 0.6f);

    static final int WINDUP_TICKS = 15, COOLDOWN = 60;
    static final double START = 3.4, HIT = 3.8;
    static final float STING_DAMAGE = 26;

    private int cooldown = 20;

    public TombScorpion(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(70, 12, 8, 0.27);
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
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new StingGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0), this);
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return entityData.get(WINDING) ? WINDUP : super.baseAnimation(state);
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        action.triggerableAnim("sting", STING);
    }

    class StingGoal extends Goal {
        private int windup;

        StingGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && cooldown == 0 && distanceTo(t) <= START && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            windup = WINDUP_TICKS;
            entityData.set(WINDING, true);
            getNavigation().stop();
            playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.4f, 0.6f);
        }

        @Override
        public void stop() {
            windup = 0;
            entityData.set(WINDING, false);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30, 30);
            if (--windup > 0) return;
            entityData.set(WINDING, false);
            cooldown = COOLDOWN;
            triggerAnim("action", "sting");
            playSound(SoundEvents.TRIDENT_THROW.value(), 1.0f, 0.6f);
            if (t != null && t.isAlive() && distanceTo(t) <= HIT && hasLineOfSight(t)
                    && t.hurt(damageSources().mobAttack(TombScorpion.this), STING_DAMAGE)) {
                t.addEffect(new MobEffectInstance(ModEffects.SANDBOUND, 60, 0), TombScorpion.this);
            }
        }
    }
}
