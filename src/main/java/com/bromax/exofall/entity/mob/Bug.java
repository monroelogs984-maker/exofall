package com.bromax.exofall.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base for Exofall's GeckoLib bugs. Two animation controllers: "main" loops the base pose
 * (idle / walk / fly, or whatever {@link #baseAnimation} picks) and "action" plays one-shot
 * triggered animations ("attack" = attack.strike on every landed hit, plus whatever subclasses add).
 * Hostile bugs implement {@link Enemy}: they despawn in Peaceful. Persistent-category bugs
 * (creatures) don't despawn when far away, like animals.
 */
public abstract class Bug extends PathfinderMob implements GeoEntity {
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    public static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");
    public static final RawAnimation STRIKE = RawAnimation.begin().thenPlay("attack.strike");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected Bug(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Mob attributes with the usual bug stats. */
    public static AttributeSupplier.Builder attrs(double health, double damage, double armor, double speed) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor).add(Attributes.MOVEMENT_SPEED, speed).add(Attributes.FOLLOW_RANGE, 24);
    }

    /** Sounds: vanilla stand-ins, pitched per bug. */
    protected abstract BugSounds sounds();

    @Override
    public boolean shouldDespawnInPeaceful() {
        return this instanceof Enemy;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !getType().getCategory().isPersistent();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("action", "attack");
        return hit;
    }

    // ---- sounds
    @Nullable @Override protected SoundEvent getAmbientSound() { return sounds().ambient(); }
    @Nullable @Override protected SoundEvent getHurtSound(DamageSource s) { return sounds().hurt(); }
    @Nullable @Override protected SoundEvent getDeathSound() { return sounds().death(); }

    @Override
    public float getVoicePitch() {
        return sounds().pitch() * (0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    protected float getSoundVolume() {
        return sounds().volume();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (sounds().step() != null) playSound(sounds().step(), 0.15f, sounds().pitch());
    }

    // ---- animation
    /** The looping pose for the "main" controller. */
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return state.isMoving() ? WALK : IDLE;
    }

    /** Playback speed of the "main" controller (e.g. a cocoon twitching faster as it ripens). */
    protected double animationSpeed() {
        return 1.0;
    }

    /** Add more one-shot animations to the "action" controller. */
    protected void addTriggers(AnimationController<Bug> action) {}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Bug>(this, "main", 4, state -> state.setAndContinue(baseAnimation(state)))
                .setAnimationSpeedHandler(b -> b.animationSpeed()));
        AnimationController<Bug> action = new AnimationController<Bug>(this, "action", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", STRIKE);
        addTriggers(action);
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
