package com.bromax.exofall.entity.mob.hive;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.HiveMob;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * A wax-silk cocoon stuck to the hive floor. While a player is within 24 blocks it ripens (2 min)
 * and twitches faster; then it hatches a Combwasp (or, 1 in 5, a Stinger), unless 8 or more hive
 * fighters are already nearby. Break it before then.
 */
public class LarvaCocoon extends Bug implements Enemy, HiveMob {
    private static final EntityDataAccessor<Integer> RIPENESS = SynchedEntityData.defineId(LarvaCocoon.class, EntityDataSerializers.INT);
    private static final RawAnimation SHAKE = RawAnimation.begin().thenPlay("misc.shake");
    private static final BugSounds SOUNDS = new BugSounds(null, SoundEvents.TURTLE_EGG_CRACK, SoundEvents.TURTLE_EGG_BREAK, null, 0.9f, 1f);

    static final int HATCH_TICKS = 2400, CROWDED = 8;

    public LarvaCocoon(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(30, 0, 2, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RIPENESS, 0);
    }

    /** Sets how ripe the cocoon is (0..1); the Waxen Queen lays nearly ripe ones. */
    public void setRipeness(float f) {
        entityData.set(RIPENESS, (int) (Math.clamp(f, 0f, 1f) * HATCH_TICKS));
    }

    public float ripeness() {
        return entityData.get(RIPENESS) / (float) HATCH_TICKS;
    }

    @Override
    public boolean defendsHive() {
        return false;
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 != 0 || !(level() instanceof ServerLevel sl)) return;
        if (!sl.hasNearbyAlivePlayer(getX(), getY(), getZ(), 24)) return;
        int r = entityData.get(RIPENESS) + 20;
        if (r >= HATCH_TICKS) {
            int fighters = sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(16), e -> e instanceof HiveMob h && h.defendsHive()).size();
            if (fighters >= CROWDED) {
                r = HATCH_TICKS - 600;
            } else {
                hatch(sl);
                return;
            }
        }
        entityData.set(RIPENESS, r);
        if (r > HATCH_TICKS * 0.75 && random.nextInt(3) == 0) triggerAnim("action", "shake");
    }

    private void hatch(ServerLevel sl) {
        EntityType<? extends Mob> kind = random.nextInt(5) == 0 ? ModEntities.STINGER.get() : ModEntities.COMBWASP.get();
        Mob young = kind.create(sl);
        if (young != null) {
            young.moveTo(getX(), getY() + 0.5, getZ(), random.nextFloat() * 360, 0);
            young.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.BREEDING, null);
            sl.addFreshEntity(young);
        }
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.HONEYCOMB)), getX(), getY() + 0.7, getZ(), 24, 0.3, 0.4, 0.3, 0.08);
        playSound(SoundEvents.TURTLE_EGG_HATCH, 1.0f, 0.8f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            triggerAnim("action", "shake");
            HiveMob.alarm(this, source);
        }
        return hurt;
    }

    @Override
    protected double animationSpeed() {
        return 1 + ripeness() * 2.5;
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        action.triggerableAnim("shake", SHAKE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Ripeness", entityData.get(RIPENESS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RIPENESS, tag.getInt("Ripeness"));
    }
}
