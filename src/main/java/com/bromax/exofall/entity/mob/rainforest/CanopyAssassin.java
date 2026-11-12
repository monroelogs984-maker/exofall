package com.bromax.exofall.entity.mob.rainforest;

import com.bromax.exofall.effect.BleedingEffect;
import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Rainforest assassin bug. Hangs upside-down under ceilings (tunnel roofs, canopy leaves). The
 * tell: a silk thread and drips fall from it whenever a player is within 10 blocks, so look up.
 * When a player passes under it (within 2.5 blocks sideways) it drops after 0.5 s: 8 damage and a
 * Bleeding stack if it lands on them. Then it fights on the ground (4 damage) and climbs back up
 * after 10 calm seconds.
 */
public class CanopyAssassin extends Bug implements Enemy {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CanopyAssassin.class, EntityDataSerializers.INT);
    public static final int GROUND = 0, CLINGING = 1, DROPPING = 2, CLIMBING = 3;
    private static final RawAnimation CLING = RawAnimation.begin().thenLoop("misc.cling");
    private static final RawAnimation DROP = RawAnimation.begin().thenPlayAndHold("misc.drop");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH, SoundEvents.SPIDER_STEP, 1.1f);

    static final int TELL_TICKS = 10, CALM_TO_CLIMB = 200, MAX_CEILING = 6;
    static final double TRIGGER = 2.5, WARN = 10;
    static final float DROP_DAMAGE = 8;

    private int tell, calm;
    @Nullable private LivingEntity prey;

    public CanopyAssassin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 9;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(22, 4, 2, 0.32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, GROUND);
    }

    public int state() {
        return entityData.get(STATE);
    }

    public boolean isClinging() {
        return state() == CLINGING;
    }

    private void setState(int s) {
        entityData.set(STATE, s);
        setNoGravity(s == CLINGING || s == CLIMBING);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HangGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> !isClinging()));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        BlockPos ceiling = ceiling();
        if (ceiling != null) {
            moveTo(getX(), ceiling.getY() - getBbHeight(), getZ());
            setState(CLINGING);
        }
        return d;
    }

    /** The first solid underside within 6 blocks straight up, with open air all the way to it. */
    @Nullable
    private BlockPos ceiling() {
        BlockPos p = blockPosition();
        for (int i = 1; i <= MAX_CEILING; i++) {
            BlockPos q = p.above(i);
            BlockState s = level().getBlockState(q);
            if (s.isFaceSturdy(level(), q, Direction.DOWN) || s.is(net.minecraft.tags.BlockTags.LEAVES)) return i >= 3 ? q : null;
            if (!s.getCollisionShape(level(), q).isEmpty()) return null;
        }
        return null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        switch (state()) {
            case CLINGING -> {
                setDeltaMovement(Vec3.ZERO);
                boolean anchored = level().getBlockState(BlockPos.containing(getX(), getBoundingBox().maxY + 0.1, getZ())).isSolid();
                if (!anchored) { setState(GROUND); return; }
                Player near = sl.getNearestPlayer(getX(), getY(), getZ(), WARN, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
                if (near != null && tickCount % 6 == 0) {   // the tell: silk thread + drips
                    sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() - 0.1, getZ(), 1, 0.05, 0, 0.05, 0);
                    sl.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY() - 0.6, getZ(), 3, 0.02, 0.4, 0.02, 0);
                }
                if (tell > 0) {
                    if (--tell == 0) drop();
                    return;
                }
                if (near != null && near.getY() < getY() && Math.hypot(near.getX() - getX(), near.getZ() - getZ()) < TRIGGER) {
                    prey = near;
                    tell = TELL_TICKS;
                    playSound(SoundEvents.SPIDER_AMBIENT, 0.8f, 1.8f);
                }
            }
            case DROPPING -> {
                if (prey != null && prey.isAlive() && getBoundingBox().inflate(0.4).intersects(prey.getBoundingBox())) {
                    if (prey.hurt(damageSources().mobAttack(this), DROP_DAMAGE)) BleedingEffect.addStack(prey, this);
                    triggerAnim("action", "attack");
                    setTarget(prey);
                    prey = null;
                }
                if (onGround()) {
                    setState(GROUND);
                    prey = null;
                }
            }
            case CLIMBING -> {
                setDeltaMovement(0, 0.15, 0);
                if (verticalCollision || horizontalCollision) setState(CLINGING);
                if (++calm > CALM_TO_CLIMB + 80) setState(GROUND);   // nothing up there after all
            }
            default -> {
                if (getTarget() != null) { calm = 0; return; }
                if (++calm > CALM_TO_CLIMB && onGround() && ceiling() != null) {
                    calm = CALM_TO_CLIMB;
                    setState(CLIMBING);
                }
            }
        }
    }

    private void drop() {
        setState(DROPPING);
        calm = 0;
        setDeltaMovement(0, -0.3, 0);
        playSound(SoundEvents.SPIDER_HURT, 0.8f, 1.6f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && (isClinging() || state() == CLIMBING)) {
            tell = 0;
            setState(DROPPING);   // shot down
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isClinging() && super.isPushable();
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return switch (state()) {
            case CLINGING, CLIMBING -> CLING;
            case DROPPING -> DROP;
            default -> super.baseAnimation(state);
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Hang", state() == CLINGING ? CLINGING : GROUND);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setState(tag.getInt("Hang"));
    }

    /** No wandering or chasing while up on the ceiling. */
    class HangGoal extends Goal {
        HangGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return state() != GROUND;
        }

        @Override
        public void tick() {
            getNavigation().stop();
        }
    }
}
