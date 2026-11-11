package com.bromax.exofall.entity.mob.rainforest;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Stick insect. Neutral: freezes like a twig whenever a player is within 8 blocks, and kicks with
 * its spiny hind legs if attacked.
 */
public class Twigwalker extends Bug {
    private static final EntityDataAccessor<Boolean> FROZEN = SynchedEntityData.defineId(Twigwalker.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation FREEZE = RawAnimation.begin().thenLoop("misc.freeze");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.BAMBOO_HIT, SoundEvents.BAMBOO_BREAK, SoundEvents.BAMBOO_STEP, 0.8f);

    public Twigwalker(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(20, 5, 2, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FROZEN, false);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FreezeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return entityData.get(FROZEN) ? FREEZE : super.baseAnimation(state);
    }

    class FreezeGoal extends Goal {
        FreezeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && level().getNearestPlayer(getX(), getY(), getZ(), 8, EntitySelector.NO_SPECTATORS) != null;
        }

        @Override
        public void start() {
            getNavigation().stop();
            entityData.set(FROZEN, true);
        }

        @Override
        public void tick() {
            getNavigation().stop();
        }

        @Override
        public void stop() {
            entityData.set(FROZEN, false);
        }
    }
}
