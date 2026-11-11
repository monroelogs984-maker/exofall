package com.bromax.exofall.entity.boss;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashSet;
import java.util.Set;

/**
 * Dozen-legged Wrath: a giant red centipede, head plus six leg-bearing body segments and a tail that follow the head's path
 * (each segment takes the heading the head had a few ticks earlier; the model and the hittable
 * {@link WrathSegment} parts use the same history). Moves:
 * <ul>
 *   <li>Bite: head rears back 0.5 s, then strikes (15 + Poison II).</li>
 *   <li>Charge: rears the front third up and hisses for 1 s, then runs straight where it was aimed (18, big knockback).
 *       Hitting a wall stuns it for 2.5 s. Charges twice in a row when enraged.</li>
 *   <li>Burrow: dives into the sand (can't be hit), a sand puff marks where it bursts up 1 s later (16, launches).</li>
 *   <li>Tail sweep: if you're beside or behind it, the body coils for 0.6 s and sweeps around (10, knockback).</li>
 *   <li>Enraged (40% health): faster, cooldowns shorter.</li>
 * </ul>
 */
public class DozenLeggedWrath extends ExofallBoss {
    public static final int SEGMENTS = 7, LAG = 4;
    public static final double SEG_LEN = 1.375, HEAD_BACK = 0.5;
    private static final int BITE = 1, CHARGE = 2, BURROW = 3, SWEEP = 4, STUN = 5;
    private static final int CD_BITE = 0, CD_CHARGE = 1, CD_BURROW = 2, CD_SWEEP = 3;
    private static final ResourceLocation ENRAGE_SPEED = com.bromax.exofall.Exofall.id("wrath_enrage");
    private static final int[] PART_AT = {1, 2, 3, 4, 5, 6, 7};

    private final float[] yawHist = new float[SEGMENTS * LAG + 1];
    private boolean histReady;
    private final WrathSegment[] parts;
    private final Set<Integer> hitThisCharge = new HashSet<>();
    private boolean chained;

    public DozenLeggedWrath(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        parts = new WrathSegment[PART_AT.length];
        for (int i = 0; i < parts.length; i++) parts[i] = new WrathSegment(this, 1.5f - i * 0.1f, 1.0f - i * 0.05f);
        setId(getId());   // parts take the ids right after ours (as the ender dragon's do), on both sides
    }

    public static AttributeSupplier.Builder createAttributes() {
        return bossAttrs(450, 15, 14, 0.3);
    }

    @Override
    protected BugSounds sounds() {
        return new BugSounds(SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH, SoundEvents.SILVERFISH_STEP, 0.35f, 2.5f);
    }

    @Override
    protected float enrageAt() {
        return 0.4f;
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        for (String a : new String[] {"bite", "charge", "stun", "burrow", "erupt", "sweep"}) {
            action.triggerableAnim(a, RawAnimation.begin().thenPlay("attack." + a));
        }
    }

    // ---------------------------------------------------------------- body

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    /** World heading of body segment i (0 = head, 1..SEGMENTS trailing). */
    public float segmentYaw(int i) {
        return yawHist[Math.min(yawHist.length - 1, i * LAG)];
    }

    @Override
    public void tick() {
        super.tick();
        if (!histReady) {
            java.util.Arrays.fill(yawHist, yBodyRot);
            histReady = true;
        }
        System.arraycopy(yawHist, 0, yawHist, 1, yawHist.length - 1);
        yawHist[0] = yBodyRot;
        // Lay the hittable parts along the trail
        Vec3 p = position().add(back(segmentYaw(0)).scale(HEAD_BACK));
        int next = 0;
        for (int i = 1; i <= SEGMENTS && next < parts.length; i++) {
            Vec3 dir = back(segmentYaw(i));
            Vec3 mid = p.add(dir.scale(SEG_LEN / 2));
            if (i == PART_AT[next]) {
                WrathSegment s = parts[next++];
                s.xo = s.getX();
                s.yo = s.getY();
                s.zo = s.getZ();
                s.setPos(mid.x, getY(), mid.z);
            }
            p = p.add(dir.scale(SEG_LEN));
        }
    }

    private static Vec3 back(float yaw) {
        return Vec3.directionFromRotation(0, yaw).scale(-1);
    }

    private Vec3 midBody() {
        return parts[3].position();
    }

    // ---------------------------------------------------------------- fight

