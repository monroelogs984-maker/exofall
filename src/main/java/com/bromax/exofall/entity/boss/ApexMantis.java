package com.bromax.exofall.entity.boss;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Apex Mantis: a towering green mantis with blade-arms. Moves:
 * <ul>
 *   <li>Blade combo: blades lift for 0.6 s, then two quick slashes (12 each) and, after a pause, an overhead
 *       chop (18, knockback). It steps in with each.</li>
 *   <li>Leap: crouches for 0.7 s, then jumps to where you stood and lands blades-first (14 in a circle).</li>
 *   <li>Guard: after three hits in 3 s it crosses its blades for 1.5 s. Melee hits from the front are parried
 *       and answered with a riposte (20); hits from behind or from range go through.</li>
 *   <li>Camouflage (enraged, half health): fades into the foliage. Rustling leaves and a soft chime give it
 *       away. It creeps behind you; a loud chime and a leaf burst come 0.5 s before it strikes (22). Hitting
 *       it while hidden knocks it out of hiding.</li>
 * </ul>
 */
public class ApexMantis extends ExofallBoss {
    private static final int COMBO = 1, LEAP = 2, GUARD = 3, CAMO = 4, STAGGER = 5;
    private static final int CD_COMBO = 0, CD_LEAP = 1, CD_GUARD = 2, CD_CAMO = 3;
    private int recentHits, recentHitsTimer, revealAt = -1, landedAt = -1;
    private boolean riposted;

    public ApexMantis(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return bossAttrs(950, 26, 12, 0.32);
    }

    @Override
    protected BugSounds sounds() {
        return new BugSounds(SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH, SoundEvents.SPIDER_STEP, 0.4f, 2.5f);
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        for (String a : new String[] {"combo", "leap", "guard", "riposte", "fade", "ambush", "stagger"}) {
            action.triggerableAnim(a, RawAnimation.begin().thenPlay("attack." + a));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (recentHitsTimer > 0 && --recentHitsTimer == 0) recentHits = 0;
    }

    @Override
    protected void fightTick(@Nullable LivingEntity target) {
        if (target == null) return;
        getLookControl().setLookAt(target, 30, 30);
        double d = Math.sqrt(distanceToSqr(target)) - getBbWidth() / 2;
        if (recentHits >= 3 && ready(CD_GUARD)) {
            riposted = false;
            startMove(GUARD, "guard");
        } else if (enraged() && ready(CD_CAMO) && random.nextFloat() < 0.03f) {
            revealAt = -1;
            startMove(CAMO, "fade");
        } else if (d <= 4.0 && ready(CD_COMBO)) {
            startMove(COMBO, "combo");
        } else if (d >= 7 && d <= 18 && ready(CD_LEAP)) {
            landedAt = -1;
            startMove(LEAP, "leap");
        } else if (d > 2.5) {
            getNavigation().moveTo(target, 1.0);
        }
    }

    private void slash(float damage, double range, double angle, boolean heavy) {
        Vec3 f = forward(lockedYaw);
        push(f.x * 0.35, 0, f.z * 0.35);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, heavy ? 0.4f : 0.7f);
        particlesAt(ParticleTypes.SWEEP_ATTACK, position().add(f.scale(2.5)).add(0, 1.8, 0), 3, 0.8, 0);
        hitCone(lockedYaw, range, angle, damage, heavy ? knock(this, 1.2, 0.3) : null);
    }

