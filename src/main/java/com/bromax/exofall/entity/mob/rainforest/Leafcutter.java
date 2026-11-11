package com.bromax.exofall.entity.mob.rainforest;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Leafcutter ant. March in columns carrying leaves: each ant follows the nearest one ranked ahead of
 * it, the head of the column wanders to far points. Neutral, unless you stand in their way: an ant
 * that is blocked by a player chitters (tell) and after 2 s the whole column turns on them.
 */
public class Leafcutter extends Bug {
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH,
            SoundEvents.SILVERFISH_STEP, 1.1f);
    static final int BLOCKED_TELL = 20, BLOCKED_ANGER = 40;

    private int rank;
    private int blocked;

    public Leafcutter(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.rank = random.nextInt(1_000_000);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(10, 4, 4, 0.3);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(3, new ColumnGoal());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() != null || !getNavigation().isInProgress()) {
            blocked = Math.max(0, blocked - 1);
            return;
        }
        Vec3 ahead = position().add(Vec3.directionFromRotation(0, getYRot()).scale(0.9));
        Player p = level().getNearestPlayer(ahead.x, ahead.y, ahead.z, 1.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (p == null) {
            blocked = Math.max(0, blocked - 2);
            return;
        }
        blocked++;
        if (blocked == BLOCKED_TELL) playSound(SoundEvents.SILVERFISH_AMBIENT, 1.2f, 1.6f);
        if (blocked >= BLOCKED_ANGER) {
            blocked = 0;
            for (Leafcutter ant : level().getEntitiesOfClass(Leafcutter.class, getBoundingBox().inflate(16), a -> a.getTarget() == null)) {
                ant.setTarget(p);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rank", rank);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Rank")) rank = tag.getInt("Rank");
    }

    class ColumnGoal extends Goal {
        @Nullable private Vec3 dest;
        private int delay;

        ColumnGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null;
        }

        @Override
        public void tick() {
            if (--delay > 0) return;   // ticked every other tick
            delay = 5;
            Leafcutter leader = level().getEntitiesOfClass(Leafcutter.class, getBoundingBox().inflate(12), a -> a != Leafcutter.this && a.rank < rank)
                    .stream().max(Comparator.comparingInt(a -> a.rank)).orElse(null);
            if (leader != null) {
                Vec3 behind = leader.position().subtract(Vec3.directionFromRotation(0, leader.getYRot()).scale(1.6));
                if (distanceToSqr(behind) > 2.25) getNavigation().moveTo(behind.x, behind.y, behind.z, 1.1);
                else getNavigation().stop();
                return;
            }
            if (dest == null || distanceToSqr(dest) < 4 || getNavigation().isDone()) {
                dest = DefaultRandomPos.getPos(Leafcutter.this, 24, 4);
                if (dest != null) getNavigation().moveTo(dest.x, dest.y, dest.z, 0.9);
            }
        }
    }
}
