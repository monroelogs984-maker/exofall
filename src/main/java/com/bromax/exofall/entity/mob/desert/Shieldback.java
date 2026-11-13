package com.bromax.exofall.entity.mob.desert;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Armoured desert beetle. Its front shield blocks 80% of damage from anything in front of it
 * (a clang tells you), but it turns slowly: get around to its side or back.
 */
public class Shieldback extends Bug implements Enemy {
    private static final RawAnimation BLOCK = RawAnimation.begin().thenPlay("misc.block");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.IRON_GOLEM_HURT, SoundEvents.SILVERFISH_DEATH, SoundEvents.SPIDER_STEP, 0.5f);

    static final float FRONT_TAKEN = 0.2f;
    static final double FRONT_CONE = 0.5;   // cos 60 degrees

    public Shieldback(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(100, 20, 8, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int getMaxHeadYRot() {
        return 20;
    }

    @Override
    public int getHeadRotSpeed() {
        return 4;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Vec3 from = source.getSourcePosition();
        if (from != null && !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);
            Vec3 to = from.subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 1.0E-4 && facing.dot(to.normalize()) > FRONT_CONE) {
                amount *= FRONT_TAKEN;
                playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.7f + random.nextFloat() * 0.2f);
                triggerAnim("action", "block");
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        action.triggerableAnim("block", BLOCK);
    }
}
