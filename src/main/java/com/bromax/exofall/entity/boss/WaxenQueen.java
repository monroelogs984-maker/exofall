package com.bromax.exofall.entity.boss;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.HiveMob;
import com.bromax.exofall.entity.mob.hive.LarvaCocoon;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashSet;
import java.util.Set;

/**
 * Waxen Queen: the hive's huge queen wasp. Circles above the fight. Moves:
 * <ul>
 *   <li>Sting dive: hovers over you for 1.2 s with her abdomen curled and glowing, then dives where you stood
 *       (18 + Poison II). She lands dazed for 1.5 s afterwards: the time to hit her.</li>
 *   <li>Wax spray: abdomen pulses for 0.8 s, then a fan of 5 wax globs (6 + Slowness II); 7 when enraged.</li>
 *   <li>Brood: lays two nearly ripe Larva Cocoons (they hatch in ~18 s unless broken).</li>
 *   <li>Wing buffet: if you stand under her, a 0.6 s wing-up tell, then a downdraft that throws you back.</li>
 *   <li>Enraged (half health): screeches and calls two Stingers. Hive mobs defend her and Nursemaids heal her.</li>
 * </ul>
 */
public class WaxenQueen extends ExofallBoss implements HiveMob {
    private static final int DIVE = 1, SPRAY = 2, BROOD = 3, BUFFET = 4, GUARD = 5;
    private static final int CD_DIVE = 0, CD_SPRAY = 1, CD_BROOD = 2, CD_BUFFET = 3;
    public static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");
    private final Set<Integer> hitThisDive = new HashSet<>();
    private boolean diving;
    private int diveEnd;
    private double orbit;

