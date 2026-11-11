package com.bromax.exofall.entity.mob.hive;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.HiveMob;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Hive scavenger. Eats food lying on the ground (one item a second; never gear), and only goes for
 * players who are already below half health. Otherwise it minds its own business.
 */
public class Waxmite extends Bug implements Enemy, HiveMob {
    private static final BugSounds SOUNDS = BugSounds.of(SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT,
            SoundEvents.SILVERFISH_DEATH, SoundEvents.SILVERFISH_STEP, 1.2f);

    public Waxmite(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(8, 3, 2, 0.3);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new ScavengeGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p.getHealth() < p.getMaxHealth() * 0.5f));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) HiveMob.alarm(this, source);
        return hurt;
    }

    class ScavengeGoal extends Goal {
        @Nullable private ItemEntity food;
        private int chew, searchDelay;

        ScavengeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || --searchDelay > 0) return false;   // polled every other tick
            searchDelay = 10;
            food = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(12),
                            i -> i.isAlive() && i.getItem().has(DataComponents.FOOD))
                    .stream().min(Comparator.comparingDouble(Waxmite.this::distanceToSqr)).orElse(null);
            return food != null;
        }

        @Override
        public boolean canContinueToUse() {
            return food != null && food.isAlive() && getTarget() == null;
        }

        @Override
        public void tick() {
            if (food == null) return;
            if (distanceToSqr(food) > 2.0) {
                if (chew++ % 5 == 0) getNavigation().moveTo(food, 1.1);
                return;
            }
            getNavigation().stop();
            getLookControl().setLookAt(food);
            if (++chew % 10 == 0) {
                food.getItem().shrink(1);
                if (food.getItem().isEmpty()) food.discard();
                heal(2);
                playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.4f);
                triggerAnim("action", "attack");
            }
        }
    }
}
