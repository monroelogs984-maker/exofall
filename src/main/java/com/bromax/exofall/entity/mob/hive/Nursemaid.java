package com.bromax.exofall.entity.mob.hive;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.FlyingBug;
import com.bromax.exofall.entity.mob.HiveMob;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Hive healer. Never attacks; keeps away from players and flies to the most hurt hive mob (or
 * cocoon) within 16 blocks, feeding it wax: +3 health a second with sparkles and a slurp. Kill her
 * first. Visible by the glowing wax drop she carries.
 */
public class Nursemaid extends FlyingBug implements Enemy, HiveMob {
    private static final RawAnimation TEND = RawAnimation.begin().thenPlay("attack.tend");
    private static final BugSounds SOUNDS = new BugSounds(SoundEvents.BEE_LOOP, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH, null, 1.1f, 0.6f);

    static final float HEAL = 3;
    static final double RANGE = 16;

    public Nursemaid(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return flyingAttrs(16, 0, 0, 0.6);
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
    protected void registerGoals() {
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6, 1.0, 1.4));
        goalSelector.addGoal(2, new TendGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        action.triggerableAnim("tend", TEND);
    }

    class TendGoal extends Goal {
        @Nullable private LivingEntity patient;
        private int ticks, searchDelay;

        TendGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--searchDelay > 0) return false;   // goals are polled every other tick, so count calls
            searchDelay = 10;
            patient = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RANGE),
                            e -> e instanceof HiveMob && e != Nursemaid.this && e.isAlive() && e.getHealth() < e.getMaxHealth())
                    .stream().min(Comparator.comparingDouble(e -> e.getHealth() / e.getMaxHealth())).orElse(null);
            return patient != null;
        }

        @Override
        public boolean canContinueToUse() {
            return patient != null && patient.isAlive() && patient.getHealth() < patient.getMaxHealth() && ticks < 400;
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public void tick() {
            if (patient == null) return;
            ticks++;
            getLookControl().setLookAt(patient, 30, 30);
            if (distanceTo(patient) > 2.5) {
                getMoveControl().setWantedPosition(patient.getX(), patient.getY() + patient.getBbHeight() + 0.8, patient.getZ(), 1.0);
            } else if (ticks % 10 == 0 && level() instanceof ServerLevel sl) {
                patient.heal(HEAL);
                triggerAnim("action", "tend");
                playSound(SoundEvents.HONEY_DRINK, 0.8f, 1.3f);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getY() + patient.getBbHeight() * 0.6, patient.getZ(),
                        6, 0.4, 0.4, 0.4, 0);
            }
        }
    }
}
