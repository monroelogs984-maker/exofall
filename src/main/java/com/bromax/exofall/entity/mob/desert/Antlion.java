package com.bromax.exofall.entity.mob.desert;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Desert pit trapper. The first time it settles on sand it digs a funnel (radius 3, 2 deep) and
 * sits buried at the bottom with only its jaws showing. Sand trickles down the funnel walls (the
 * tell: you can see the pit and the falling sand). Anything inside the funnel is dragged toward the
 * jaws; sprint and jump to climb out. In reach it bites for 8 and grips (Slowness IV 1 s).
 * Off sand it can't dig and just fights like a slow ground bug.
 */
public class Antlion extends Bug implements Enemy {
    private static final EntityDataAccessor<Boolean> PIT = SynchedEntityData.defineId(Antlion.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation BURIED = RawAnimation.begin().thenLoop("misc.buried");
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH, SoundEvents.SAND_STEP, 0.5f);

    public static final double PIT_RADIUS = 4.5, REACH = 1.9;
    static final double PULL = 0.05;
    static final int BITE_COOLDOWN = 20;

    private boolean dug;
    private int bite;

    public Antlion(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(32, 8, 6, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PIT, false);
    }

    public boolean inPit() {
        return entityData.get(PIT);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PitGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!inPit()) return;
        if (level().isClientSide) {
            // the tell: sand trickling down the funnel walls
            if (tickCount % 3 == 0) {
                double a = random.nextDouble() * Math.PI * 2, r = 1.5 + random.nextDouble() * 2;
                level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                        getX() + Math.cos(a) * r, getY() + 1.2 + r * 0.3, getZ() + Math.sin(a) * r, 0, 0, 0);
            }
            // players move on their own client, so the local player is dragged here
            for (Player p : level().players()) if (p.isLocalPlayer()) drag(p);
        } else {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PIT_RADIUS, 2, PIT_RADIUS),
                    e -> e != this && !(e instanceof Player) && !(e instanceof Antlion))) drag(e);
        }
    }

    private void drag(LivingEntity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        Vec3 d = position().subtract(e.position());
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        if (h > PIT_RADIUS || h < 0.6 || d.y > 0.5 || d.y < -3.5) return;
        double f = PULL * (0.5 + 0.5 * (1 - h / PIT_RADIUS));
        e.setDeltaMovement(e.getDeltaMovement().add(d.x / h * f, 0, d.z / h * f));
    }

    /** Dig a sand funnel around itself and settle at the bottom. */
    private void digPit() {
        dug = true;
        BlockPos c = blockPosition();
        if (!level().getBlockState(c.below()).is(BlockTags.SAND)) return;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                int depth = r < 1.3 ? 2 : r < 2.6 ? 1 : 0;
                for (int k = 1; k <= depth; k++) {
                    BlockPos p = c.offset(dx, -k, dz);
                    BlockState s = level().getBlockState(p);
                    if (s.is(BlockTags.SAND) && level().getBlockState(p.above()).isAir()) level().removeBlock(p, false);
                }
            }
        }
        moveTo(c.getX() + 0.5, c.getY() - 2, c.getZ() + 0.5);
        entityData.set(PIT, true);
        playSound(SoundEvents.SAND_BREAK, 1.2f, 0.6f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (bite > 0) bite--;
        if (!dug && onGround() && tickCount > 5) digPit();
        if (inPit() && !level().getBlockState(blockPosition().below()).is(BlockTags.SAND) && !level().getBlockState(blockPosition().below()).isSolid())
            entityData.set(PIT, false);   // its pit is gone
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return inPit() ? BURIED : super.baseAnimation(state);
    }

    @Override
    public boolean isPushable() {
        return !inPit() && super.isPushable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dug", dug);
        tag.putBoolean("Pit", inPit());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dug = tag.getBoolean("Dug");
        entityData.set(PIT, tag.getBoolean("Pit"));
    }

    /** In its pit: never leaves, bites whatever the funnel drags into reach. */
    class PitGoal extends Goal {
        PitGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return inPit();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (bite > 0) return;
            LivingEntity t = getTarget();
            if (t == null || distanceTo(t) > REACH + 1) {
                t = level().getNearestPlayer(getX(), getY(), getZ(), REACH + 1, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
            }
            if (t != null && distanceTo(t) <= REACH + 0.6) {
                getLookControl().setLookAt(t, 60, 30);
                bite = BITE_COOLDOWN;
                if (doHurtTarget(t)) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
            }
        }
    }
}
