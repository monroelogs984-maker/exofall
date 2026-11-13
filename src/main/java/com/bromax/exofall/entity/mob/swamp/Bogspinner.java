package com.bromax.exofall.entity.mob.swamp;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.SpitGlob;
import com.bromax.exofall.registry.ModBlocks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Swamp fishing spider. While nothing is around it strings Bogsilk Webs across nearby openings
 * (narrow gaps between blocks). In a fight it keeps 4-14 blocks off, rears up for 0.75 s (the tell:
 * it tips its abdomen and hisses), then spits a silk glob that leaves a web where it lands. Bites
 * anything caught in a web for +50%. The Leafcutter Charm walks straight through its webs.
 */
public class Bogspinner extends Bug implements Enemy {
    private static final EntityDataAccessor<Boolean> WINDING = SynchedEntityData.defineId(Bogspinner.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("attack.windup");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("attack.shoot");
    private static final BugSounds SOUNDS = BugSounds.of(SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH, SoundEvents.SPIDER_STEP, 0.8f);
    private static final ResourceLocation WEB_BITE = ResourceLocation.fromNamespaceAndPath("exofall", "web_bite");

    static final int WINDUP_TICKS = 15, SHOT_COOLDOWN = 120, SPIN_EVERY = 400, MAX_WEBS = 3;

    private int spin = 100, shotCooldown = 40;

    public Bogspinner(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(50, 12, 2, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINDING, false);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new WebShotGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Webs never stick their own spinner. */
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 mult) {
        if (!(state.getBlock() instanceof WebBlock)) super.makeStuckInBlock(state, mult);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        AttributeInstance dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        boolean webbed = level().getBlockState(target.blockPosition()).getBlock() instanceof WebBlock;
        if (webbed && dmg != null) dmg.addOrReplacePermanentModifier(new AttributeModifier(WEB_BITE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        try {
            return super.doHurtTarget(target);
        } finally {
            if (dmg != null) dmg.removeModifier(WEB_BITE);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (shotCooldown > 0) shotCooldown--;
        if (getTarget() == null && --spin <= 0) {
            spin = SPIN_EVERY + random.nextInt(200);
            spinWeb();
        }
    }

    /** String a web across a narrow gap near it (an air block with solid blocks on two opposite sides). */
    private void spinWeb() {
        BlockPos me = blockPosition();
        int near = 0;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-6, -3, -6), me.offset(6, 3, 6)))
            if (level().getBlockState(p).is(ModBlocks.BOGSILK_WEB.get()) && ++near >= MAX_WEBS) return;
        for (int tries = 0; tries < 12; tries++) {
            BlockPos p = me.offset(random.nextInt(7) - 3, random.nextInt(3), random.nextInt(7) - 3);
            if (!level().getBlockState(p).isAir() || p.closerThan(me, 1.5)) continue;
            if (gap(p, Direction.EAST) || gap(p, Direction.NORTH) || gap(p, Direction.UP)) {
                level().setBlockAndUpdate(p, ModBlocks.BOGSILK_WEB.get().defaultBlockState());
                playSound(SoundEvents.WOOL_PLACE, 0.6f, 1.4f);
                return;
            }
        }
    }

    private boolean gap(BlockPos p, Direction d) {
        return level().getBlockState(p.relative(d)).isSolid() && level().getBlockState(p.relative(d.getOpposite())).isSolid();
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return entityData.get(WINDING) ? WINDUP : super.baseAnimation(state);
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        action.triggerableAnim("shoot", SHOOT);
    }

    class WebShotGoal extends Goal {
        private int windup;

        WebShotGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || shotCooldown > 0) return false;
            double d = distanceTo(t);
            return d > 4 && d < 14 && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            windup = WINDUP_TICKS;
            entityData.set(WINDING, true);
            getNavigation().stop();
            playSound(SoundEvents.SPIDER_AMBIENT, 1.2f, 1.5f);
        }

        @Override
        public void stop() {
            windup = 0;
            entityData.set(WINDING, false);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30, 30);
            getNavigation().stop();
            if (--windup > 0) return;
            entityData.set(WINDING, false);
            shotCooldown = SHOT_COOLDOWN;
            SpitGlob g = SpitGlob.silk(Bogspinner.this);
            double dx = t.getX() - getX(), dz = t.getZ() - getZ();
            double dy = t.getY(0.3) - g.getY();
            g.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.12, dz, 1.1f, 3.0f);
            level().addFreshEntity(g);
            triggerAnim("action", "shoot");
            playSound(SoundEvents.SPIDER_HURT, 0.8f, 1.8f);
        }
    }
}
