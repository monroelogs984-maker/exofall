package com.bromax.exofall.entity.mob;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * A mantis that hides in cover and ambushes. When calm it walks into nearby cover (Sapcane, alien
 * plants...) and crouches still. A player (or prey) that comes within reach sets off the TELL: the
 * cover rustles (particles + sound, {@link #tell}) and the mantis rocks with its arms raised for one
 * second, then it lunges and fights normally. Hurting it while hidden makes it fight at once.
 */
public abstract class AmbushMantis extends Bug implements Enemy {
    public static final byte ROAM = 0, HIDDEN = 1, TELL = 2;
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(AmbushMantis.class, EntityDataSerializers.BYTE);
    private static final RawAnimation HIDE = RawAnimation.begin().thenLoop("misc.hide");
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack.windup");

    static final int TELL_TICKS = 20;
    static final double DETECT_PLAYER = 6, DETECT_PREY = 5;

    private int calmTicks;

    protected AmbushMantis(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    /** Blocks this mantis hides in. */
    protected abstract boolean isCover(BlockState state);

    /** The ambush tell: rustle the cover around it. Called at the start and halfway through. */
    protected abstract void tell(ServerLevel level, BlockPos at);

    /** Non-player things it ambushes. */
    protected abstract boolean isPrey(LivingEntity e);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, ROAM);
    }

    public byte ambushState() {
        return entityData.get(STATE);
    }

    void setAmbushState(byte s) {
        entityData.set(STATE, s);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AmbushGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> ambushState() == ROAM));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() != null) {
            calmTicks = 0;
            if (ambushState() != ROAM) setAmbushState(ROAM);
        } else {
            calmTicks++;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) setAmbushState(ROAM);
        return hurt;
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return switch (ambushState()) {
            case HIDDEN -> HIDE;
            case TELL -> WINDUP;
            default -> super.baseAnimation(state);
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hidden", ambushState() == HIDDEN);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Hidden")) setAmbushState(HIDDEN);
    }

    /** Walk into cover, crouch, wait, tell, lunge. Only runs while it has no target. */
    class AmbushGoal extends Goal {
        @Nullable private BlockPos cover;
        @Nullable private LivingEntity prey;
        private int tellTicks;

        AmbushGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && (calmTicks > 60 || ambushState() != ROAM);
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            if (ambushState() == ROAM) {
                cover = findCover();
                if (cover != null) getNavigation().moveTo(cover.getX() + 0.5, cover.getY(), cover.getZ() + 0.5, 0.9);
            }
        }

        @Override
        public void stop() {
            if (ambushState() == TELL) setAmbushState(HIDDEN);
            prey = null;
        }

        @Override
        public void tick() {
            switch (ambushState()) {
                case ROAM -> {
                    if (cover == null || getNavigation().isDone() || distanceToSqr(Vec3.atBottomCenterOf(cover)) < 1.5) {
                        getNavigation().stop();
                        setAmbushState(HIDDEN);
                    }
                }
                case HIDDEN -> {
                    getNavigation().stop();
                    if (tickCount % 4 != 0) return;
                    LivingEntity found = level().getNearestPlayer(getX(), getY(), getZ(), DETECT_PLAYER,
                            e -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e) && hasLineOfSight(e));
                    if (found == null) {
                        found = level().getNearestEntity(LivingEntity.class,
                                net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().range(DETECT_PREY).selector(AmbushMantis.this::isPrey),
                                AmbushMantis.this, getX(), getY(), getZ(), getBoundingBox().inflate(DETECT_PREY));
                    }
                    if (found != null && level() instanceof ServerLevel sl) {
                        prey = found;
                        tellTicks = TELL_TICKS;
                        setAmbushState(TELL);
                        tell(sl, blockPosition());
                    }
                }
                case TELL -> {
                    getNavigation().stop();
                    if (prey == null || !prey.isAlive() || distanceToSqr(prey) > 100) {
                        setAmbushState(HIDDEN);
                        return;
                    }
                    getLookControl().setLookAt(prey, 60, 60);
                    tellTicks--;
                    if (tellTicks == TELL_TICKS / 2 && level() instanceof ServerLevel sl) tell(sl, blockPosition());
                    if (tellTicks <= 0) {
                        Vec3 d = prey.position().subtract(position()).multiply(1, 0, 1).normalize();
                        setDeltaMovement(d.x * 0.7, 0.35, d.z * 0.7);
                        setAmbushState(ROAM);
                        setTarget(prey);
                    }
                }
                default -> {}
            }
        }

        @Nullable
        private BlockPos findCover() {
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            BlockPos me = blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-8, -2, -8), me.offset(8, 2, 8))) {
                if (!isCover(level().getBlockState(p))) continue;
                double d = p.distSqr(me);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
            return best;
        }
    }
}
