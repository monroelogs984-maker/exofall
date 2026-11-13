package com.bromax.exofall.entity.mob.desert;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Desert dung-roller. Keeps 5-16 blocks from its target; when lined up on roughly the same level it
 * rears back for 1 s (digging sound), then shoves its boulder, which rolls straight along the floor
 * and hits hard. Step out of the line. Takes 8 s to pack a new boulder, and bites weakly meanwhile.
 */
public class Roller extends Bug implements Enemy {
    private static final EntityDataAccessor<Boolean> HAS_BOULDER = SynchedEntityData.defineId(Roller.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WINDING = SynchedEntityData.defineId(Roller.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack.windup");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH, SoundEvents.SPIDER_STEP, 0.6f);

    static final int WINDUP_TICKS = 20, REGROW = 160;

    private int regrow;

    public Roller(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(60, 12, 6, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_BOULDER, true);
        builder.define(WINDING, false);
    }

    public boolean hasBoulder() {
        return entityData.get(HAS_BOULDER);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RollGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override
            public boolean canUse() {
                return !hasBoulder() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!hasBoulder() && --regrow <= 0) {
            entityData.set(HAS_BOULDER, true);
            playSound(SoundEvents.MUD_PLACE, 0.8f, 0.8f);
        }
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return entityData.get(WINDING) ? WINDUP : super.baseAnimation(state);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasBoulder", hasBoulder());
        tag.putInt("Regrow", regrow);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HasBoulder")) entityData.set(HAS_BOULDER, tag.getBoolean("HasBoulder"));
        regrow = tag.getInt("Regrow");
    }

    class RollGoal extends Goal {
        private int windup;

        RollGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return hasBoulder() && t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || windup > 0;
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
            if (t == null) {
                windup = 0;
                entityData.set(WINDING, false);
                return;
            }
            getLookControl().setLookAt(t, 30, 30);
            if (windup > 0) {
                getNavigation().stop();
                yBodyRot = getYRot();
                if (--windup == 0) launch(t);
                return;
            }
            double dist = distanceTo(t);
            boolean level = Math.abs(t.getY() - getY()) < 2;
            if (dist < 5) {
                Vec3 away = DefaultRandomPos.getPosAway(Roller.this, 8, 3, t.position());
                if (away != null && getNavigation().isDone()) getNavigation().moveTo(away.x, away.y, away.z, 1.1);
            } else if (dist > 16 || !hasLineOfSight(t) || !level) {
                if (tickCount % 10 == 0) getNavigation().moveTo(t, 1.0);
            } else {
                getNavigation().stop();
                windup = WINDUP_TICKS;
                entityData.set(WINDING, true);
                playSound(SoundEvents.GRAVEL_BREAK, 1.3f, 0.6f);
            }
        }

        private void launch(LivingEntity t) {
            entityData.set(WINDING, false);
            Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
            if (d.lengthSqr() < 0.01) return;
            d = d.normalize();
            RolledBoulder b = new RolledBoulder(level(), Roller.this, d);
            b.moveTo(getX() + d.x * 1.3, getY() + 0.1, getZ() + d.z * 1.3, getYRot(), 0);
            level().addFreshEntity(b);
            entityData.set(HAS_BOULDER, false);
            regrow = REGROW;
            triggerAnim("action", "attack");
            playSound(SoundEvents.MUD_BREAK, 1.2f, 0.7f);
        }
    }
}
