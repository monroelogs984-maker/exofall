package com.bromax.exofall.entity.mob.swamp;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Swamp leech. Lurks in murky pools and only hunts things that are in the water. The tell: ripples
 * on the surface drift toward you as it closes in. When it touches you it latches onto your legs and
 * drains 1 heart every 2 s (healing itself). Climb out of the water or hit it to knock it off.
 */
public class MireLeech extends Bug implements Enemy {
    private static final EntityDataAccessor<Integer> HOST = SynchedEntityData.defineId(MireLeech.class, EntityDataSerializers.INT);
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("move.swim");
    private static final RawAnimation LATCH = RawAnimation.begin().thenLoop("misc.latch");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SLIME_HURT_SMALL, SoundEvents.SLIME_DEATH_SMALL, null, 0.7f);

    static final int DRAIN_EVERY = 40, MAX_LATCH = 600, DRY_LIMIT = 10;
    static final float DRAIN = 6;

    private int latched, dry, relatch;

    public MireLeech(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.5F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
        setPathfindingMalus(PathType.WATER, 0.0F);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(24, 4, 0, 1.0);
    }

    public static boolean checkSpawnRules(EntityType<MireLeech> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getFluidState(pos).is(FluidTags.WATER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOST, -1);
    }

    @Nullable
    public LivingEntity host() {
        int id = entityData.get(HOST);
        return id >= 0 && level().getEntity(id) instanceof LivingEntity le ? le : null;
    }

    public boolean isLatched() {
        return entityData.get(HOST) >= 0;
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return !isLatched() && super.canUse();
            }
        });
        goalSelector.addGoal(3, new RandomSwimmingGoal(this, 0.8, 60));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> p.isInWater()));
    }

    /** A landed bite latches on instead of just hurting. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (relatch > 0 || !(target instanceof LivingEntity le) || !le.isInWater()) return super.doHurtTarget(target);
        entityData.set(HOST, le.getId());
        latched = 0;
        dry = 0;
        playSound(SoundEvents.SLIME_ATTACK, 1.0f, 0.6f);
        triggerAnim("action", "attack");
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity h = host();
        if (h == null) {
            noPhysics = false;
            return;
        }
        // ride along on the host's legs, behind them
        noPhysics = true;
        Vec3 back = Vec3.directionFromRotation(0, h.getYRot()).scale(-0.35);
        setPos(h.getX() + back.x, h.getY() + 0.25, h.getZ() + back.z);
        setDeltaMovement(Vec3.ZERO);
        setYRot(h.getYRot());
        yBodyRot = h.getYRot();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (relatch > 0) relatch--;
        LivingEntity h = host();
        if (h == null) {
            if (isLatched()) entityData.set(HOST, -1);
            ripples();
            return;
        }
        getNavigation().stop();
        dry = h.isInWater() ? 0 : dry + 1;
        if (!h.isAlive() || dry > DRY_LIMIT || ++latched > MAX_LATCH || (h instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            detach(false);
            return;
        }
        if (latched % DRAIN_EVERY == 0 && h.hurt(damageSources().mobAttack(this), DRAIN)) {
            heal(DRAIN);
            playSound(SoundEvents.HONEY_DRINK, 0.7f, 0.5f);
        }
    }

    /** The tell: surface ripples above the leech while it stalks someone in the water. */
    private void ripples() {
        if (getTarget() == null || !isInWater() || tickCount % 4 != 0 || !(level() instanceof ServerLevel sl)) return;
        BlockPos p = blockPosition();
        for (int i = 0; i < 8 && sl.getFluidState(p.above()).is(FluidTags.WATER); i++) p = p.above();
        sl.sendParticles(ParticleTypes.FISHING, getX(), p.getY() + 1.0, getZ(), 3, 0.25, 0, 0.25, 0.02);
    }

    private void detach(boolean knocked) {
        LivingEntity h = host();
        entityData.set(HOST, -1);
        noPhysics = false;
        relatch = 60;
        if (h != null) {
            Vec3 away = position().subtract(h.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.3, 0, 0) : away.normalize();
            moveTo(h.getX() + away.x * 0.8, h.getY() + 0.1, h.getZ() + away.z * 0.8);
            setDeltaMovement(away.x * (knocked ? 0.5 : 0.2), 0.3, away.z * (knocked ? 0.5 : 0.2));
        }
        playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0f, 0.7f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isLatched() && !level().isClientSide) detach(true);
        return hurt;
    }

    @Override
    public void move(MoverType type, Vec3 by) {
        if (!isLatched()) super.move(type, by);
    }

    @Override
    public boolean isPushable() {
        return !isLatched() && super.isPushable();
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return !isLatched() && super.canCollideWith(other);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (isLatched()) return;
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        if (isLatched()) return LATCH;
        return isInWater() ? SWIM : super.baseAnimation(state);
    }
}
