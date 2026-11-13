package com.bromax.exofall.entity.mob;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModEntities;
import com.bromax.exofall.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Swamp meteor prey: a slow, harmless larva that rummages in the mud. Glasswings and Bloodgnats
 * hunt it. Players can breed it with Sapcane (what it is farmed for is not decided yet).
 */
public class Mudgrub extends Animal implements GeoEntity, com.bromax.exofall.entity.mob.pet.Pet {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Mudgrub(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.15);
    }

    /** Spawns on meteor soil, mud or moss at any light level (meteor interiors are dim). */
    public static boolean checkSpawnRules(EntityType<Mudgrub> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModTags.ALIEN_SOIL) || below.is(Blocks.MUD) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.GRASS_BLOCK);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(ModBlocks.SAPCANE.get()), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.SAPCANE.get().asItem()) || stack.is(com.bromax.exofall.registry.ModItems.SAPCANE_CANDY.get());
    }

    // ---- pet: already passive and breedable (vanilla animal breeding); the treat tames it for cross-breeding later
    private final com.bromax.exofall.entity.mob.pet.PetState petState = new com.bromax.exofall.entity.mob.pet.PetState();

    @Override
    public com.bromax.exofall.entity.mob.pet.PetState petState() {
        return petState;
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!petState.tamed) {
            var r = com.bromax.exofall.entity.mob.pet.Pets.interact(this, this, player, hand, isBaby());
            if (r != net.minecraft.world.InteractionResult.PASS) return r;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        petState.save(tag);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        petState.load(tag);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !petState.tamed && super.removeWhenFarAway(distance);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Mudgrub baby = ModEntities.MUDGRUB.get().create(level);
        if (baby != null && petState.tamed && other instanceof Mudgrub o && o.petState.tamed) baby.petState.tamed = true;
        return baby;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f + random.nextFloat() * 0.15f;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.15f, 1.2f);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