    @Override
    protected void onEnrage() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(ENRAGE_SPEED)) {
            speed.addTransientModifier(new AttributeModifier(ENRAGE_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.8f);
    }

    @Override
    protected void onReset() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(ENRAGE_SPEED);
    }

    @Override
    protected void fightTick(@Nullable LivingEntity target) {
        if (target == null) return;
        getLookControl().setLookAt(target, 30, 30);
        double d = Math.sqrt(distanceToSqr(target)) - getBbWidth() / 2;
        Vec3 to = target.position().subtract(position());
        double facing = new Vec3(to.x, 0, to.z).normalize().dot(forward(getYRot()));
        if (d <= 6 && facing < -0.2 && ready(CD_SWEEP)) {
            startMove(SWEEP, "sweep");
        } else if (d <= 3.2 && ready(CD_BITE)) {
            startMove(BITE, "bite");
        } else if (d >= 6 && d <= 24 && ready(CD_CHARGE)) {
            chained = false;
            startMove(CHARGE, "charge");
        } else if (ready(CD_BURROW) && random.nextFloat() < 0.025f) {
            startMove(BURROW, "burrow");
        } else if (d > 2.5) {
            getNavigation().moveTo(target, 1.0);
        }
    }

    @Override
    protected void tickMove(int m, int t, @Nullable LivingEntity target) {
        switch (m) {
            case BITE -> {
                if (t < 6 && target != null) lockedYaw = yawTo(target.position());
                faceYaw(lockedYaw);
                if (t == 10) {
                    playSound(SoundEvents.EVOKER_FANGS_ATTACK, 2f, 0.6f);
                    hitCone(lockedYaw, 3.5, 70, 15, effect(MobEffects.POISON, 80, 1));
                }
                if (t >= 18) {
                    cooldown(CD_BITE, 25);
                    endMove();
                }
            }
            case CHARGE -> charge(t, target);
            case STUN -> {
                if (t % 5 == 0) particlesAt(ParticleTypes.CRIT, position().add(0, 1.5, 0), 4, 0.5, 0.1);
                if (t >= 50) endMove();
            }
            case BURROW -> burrow(t, target);
            case SWEEP -> {
                if (t == 12) {
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.4f);
                    hitRadius(midBody(), 4.5, 10, knock(this, 1.4, 0.4));
                    hitRadius(position(), 3.0, 10, knock(this, 1.4, 0.4));
                }
                if (t >= 22) {
                    cooldown(CD_SWEEP, 80);
                    endMove();
                }
            }
            default -> endMove();
        }
    }

    private void charge(int t, @Nullable LivingEntity target) {
        if (t < 16 && target != null) lockedYaw = yawTo(target.position());
        faceYaw(lockedYaw);
        if (t < 20) {
            if (t % 4 == 0) playSound(SoundEvents.SILVERFISH_AMBIENT, 2.5f, 0.3f);
            return;
        }
        if (t == 20) {
            hitThisCharge.clear();
            playSound(SoundEvents.RAVAGER_ROAR, 2f, 1.1f);
        }
        Vec3 f = forward(lockedYaw);
        setDeltaMovement(f.x * 0.85, getDeltaMovement().y, f.z * 0.85);
        particlesAt(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), position(), 4, 0.8, 0.05);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6), this::hittable)) {
            if (hitThisCharge.add(e.getId())) {
                hurtTarget(e, 18);
                knock(this, 1.8, 0.5).accept(e);
            }
        }
        if (horizontalCollision && t > 22) {
            // Slammed into a wall: stunned
            setDeltaMovement(Vec3.ZERO);
            playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 2.5f, 0.5f);
            triggerAnim("action", "stun");
            cooldown(CD_CHARGE, 200);
            move = STUN;
            moveTick = 0;
            return;
        }
        if (t >= 50) {
            setDeltaMovement(Vec3.ZERO);
            if (enraged() && !chained) {
                chained = true;
                moveTick = 8;   // short re-aim, then a second charge
                triggerAnim("action", "charge");
                return;
            }
            cooldown(CD_CHARGE, 160);
            endMove();
        }
    }

    private void burrow(int t, @Nullable LivingEntity target) {
        var sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
        if (t < 14) {
            particlesAt(sand, position(), 14, 1.4, 0.15);
            if (t == 0) playSound(SoundEvents.SAND_BREAK, 2.5f, 0.5f);
            return;
        }
        if (t == 14) {
            setHidden(true);
            lockedPoint = target != null ? target.position() : nearbyFloor(4, 9);
        }
        if (t < 34) {
            particlesAt(sand, lockedPoint.add(0, 0.2, 0), 8, 1.0, 0.08);
            if (t % 5 == 0) level().playSound(null, lockedPoint.x, lockedPoint.y, lockedPoint.z, SoundEvents.SAND_HIT, SoundSource.HOSTILE, 2f, 0.5f);
            return;
        }
        if (t == 34) {
            teleportTo(lockedPoint.x, lockedPoint.y, lockedPoint.z);
            histReady = false;   // the body follows straight up out of the hole
            setHidden(false);
            triggerAnim("action", "erupt");
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.7f);
            particlesAt(sand, lockedPoint.add(0, 0.5, 0), 60, 2.0, 0.3);
            hitRadius(lockedPoint, 3.2, 16, knock(this, 0.5, 1.0));
        }
        if (t >= 50) {
            cooldown(CD_BURROW, 260);
            endMove();
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || (isHidden() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));
    }

    /** For the renderer: smoothly turning segment yaw. */
    public float segmentYaw(int i, float partialTick) {
        int k = Math.min(yawHist.length - 2, i * LAG);
        return Mth.rotLerp(partialTick, yawHist[k + 1], yawHist[k]);
    }
}
