package com.bromax.exofall.entity.boss;

import com.bromax.exofall.entity.mob.Bloodgnat;
import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Terror of the Bog: a giant water scorpion. Moves (each with a wind-up you can see):
 * <ul>
 *   <li>Snap: raptorial forelegs rise for 0.7 s, then clap shut in front (16 + Slowness).</li>
 *   <li>Grab: forelegs spread wide for 1 s, then clamp; a caught player is held and squeezed for 2.5 s unless
 *       the boss takes 30 damage meanwhile, which makes it drop them and reel.</li>
 *   <li>Submerge: sinks into the muck (can't be hit), a boil of bubbles marks where it will burst up 1.2 s later (18, launches).</li>
 *   <li>Mud spit: rears back, lobs mud (8 + Slowness III); three at once when enraged.</li>
 *   <li>Enraged (half health): roars and calls Bloodgnat swarms out of the water, more whenever they thin out.</li>
 * </ul>
 */
public class TerrorOfTheBog extends ExofallBoss {
    private static final int SNAP = 1, GRAB = 2, SUBMERGE = 3, SPIT = 4, ROAR = 5, STAGGER = 6;
    private static final int CD_SNAP = 0, CD_GRAB = 1, CD_SUBMERGE = 2, CD_SPIT = 3, CD_GNATS = 4;
    @Nullable private LivingEntity grabbed;
    private float grabDamage;

    public TerrorOfTheBog(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return bossAttrs(1000, 26, 12, 0.26);
    }

    @Override
    protected BugSounds sounds() {
        return new BugSounds(SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH, SoundEvents.SPIDER_STEP, 0.6f, 2.0f);
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        for (String a : new String[] {"snap", "grab", "throw", "submerge", "erupt", "spit", "roar", "stagger"}) {
            action.triggerableAnim(a, RawAnimation.begin().thenPlay("attack." + a));
        }
    }

    @Override
    protected void fightTick(@Nullable LivingEntity target) {
        if (target == null) return;
        getLookControl().setLookAt(target, 20, 20);
        double d = Math.sqrt(distanceToSqr(target)) - getBbWidth() / 2;
        if (d <= 4.0 && ready(CD_GRAB) && random.nextFloat() < 0.35f) {
            startMove(GRAB, "grab");
        } else if (d <= 4.5 && ready(CD_SNAP)) {
            startMove(SNAP, "snap");
        } else if (d >= 6 && d <= 22 && ready(CD_SPIT)) {
            startMove(SPIT, "spit");
        } else if (d >= 3 && ready(CD_SUBMERGE) && random.nextFloat() < 0.03f) {
            startMove(SUBMERGE, "submerge");
        } else if (enraged() && ready(CD_GNATS)
                && level().getEntitiesOfClass(Bloodgnat.class, getBoundingBox().inflate(20)).size() < 3) {
            cooldown(CD_GNATS, 400);
            startMove(ROAR, "roar");
        } else if (d > 3.0) {
            getNavigation().moveTo(target, 1.0);
        } else {
            getNavigation().stop();
        }
        if (move != 0) lockedYaw = yawTo(target.position());
    }

    @Override
    protected void onEnrage() {
        cooldowns[CD_GNATS] = 0;
    }

    @Override
    protected void tickMove(int m, int t, @Nullable LivingEntity target) {
        switch (m) {
            case SNAP -> {
                if (t < 10 && target != null) lockedYaw = yawTo(target.position());
                faceYaw(lockedYaw);
                if (t == 14) {
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.5f);
                    hitCone(lockedYaw, 4.5, 60, 42, effect(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
                if (t >= 24) {
                    cooldown(CD_SNAP, 30);
                    endMove();
                }
            }
            case GRAB -> grab(t, target);
            case SUBMERGE -> submerge(t, target);
            case SPIT -> {
                if (target != null) lockedYaw = yawTo(target.position());
                faceYaw(lockedYaw);
                if (t == 16 && target != null) {
                    int n = enraged() ? 3 : 1;
                    for (int i = 0; i < n; i++) {
                        BossGlob g = BossGlob.shoot(this, Items.MUD, 26, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                        Vec3 from = position().add(forward(lockedYaw).scale(2.0)).add(0, 1.3, 0);
                        g.setPos(from);
                        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
                        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
                        float spread = (i - (n - 1) / 2f) * 9f;
                        Vec3 dir = new Vec3(to.x, 0, to.z).yRot((float) Math.toRadians(spread));
                        g.shoot(dir.x, to.y + flat * 0.22, dir.z, 1.2f, 1.5f);
                        level().addFreshEntity(g);
                    }
                    playSound(SoundEvents.LLAMA_SPIT, 2f, 0.5f);
                }
                if (t >= 26) {
                    cooldown(CD_SPIT, 90);
                    endMove();
                }
            }
            case ROAR -> {
                if (t == 8) level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.55f);
                if (t == 14) {
                    for (int i = 0; i < 4; i++) summon(ModEntities.BLOODGNAT.get(), nearbyFloor(3, 7).add(0, 1.5, 0), target);
                }
                if (t >= 30) endMove();
            }
            case STAGGER -> {
                if (t >= 40) endMove();
            }
            default -> endMove();
        }
    }

    private void grab(int t, @Nullable LivingEntity target) {
        if (t < 16 && target != null) lockedYaw = yawTo(target.position());
        faceYaw(lockedYaw);
        if (t == 20) {
            grabbed = null;
            grabDamage = 0;
            playSound(SoundEvents.PLAYER_ATTACK_STRONG, 2f, 0.4f);
            if (target != null && hittable(target)) {
                Vec3 to = target.position().subtract(position());
                double dist = Math.sqrt(to.x * to.x + to.z * to.z) - getBbWidth() / 2 - target.getBbWidth() / 2;
                if (dist <= 3.5 && new Vec3(to.x, 0, to.z).normalize().dot(forward(lockedYaw)) > 0.6 && hurtTarget(target, 20)) {
                    grabbed = target;
                }
            }
            if (grabbed == null) {
                cooldown(CD_GRAB, 120);
                triggerAnim("action", "throw");
            }
        }
        if (t > 20) {
            if (grabbed == null) {
                if (t >= 34) endMove();
                return;
            }
            if (!grabbed.isAlive() || grabDamage >= 30) {
                // Hurt enough to let go: drops the prey and reels
                grabbed = null;
                cooldown(CD_GRAB, 300);
                triggerAnim("action", "stagger");
                playSound(SoundEvents.RAVAGER_STUNNED, 2f, 0.6f);
                move = STAGGER;
                moveTick = 0;
                return;
            }
            Vec3 hold = position().add(forward(lockedYaw).scale(getBbWidth() / 2 + 1.0)).add(0, 0.4, 0);
            if (t % 2 == 0) {
                if (grabbed instanceof ServerPlayer sp) sp.teleportTo(hold.x, hold.y, hold.z);
                else grabbed.teleportTo(hold.x, hold.y, hold.z);
            }
            grabbed.setDeltaMovement(Vec3.ZERO);
            if (t % 10 == 0) hurtTarget(grabbed, 8);
            if (t >= 70) {
                Vec3 f = forward(lockedYaw);
                grabbed.push(f.x * 1.4, 0.6, f.z * 1.4);
                grabbed.hurtMarked = true;
                triggerAnim("action", "throw");
                grabbed = null;
                cooldown(CD_GRAB, 260);
                endMove();
            }
        }
    }

    private void submerge(int t, @Nullable LivingEntity target) {
        if (t == 0) playSound(SoundEvents.GENERIC_SPLASH, 2f, 0.5f);
        if (t < 12) {
            particlesAt(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), position(), 12, 1.2, 0.1);
            return;
        }
        if (t == 12) {
            setHidden(true);
            lockedPoint = target != null ? target.position() : nearbyFloor(4, 8);
        }
        if (t < 36) {
            particlesAt(ParticleTypes.BUBBLE_POP, lockedPoint.add(0, 0.3, 0), 10, 1.2, 0.05);
            particlesAt(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), lockedPoint.add(0, 0.2, 0), 6, 1.0, 0.05);
            if (t % 6 == 0) level().playSound(null, lockedPoint.x, lockedPoint.y, lockedPoint.z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.HOSTILE, 2f, 0.6f);
            return;
        }
        if (t == 36) {
            teleportTo(lockedPoint.x, lockedPoint.y, lockedPoint.z);
            setHidden(false);
            triggerAnim("action", "erupt");
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.6f);
            particlesAt(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), lockedPoint.add(0, 0.5, 0), 60, 2.0, 0.3);
            hitRadius(lockedPoint, 3.2, 50, knock(this, 0.6, 0.9));
        }
        if (t >= 52) {
            cooldown(CD_SUBMERGE, 260);
            endMove();
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || (isHidden() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && grabbed != null) grabDamage += amount;
        return hit;
    }

    @Override
    protected void onReset() {
        grabbed = null;
    }
}
