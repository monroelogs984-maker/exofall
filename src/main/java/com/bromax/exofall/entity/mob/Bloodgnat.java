package com.bromax.exofall.entity.mob;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Swamp meteor mosquito. Comes in swarms of 3-5 with a constant high whine. Each bite drains a
 * little health (healing the gnat) and gives Weakness. After three bites a gnat is full and leaves
 * for 30 seconds, so a swarm wears itself out. Campfire smoke (a lit campfire under it) scatters it.
 */
public class Bloodgnat extends Monster implements GeoEntity {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("attack.bite");

    static final int BITES_TO_SATE = 3;
    static final int SATED_TICKS = 600;
    static final int SMOKED_TICKS = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int bites;
    private int satedTicks;
    private int smokedTicks;
    private BlockPos smokeSource;

    public Bloodgnat(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.8).add(Attributes.FOLLOW_RANGE, 20);
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
        goalSelector.addGoal(1, new FleeSmokeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mudgrub.class, true));
    }

    public boolean isSated() {
        return satedTicks > 0;
    }

    public boolean isSmoked() {
        return smokedTicks > 0;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isSated() && !isSmoked() && super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof LivingEntity victim) {
            heal(2);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
            triggerAnim("main", "bite");
            if (++bites >= BITES_TO_SATE) {
                bites = 0;
                satedTicks = SATED_TICKS;
                setTarget(null);
            }
        }
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (satedTicks > 0) satedTicks--;
        if (smokedTicks > 0) smokedTicks--;
        if (tickCount % 10 == 0) {
            BlockPos fire = findSmoke();
            if (fire != null) {
                smokedTicks = SMOKED_TICKS;
                smokeSource = fire;
                setTarget(null);
            }
        }
    }

    /** A lit campfire up to 10 blocks below and 3 to the side counts as being in its smoke. */
    private BlockPos findSmoke() {
        BlockPos p = blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-3, -10, -3), p.offset(3, 1, 3))) {
            if (CampfireBlock.isLitCampfire(level().getBlockState(q))) return q.immutable();
        }
        return null;
    }

    @Override
    public boolean isNoGravity() {
        return !isDeadOrDying();   // always flying; /summon NBT would otherwise reset it
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Bites", bites);
        tag.putInt("Sated", satedTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bites = tag.getInt("Bites");
        satedTicks = tag.getInt("Sated");
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public float getVoicePitch() {
        return 1.8f + random.nextFloat() * 0.3f;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BEE_LOOP_AGGRESSIVE; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 40;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> state.setAndContinue(FLY))
                .triggerableAnim("bite", BITE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Smoked: climb up and away for a few seconds. */
    class FleeSmokeGoal extends Goal {
        FleeSmokeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isSmoked();
        }

        /** Sideways away from the fire (climbing would keep it in the smoke column). */
        @Override
        public void tick() {
            if (tickCount % 20 != 0 && getMoveControl().hasWanted()) return;
            Vec3 away = smokeSource == null ? new Vec3(random.nextGaussian(), 0, random.nextGaussian())
                    : position().subtract(Vec3.atCenterOf(smokeSource)).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
            away = away.normalize().yRot((random.nextFloat() - 0.5f) * 1.2f);
            Vec3 to = position().add(away.x * 10, random.nextInt(3) - 1, away.z * 10);
            getMoveControl().setWantedPosition(to.x, to.y, to.z, 1.3);
        }
    }
}
