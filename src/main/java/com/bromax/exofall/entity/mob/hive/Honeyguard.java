package com.bromax.exofall.entity.mob.hive;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.HiveMob;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Hive guard. Slow and heavy, holds corridors behind a tall wax shield and shoves you back with
 * every bash (big knockback, little damage). Hits on the shield (from the front) do 15% damage but
 * wear it down by the damage dealt, doubled for heavy hits of 12+: the Royal Grandhammer breaks
 * it in one blow, a netherite sword in about three. Once broken it fights bare for 30 s while it
 * reseals the comb. Hurting it alarms the hive.
 */
public class Honeyguard extends Bug implements Enemy, HiveMob {
    private static final EntityDataAccessor<Float> SHIELD = SynchedEntityData.defineId(Honeyguard.class, EntityDataSerializers.FLOAT);
    private static final RawAnimation BLOCK = RawAnimation.begin().thenPlay("misc.block");
    private static final BugSounds SOUNDS = new BugSounds(SoundEvents.BEE_LOOP, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH, SoundEvents.SPIDER_STEP, 0.5f, 0.6f);

    public static final float SHIELD_MAX = 60, HEAVY = 28;
    static final float FRONT_TAKEN = 0.15f;
    static final double FRONT_CONE = 0.35;
    static final int RESEAL = 600;

    private int reseal;

    public Honeyguard(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(100, 16, 10, 0.18).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ATTACK_KNOCKBACK, 2.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHIELD, SHIELD_MAX);
    }

    public boolean hasShield() {
        return entityData.get(SHIELD) > 0;
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    public int getHeadRotSpeed() {
        return 5;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Vec3 from = source.getSourcePosition();
        if (hasShield() && from != null && !level().isClientSide && !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);
            Vec3 to = from.subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 1.0E-4 && facing.dot(to.normalize()) > FRONT_CONE) {
                float left = entityData.get(SHIELD) - amount * (amount >= HEAVY ? 2 : 1);
                amount *= FRONT_TAKEN;
                if (left <= 0) {
                    shatter();
                } else {
                    entityData.set(SHIELD, left);
                    playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.6f + random.nextFloat() * 0.2f);
                    playSound(SoundEvents.HONEY_BLOCK_HIT, 1.0f, 0.8f);
                    triggerAnim("action", "block");
                }
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt) HiveMob.alarm(this, source);
        return hurt;
    }

    private void shatter() {
        entityData.set(SHIELD, 0f);
        reseal = RESEAL;
        playSound(SoundEvents.SHIELD_BREAK, 1.2f, 0.7f);
        playSound(SoundEvents.HONEY_BLOCK_BREAK, 1.4f, 0.6f);
        if (level() instanceof ServerLevel sl) {
            Vec3 f = Vec3.directionFromRotation(0, yBodyRot).scale(0.9);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HONEYCOMB_BLOCK.defaultBlockState()),
                    getX() + f.x, getY() + 1.0, getZ() + f.z, 40, 0.5, 0.6, 0.5, 0.15);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!hasShield() && --reseal <= 0) {
            entityData.set(SHIELD, SHIELD_MAX);
            playSound(SoundEvents.HONEYCOMB_WAX_ON, 1.2f, 0.7f);
        }
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        action.triggerableAnim("block", BLOCK);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Shield", entityData.get(SHIELD));
        tag.putInt("Reseal", reseal);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Shield")) entityData.set(SHIELD, tag.getFloat("Shield"));
        reseal = tag.getInt("Reseal");
    }
}