    private void aim(@Nullable LivingEntity target, float maxTurn) {
        if (target == null) return;
        float want = yawTo(target.position());
        lockedYaw += net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(want - lockedYaw), -maxTurn, maxTurn);
    }

    @Override
    protected void tickMove(int m, int t, @Nullable LivingEntity target) {
        switch (m) {
            case COMBO -> {
                if (t == 0 && target != null) lockedYaw = yawTo(target.position());
                if (t < 8 || (t > 14 && t < 20) || (t > 24 && t < 30)) aim(target, 8);
                faceYaw(lockedYaw);
                if (t == 12) slash(26, 4.5, 70, false);
                if (t == 22) slash(26, 4.5, 70, false);
                if (t == 36) slash(42, 5.0, 50, true);
                if (t >= 46) {
                    cooldown(CD_COMBO, 40);
                    endMove();
                }
            }
            case LEAP -> leap(t, target);
            case GUARD -> {
                if (target != null) {
                    lockedYaw = yawTo(target.position());
                    faceYaw(lockedYaw);
                }
                getNavigation().stop();
                if (t >= 30 || riposted) {
                    recentHits = 0;
                    cooldown(CD_GUARD, 300);
                    endMove();
                }
            }
            case CAMO -> camo(t, target);
            case STAGGER -> {
                if (t >= 24) endMove();
            }
            default -> endMove();
        }
    }

    private void leap(int t, @Nullable LivingEntity target) {
        if (t < 14) {
            if (target != null) {
                lockedYaw = yawTo(target.position());
                lockedPoint = target.position();
            }
            faceYaw(lockedYaw);
            return;
        }
        if (t == 14) {
            Vec3 to = lockedPoint.subtract(position());
            double flight = 22;
            setDeltaMovement(to.x / flight, 0.9, to.z / flight);
            hasImpulse = true;
            playSound(SoundEvents.RAVAGER_ATTACK, 2f, 1.3f);
            return;
        }
        faceYaw(lockedYaw);
        if (landedAt < 0 && t > 18 && onGround()) {
            landedAt = t;
            setDeltaMovement(Vec3.ZERO);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 1.2f);
            particlesAt(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), position(), 40, 2.0, 0.2);
            hitRadius(position(), 3.2, 36, knock(this, 0.9, 0.4));
        }
        if ((landedAt >= 0 && t >= landedAt + 12) || t > 70) {
            cooldown(CD_LEAP, 140);
            endMove();
        }
    }

    private void camo(int t, @Nullable LivingEntity target) {
        if (t == 10) {
            setHidden(true);
            playSound(SoundEvents.AZALEA_LEAVES_BREAK, 1.5f, 0.6f);
        }
        if (t < 10) return;
        if (revealAt < 0) {
            // Hidden: creep round behind the target; leaves rustle where it is
            if (t % 4 == 0) particlesAt(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AZALEA_LEAVES.defaultBlockState()), position().add(0, 1.5, 0), 3, 0.8, 0.02);
            if (t % 20 == 0) playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.4f);
            if (target != null) {
                Vec3 behind = target.position().add(forward(target.getYRot()).scale(-2.5));
                getNavigation().moveTo(behind.x, behind.y, behind.z, 1.1);
                boolean close = distanceToSqr(behind) < 4;
                if ((close && t > 30) || t >= 90) {
                    revealAt = t;
                    getNavigation().stop();
                    lockedYaw = yawTo(target.position());
                }
            } else if (t >= 90) {
                revealAt = t;
            }
            return;
        }
        faceYaw(lockedYaw);
        if (t == revealAt) {
            setHidden(false);
            triggerAnim("action", "ambush");
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5f, 1.6f);
            particlesAt(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AZALEA_LEAVES.defaultBlockState()), position().add(0, 2, 0), 40, 1.2, 0.2);
        }
        if (t == revealAt + 10) slash(55, 4.5, 80, true);
        if (t >= revealAt + 22) {
            cooldown(CD_CAMO, 360);
            endMove();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (state() == FIGHT && move == GUARD && !riposted && !isInvulnerableTo(source)) {
            Entity attacker = source.getDirectEntity();
            if (attacker != null && attacker == source.getEntity() && distanceToSqr(attacker) < 49) {
                Vec3 to = attacker.position().subtract(position());
                if (new Vec3(to.x, 0, to.z).normalize().dot(forward(getYRot())) > 0.3) {
                    // Parried: clang, then riposte
                    riposted = true;
                    playSound(SoundEvents.SHIELD_BLOCK, 2f, 0.6f);
                    playSound(SoundEvents.ANVIL_LAND, 0.8f, 1.8f);
                    triggerAnim("action", "riposte");
                    lockedYaw = yawTo(attacker.position());
                    faceYaw(lockedYaw);
                    hitCone(lockedYaw, 5.0, 80, 48, knock(this, 1.4, 0.4));
                    return false;
                }
            }
        }
        boolean hit = super.hurt(source, amount);
        if (hit) {
            recentHits++;
            recentHitsTimer = 60;
            if (isHidden()) {
                setHidden(false);
                triggerAnim("action", "stagger");
                getNavigation().stop();
                cooldown(CD_CAMO, 360);
                move = STAGGER;
                moveTick = 0;
            }
        }
        return hit;
    }
}
