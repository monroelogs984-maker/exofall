package com.bromax.exofall.entity.mob;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Swamp meteor dragonfly. It never bites from the air: it climbs to a point above and to the side
 * of its target, hangs there for a second with its wings buzzing loud (the tell), then dives in a
 * straight line at where the target was when the wind-up ended. Step aside and it misses; hit it
 * during the wind-up and it breaks off. Also hunts Bloodgnats and Mudgrubs.
 */
public class Glasswing extends Monster implements GeoEntity, com.bromax.exofall.entity.mob.pet.Pet,
        com.bromax.exofall.entity.mob.pet.Pets.BabySize {
    public static final byte CRUISE = 0, WINDUP = 1, DIVE = 2;
    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(Glasswing.class, EntityDataSerializers.BYTE);

    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");
    private static final RawAnimation WINDUP_ANIM = RawAnimation.begin().thenPlayAndHold("attack.windup");
    private static final RawAnimation DIVE_ANIM = RawAnimation.begin().thenLoop("attack.dive");

    static final int WINDUP_TICKS = 20;
    static final int DIVE_TICKS = 24;
    static final double DIVE_SPEED = 0.95;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean staggered;

    public Glasswing(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.ARMOR, 2).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, CRUISE);
        builder.define(PET_BABY, false);
    }

    public byte phase() {
        return entityData.get(PHASE);
    }

    void setPhase(byte p) {
        entityData.set(PHASE, p);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DiveGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Bloodgnat.class, 10, true, false, null));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mudgrub.class, 20, true, false, null));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && phase() == WINDUP) staggered = true;
        return hurt;
    }

    @Override
    public boolean isNoGravity() {
        return !isDeadOrDying();   // always flying; /summon NBT would otherwise reset it
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BEE_LOOP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> switch (phase()) {
            case WINDUP -> state.setAndContinue(WINDUP_ANIM);
            case DIVE -> state.setAndContinue(DIVE_ANIM);
            default -> state.setAndContinue(state.isMoving() ? FLY : HOVER);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Approach -> wind-up (tell) -> straight dive -> climb away -> repeat. */
    class DiveGoal extends Goal {
        private static final int APPROACH = 0, HOLD = 1, STRIKE = 2, RECOVER = 3;
        private int step;
        private int timer;
        private Vec3 hover;
        private Vec3 dir = Vec3.ZERO;
        private Vec3 retreat;
        private float angleOffset;
        private Vec3 bearing;

        DiveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && canAttack(t);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && canAttack(t) && distanceToSqr(t) < 32 * 32;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            step = APPROACH;
            timer = 0;
            hover = null;
            bearing = null;
            angleOffset = 0;
            getNavigation().stop();
        }

        @Override
        public void stop() {
            setPhase(CRUISE);
            staggered = false;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            switch (step) {
                case APPROACH -> {
                    if (hover == null || timer % 10 == 0) hover = pickHover(t);
                    getMoveControl().setWantedPosition(hover.x, hover.y, hover.z, 1.0);
                    getLookControl().setLookAt(t, 30, 30);
                    timer++;
                    if (distanceToSqr(hover) < 2.25 && hasLineOfSight(t)) {
                        step = HOLD;
                        timer = WINDUP_TICKS;
                        staggered = false;
                        setPhase(WINDUP);
                        playSound(SoundEvents.PHANTOM_SWOOP, 1.2f, 1.7f);
                    } else if (timer > 160) {
                        angleOffset += 70;   // stuck: try another side
                        bearing = null;
                        timer = 0;
                    }
                }
                case HOLD -> {
                    setDeltaMovement(getDeltaMovement().scale(0.5));
                    getLookControl().setLookAt(t, 60, 60);
                    faceTowards(t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(position()));
                    if (staggered) {
                        recover(new Vec3(0, -1, 0));
                    } else if (--timer <= 0) {
                        dir = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(position().add(0, getBbHeight() * 0.5, 0)).normalize();
                        step = STRIKE;
                        timer = DIVE_TICKS;
                        setPhase(DIVE);
                    }
                }
                case STRIKE -> {
                    setDeltaMovement(dir.scale(DIVE_SPEED));
                    faceTowards(dir);
                    if (getBoundingBox().inflate(0.35).intersects(t.getBoundingBox())) {
                        doHurtTarget(t);
                        recover(dir);
                    } else if (horizontalCollision || verticalCollision || --timer <= 0) {
                        recover(dir);
                    }
                }
                case RECOVER -> {
                    getMoveControl().setWantedPosition(retreat.x, retreat.y, retreat.z, 1.0);
                    if (--timer <= 0) {
                        step = APPROACH;
                        timer = 0;
                        hover = null;
                        bearing = null;
                        angleOffset += 40 + random.nextInt(60);
                    }
                }
                default -> {}
            }
        }

        private void recover(Vec3 from) {
            step = RECOVER;
            timer = 30;
            setPhase(CRUISE);
            retreat = position().add(from.x * 4, 4, from.z * 4);
        }

        private void faceTowards(Vec3 d) {
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
            double h = Math.sqrt(d.x * d.x + d.z * d.z);
            setXRot((float) -(Mth.atan2(d.y, h) * Mth.RAD_TO_DEG));
        }

        /**
         * A point ~6 blocks out and ~4 up from the target that we fit into. The side is fixed once per
         * approach (our side of the target, turned by angleOffset) so the point only follows the target.
         */
        private Vec3 pickHover(LivingEntity t) {
            if (bearing == null) {
                Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
                bearing = away.normalize().yRot(angleOffset * Mth.DEG_TO_RAD);
            }
            Vec3 away = bearing;
            for (int up = 4; up >= 1; up--) {
                Vec3 p = t.position().add(away.x * 6, up, away.z * 6);
                if (level().noCollision(Glasswing.this, getBoundingBox().move(p.subtract(position())))) return p;
            }
            return t.position().add(away.x * 4, 2, away.z * 4);
        }
    }
    // ---- pets (only the PetPair species use this; see pet.Pets)
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> PET_BABY =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Glasswing.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private final com.bromax.exofall.entity.mob.pet.PetState petState = new com.bromax.exofall.entity.mob.pet.PetState();

    @Override
    public com.bromax.exofall.entity.mob.pet.PetState petState() {
        return petState;
    }

    private boolean isPetSpecies() {
        return true;
    }

    @Override
    public boolean isBaby() {
        return entityData.get(PET_BABY);
    }

    @Override
    public void setBabySize(boolean baby) {
        entityData.set(PET_BABY, baby);
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        if (PET_BABY.equals(key)) refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        var r = com.bromax.exofall.entity.mob.pet.Pets.interact(this, this, player, hand, isBaby());
        return r != net.minecraft.world.InteractionResult.PASS ? r : super.mobInteract(player, hand);
    }

    @Override
    public void setTarget(@Nullable net.minecraft.world.entity.LivingEntity target) {
        super.setTarget(com.bromax.exofall.entity.mob.pet.Pets.refuses(this, target) ? null : target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isPetSpecies()) {
            com.bromax.exofall.entity.mob.pet.Pets.tick(this, this, l -> (net.minecraft.world.entity.Mob) getType().create(l), this::setBabySize);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !petState.tamed && super.removeWhenFarAway(distance);
    }


    @Override
    public boolean shouldDespawnInPeaceful() {
        return !petState.tamed;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        petState.save(tag);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        petState.load(tag);
        setBabySize(petState.age < 0);
    }
}
