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
public abstract class Bug extends PathfinderMob implements GeoEntity, com.bromax.exofall.entity.mob.pet.Pet,
        com.bromax.exofall.entity.mob.pet.Pets.BabySize {
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
        return this instanceof Enemy && !petState.tamed;
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

    // ---- pets (only the PetPair species use this; see pet.Pets)
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> PET_BABY =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Bug.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private final com.bromax.exofall.entity.mob.pet.PetState petState = new com.bromax.exofall.entity.mob.pet.PetState();

    @Override
    public com.bromax.exofall.entity.mob.pet.PetState petState() {
        return petState;
    }

    private boolean isPetSpecies() {
        return com.bromax.exofall.entity.mob.pet.PetPair.of(getType()) != null;
    }

    @Override
    public boolean isBaby() {
        return entityData.get(PET_BABY);
    }

    @Override
    public void setBabySize(boolean baby) {
        entityData.set(PET_BABY, baby);
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        if (PET_BABY.equals(key)) refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        var r = com.bromax.exofall.entity.mob.pet.Pets.interact(this, this, player, hand, isBaby());
        return r != net.minecraft.world.InteractionResult.PASS ? r : super.mobInteract(player, hand);
    }

    @Override
    public void setTarget(@Nullable net.minecraft.world.entity.LivingEntity target) {
        super.setTarget(com.bromax.exofall.entity.mob.pet.Pets.refuses(this, target) ? null : target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isPetSpecies()) {
            com.bromax.exofall.entity.mob.pet.Pets.tick(this, this, l -> (net.minecraft.world.entity.Mob) getType().create(l), this::setBabySize);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !petState.tamed && !getType().getCategory().isPersistent();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PET_BABY, false);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (isPetSpecies()) petState.save(tag);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (isPetSpecies()) {
            petState.load(tag);
            setBabySize(petState.age < 0);
        }
    }
}
