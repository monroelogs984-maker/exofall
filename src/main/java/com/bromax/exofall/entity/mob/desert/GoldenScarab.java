package com.bromax.exofall.entity.mob.desert;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Rare gold-encrusted scarab: heavily armoured, runs from players. Planned to drop the sigil that
 * summons the Dozen-legged Wrath (sigils not built yet; drops gold nuggets for now).
 */
public class GoldenScarab extends Bug {
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.ARMADILLO_HURT, SoundEvents.SILVERFISH_DEATH,
            SoundEvents.SILVERFISH_STEP, 1.0f);

    public GoldenScarab(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(20, 0, 10, 0.3);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.7));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10, 1.3, 1.7));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }
}
