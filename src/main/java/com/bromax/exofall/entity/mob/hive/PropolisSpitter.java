package com.bromax.exofall.entity.mob.hive;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.FlyingBug;
import com.bromax.exofall.entity.mob.HiveMob;
import com.bromax.exofall.entity.mob.SpitGlob;
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
 * Hive artillery. Hovers 6-12 blocks off and a little above its target. The tell: it stops, tips
 * its head back and its glowing belly pumps for 1 s (dripping resin), then it spits a propolis
 * glob: 4 damage, Slowness II for 3 s, and sticky puddles on the floor for 10 s.
 */
public class PropolisSpitter extends FlyingBug implements Enemy, HiveMob {
    private static final EntityDataAccessor<Boolean> WINDING = SynchedEntityData.defineId(PropolisSpitter.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack.windup");
    private static final BugSounds SOUNDS = new BugSounds(SoundEvents.BEE_LOOP, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH, null, 0.7f, 0.7f);

    static final int WINDUP_TICKS = 20, COOLDOWN = 60;
    static final double MIN = 6, MAX = 12, RANGE = 16;

    private int cooldown = 30;

    public PropolisSpitter(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 9;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return flyingAttrs(20, 3, 2, 0.5);
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
        goalSelector.addGoal(2, new SpitGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
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
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (cooldown > 0) cooldown--;
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return entityData.get(WINDING) ? WINDUP : super.baseAnimation(state);
    }

    class SpitGoal extends Goal {
        private int windup;

        SpitGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && distanceTo(t) < RANGE * 1.5;
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
            getLookControl().setLookAt(t, 30, 30);
            if (windup > 0) {
                setDeltaMovement(getDeltaMovement().scale(0.5));
                if (windup % 4 == 0 && level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.DRIPPING_HONEY, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0);
                if (--windup == 0) spit(t);
                return;
            }
            double d = distanceTo(t);
            if (d >= MIN - 1 && d <= RANGE && cooldown == 0 && hasLineOfSight(t)) {
                windup = WINDUP_TICKS;
                entityData.set(WINDING, true);
                playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.5f, 0.6f);
                return;
            }
            // hold a ring 6-12 blocks out, 2-3 blocks above the target
            Vec3 to = t.position();
            Vec3 off = position().subtract(to).multiply(1, 0, 1);
            off = off.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : off.normalize();
            double want = Math.max(MIN + 1, Math.min(MAX - 1, d));
            Vec3 at = to.add(off.scale(want)).add(0, 2.5, 0);
            getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.0);
        }

        private void spit(LivingEntity t) {
            entityData.set(WINDING, false);
            cooldown = COOLDOWN;
            SpitGlob g = SpitGlob.propolis(PropolisSpitter.this);
            double dx = t.getX() - getX(), dz = t.getZ() - getZ();
            double dy = t.getY(0.3) - g.getY();
            g.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.1, dz, 1.0f, 2.0f);
            level().addFreshEntity(g);
            triggerAnim("action", "attack");
            playSound(SoundEvents.LLAMA_SPIT, 1.0f, 0.7f);
        }
    }
}
