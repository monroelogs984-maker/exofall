package com.bromax.exofall.entity.mob.desert;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.Hideable;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Desert ambusher. Lies buried in sand, invisible. Anything that walks within ~3.5 blocks gets
 * 0.8 s of warning: sand puffs up and hisses where it is (the tell). Then it bursts out and bites.
 * Reburrows after 5 calm seconds on sand. Hitting it while buried digs it out at once.
 */
public class Sanddiver extends Bug implements Enemy, Hideable {
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(Sanddiver.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("misc.emerge");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH, SoundEvents.SAND_STEP, 0.7f);

    static final int TELL_TICKS = 16, CALM_TO_BURROW = 100;
    static final double DETECT = 3.5;

    private int emergeTicks, calm;
    @Nullable private LivingEntity prey;

    public Sanddiver(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(55, 18, 4, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    void setBurrowed(boolean b) {
        entityData.set(BURROWED, b);
    }

    @Override
    public boolean isHidden() {
        return isBurrowed();
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HoldStillGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> !isBurrowed()));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        if (level.getBlockState(blockPosition().below()).is(BlockTags.SAND)) setBurrowed(true);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (emergeTicks > 0) {
            emergeTicks--;
            BlockState sand = sl.getBlockState(blockPosition().below());
            if (emergeTicks % 2 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, sand.isAir() ? net.minecraft.world.level.block.Blocks.SAND.defaultBlockState() : sand),
                        getX(), getY() + 0.2, getZ(), 10, 0.5, 0.15, 0.5, 0.15);
            }
            if (emergeTicks == TELL_TICKS / 2) playSound(SoundEvents.SAND_BREAK, 1.4f, 0.6f);
            if (emergeTicks == 0) emerge(prey);
            return;
        }
        if (isBurrowed()) {
            if (tickCount % 5 == 0) {
                LivingEntity found = sl.getNearestPlayer(getX(), getY(), getZ(), DETECT, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
                if (found == null) {
                    found = sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3),
                            e -> e instanceof EmberBeetle || e instanceof GoldenScarab).stream().findFirst().orElse(null);
                }
                if (found != null) {
                    prey = found;
                    emergeTicks = TELL_TICKS;
                    playSound(SoundEvents.SAND_BREAK, 1.4f, 0.8f);
                }
            }
            return;
        }
        if (getTarget() == null) {
            if (++calm > CALM_TO_BURROW && onGround() && sl.getBlockState(blockPosition().below()).is(BlockTags.SAND)) {
                calm = 0;
                getNavigation().stop();
                setBurrowed(true);
                playSound(SoundEvents.SAND_PLACE, 1.0f, 0.7f);
            }
        } else {
            calm = 0;
        }
    }

    private void emerge(@Nullable LivingEntity target) {
        setBurrowed(false);
        triggerAnim("action", "emerge");
        playSound(SoundEvents.SAND_BREAK, 1.5f, 1.0f);
        if (target != null && target.isAlive()) {
            setTarget(target);
            Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 0.01) d = d.normalize();
            setDeltaMovement(d.x * 0.5, 0.4, d.z * 0.5);
            if (distanceTo(target) < 2.5) doHurtTarget(target);
        }
        prey = null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && (isBurrowed() || emergeTicks > 0)) {
            emergeTicks = 0;
            emerge(null);
        }
        return hurt;
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        action.triggerableAnim("emerge", EMERGE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Burrowed", isBurrowed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBurrowed(tag.getBoolean("Burrowed"));
    }

    /** No moving while buried or bursting out. */
    class HoldStillGoal extends Goal {
        HoldStillGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isBurrowed() || emergeTicks > 0;
        }

        @Override
        public void tick() {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
    }

    @Override
    public boolean isPushable() {
        return !isBurrowed() && super.isPushable();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isBurrowed()) super.playStepSound(pos, state);
    }
}