    public WaxenQueen(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        this.moveControl = new FlyingMoveControl(this, 10, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return bossAttrs(380, 12, 10, 0.3).add(Attributes.FLYING_SPEED, 0.55);
    }

    @Override
    protected BugSounds sounds() {
        return new BugSounds(SoundEvents.BEE_LOOP_AGGRESSIVE, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH, null, 0.45f, 2.5f);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override public boolean isNoGravity() { return !isDeadOrDying(); }
    @Override protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    protected void addTriggers(AnimationController<Bug> action) {
        for (String a : new String[] {"dive", "recover", "spray", "lay", "buffet", "screech"}) {
            action.triggerableAnim(a, RawAnimation.begin().thenPlay("attack." + a));
        }
    }

    @Override
    protected RawAnimation fightAnimation(AnimationState<? extends Bug> state) {
        return FLY;
    }

    private double floorY() {
        return home != null ? home.getY() : getY() - 5;
    }

    @Override
    protected void fightTick(@Nullable LivingEntity target) {
        if (target == null) {
            hoverAt(home != null ? Vec3.atBottomCenterOf(home).add(0, 6, 0) : position(), 0.6);
            return;
        }
        getLookControl().setLookAt(target, 30, 30);
        Vec3 tp = target.position();
        double flat = Math.sqrt((tp.x - getX()) * (tp.x - getX()) + (tp.z - getZ()) * (tp.z - getZ()));
        if (flat < 3.5 && getY() - tp.y > 2 && ready(CD_BUFFET)) {
            startMove(BUFFET, "buffet");
        } else if (ready(CD_DIVE) && random.nextFloat() < 0.04f) {
            startMove(DIVE, null);
        } else if (flat > 5 && flat < 20 && ready(CD_SPRAY)) {
            startMove(SPRAY, "spray");
        } else if (ready(CD_BROOD) && level().getEntitiesOfClass(LarvaCocoon.class, getBoundingBox().inflate(20)).size() < 4) {
            startMove(BROOD, "lay");
        } else {
            orbit += 0.025;
            Vec3 want = tp.add(Math.cos(orbit) * 7, 0, Math.sin(orbit) * 7);
            hoverAt(new Vec3(want.x, Math.max(floorY() + 5, tp.y + 4.5), want.z), 1.0);
        }
    }

    private void hoverAt(Vec3 p, double speed) {
        if (home != null) {
            double dx = p.x - (home.getX() + 0.5), dz = p.z - (home.getZ() + 0.5), r = Math.sqrt(dx * dx + dz * dz), max = 11;
            if (r > max) p = new Vec3(home.getX() + 0.5 + dx / r * max, p.y, home.getZ() + 0.5 + dz / r * max);
            p = new Vec3(p.x, Math.min(p.y, home.getY() + 9), p.z);
        }
        moveControl.setWantedPosition(p.x, p.y, p.z, speed);
    }

    @Override
    protected void onEnrage() {
        startMove(GUARD, "screech");
    }

    @Override
    protected void tickMove(int m, int t, @Nullable LivingEntity target) {
        switch (m) {
            case DIVE -> dive(t, target);
            case SPRAY -> {
                if (target != null) {
                    getLookControl().setLookAt(target, 30, 30);
                    lockedYaw = yawTo(target.position());
                }
                faceYaw(lockedYaw);
                moveControl.setWantedPosition(getX(), getY(), getZ(), 0.2);
                if (t == 16 && target != null) {
                    int n = enraged() ? 7 : 5;
                    Vec3 from = position().add(0, 0.8, 0);
                    Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
                    for (int i = 0; i < n; i++) {
                        BossGlob g = BossGlob.shoot(this, Items.HONEYCOMB, 6, MobEffects.MOVEMENT_SLOWDOWN, 80, 1);
                        g.setPos(from);
                        Vec3 dir = to.yRot((float) Math.toRadians((i - (n - 1) / 2.0) * 11));
                        g.shoot(dir.x, dir.y + Math.sqrt(dir.x * dir.x + dir.z * dir.z) * 0.08, dir.z, 1.1f, 2f);
                        level().addFreshEntity(g);
                    }
                    playSound(SoundEvents.HONEY_BLOCK_SLIDE, 2f, 0.6f);
                }
                if (t >= 28) {
                    cooldown(CD_SPRAY, 90);
                    endMove();
                }
            }
            case BROOD -> {
                moveControl.setWantedPosition(getX(), floorY() + 2.5, getZ(), 0.6);
                if (t == 20) {
                    for (int i = 0; i < 2; i++) {
                        LarvaCocoon c = summon(ModEntities.LARVA_COCOON.get(), nearbyFloor(2, 5), null);
                        if (c != null) c.setRipeness(0.85f);
                    }
                    playSound(SoundEvents.HONEY_BLOCK_PLACE, 2f, 0.6f);
                }
                if (t >= 40) {
                    cooldown(CD_BROOD, 420);
                    endMove();
                }
            }
            case BUFFET -> {
                moveControl.setWantedPosition(getX(), getY(), getZ(), 0.2);
                if (t == 12) {
                    playSound(SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.7f);
                    Vec3 below = position().add(0, -2.5, 0);
                    particlesAt(ParticleTypes.CLOUD, below, 30, 2.5, 0.1);
                    hitRadius(below, 5.5, 6, knock(this, 1.3, 0.2));
                }
                if (t >= 22) {
                    cooldown(CD_BUFFET, 160);
                    endMove();
                }
            }
            case GUARD -> {
                moveControl.setWantedPosition(getX(), getY(), getZ(), 0.2);
                if (t == 6) level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 1.4f);
                if (t == 14) {
                    for (int i = 0; i < 2; i++) summon(ModEntities.STINGER.get(), nearbyFloor(3, 6).add(0, 2, 0), target);
                }
                if (t >= 30) endMove();
            }
            default -> endMove();
        }
    }

    private void dive(int t, @Nullable LivingEntity target) {
        if (t == 0) {
            diving = false;
            hitThisDive.clear();
            triggerAnim("action", "dive");
            playSound(SoundEvents.BEE_LOOP_AGGRESSIVE, 3f, 0.4f);
        }
        if (t < 24) {
            // Tell: hover over the target, abdomen curled
            if (target != null) {
                lockedPoint = target.position();
                hoverAt(lockedPoint.add(0, 6, 0), 1.2);
                getLookControl().setLookAt(target, 30, 30);
            }
            particlesAt(ParticleTypes.WAX_ON, position(), 3, 0.8, 0.02);
            return;
        }
        if (t == 24) {
            diving = true;
            diveEnd = 0;
            faceYaw(yawTo(lockedPoint));
        }
        if (diving) {
            Vec3 to = lockedPoint.add(0, 0.5, 0).subtract(position());
            if (to.length() > 1.2 && t < 50 && !onGround() && !horizontalCollision) {
                setDeltaMovement(to.normalize().scale(1.1));
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.8), this::hittable)) {
                    if (hitThisDive.add(e.getId())) {
                        hurtTarget(e, 18);
                        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 100, 1));
                    }
                }
                return;
            }
            diving = false;
            setDeltaMovement(Vec3.ZERO);
            hitRadius(position(), 2.2, 10, e -> {
                if (hitThisDive.add(e.getId())) e.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 100, 1));
            });
            particlesAt(ParticleTypes.CLOUD, position(), 20, 1.5, 0.05);
            playSound(SoundEvents.GENERIC_SMALL_FALL, 2f, 0.6f);
            triggerAnim("action", "recover");
            diveEnd = t;
        }
        // Dazed on the floor
        moveControl.setWantedPosition(getX(), floorY() + 0.1, getZ(), 0.3);
        if (t >= diveEnd + 30) {
            cooldown(CD_DIVE, 130);
            endMove();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit) HiveMob.alarm(this, source);
        return hit;
    }
}
