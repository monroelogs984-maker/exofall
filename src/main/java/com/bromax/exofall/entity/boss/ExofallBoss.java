package com.bromax.exofall.entity.boss;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.entity.mob.Hideable;
import com.bromax.exofall.network.CinematicPayload;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.world.ArenaLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base for the four meteor bosses. Lifecycle:
 * <ol>
 *   <li><b>Dormant</b>: lies still and can't be hurt, at its arena's centre (its "home"), after a sigil opened the seal.</li>
 *   <li><b>Intro</b>: when a player walks into the arena the seal closes behind them and players who haven't seen
 *       this boss's intro get the short cinematic ({@link CinematicPayload}). Can't hurt or be hurt.</li>
 *   <li><b>Fight</b>: {@link #fightTick} runs the boss's own move set. Boss bar for everyone in the arena.</li>
 * </ol>
 * If nobody is left to fight for 10 s the boss heals, goes home and dormant again, and the seal reopens. On death the
 * seal opens for good (a new sigil wakes a new boss). Bosses spawned without an arena (eggs, /summon) skip the
 * dormant and intro steps and treat their spawn point as home.
 *
 * <p>Moves are small state machines: {@link #startMove} sets the move id and triggers its animation on the
 * "action" controller, {@link #tickMove} runs it tick by tick. Every attack has a visible wind-up first.
 */
public abstract class ExofallBoss extends Bug implements Enemy, Hideable {
    public static final int DORMANT = 0, INTRO = 1, FIGHT = 2;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(ExofallBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HIDDEN = SynchedEntityData.defineId(ExofallBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(ExofallBoss.class, EntityDataSerializers.BOOLEAN);
    public static final RawAnimation DORMANT_ANIM = RawAnimation.begin().thenLoop("misc.dormant");
    public static final RawAnimation INTRO_ANIM = RawAnimation.begin().thenPlayAndHold("misc.intro");
    public static final int INTRO_TICKS = 100;
    private static final int RESET_AFTER = 200;

    private final ServerBossEvent bossBar;
    @Nullable protected BlockPos home;
    @Nullable protected Direction door;
    private int introTicks, idleTicks, retargetIn;
    private final Set<UUID> sawIntro = new HashSet<>();

    protected int move, moveTick;
    protected final int[] cooldowns = new int[8];
    /** Bearing / point fixed when a move starts, so the attack goes where the tell said it would. */
    protected Vec3 lockedPoint = Vec3.ZERO;
    protected float lockedYaw;

    protected ExofallBoss(EntityType<? extends PathfinderMob> type, Level level, BossEvent.BossBarColor colour) {
        super(type, level);
        this.bossBar = new ServerBossEvent(getDisplayName(), colour, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 500;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder bossAttrs(double health, double damage, double armor, double speed) {
        return attrs(health, damage, armor, speed).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR_TOUGHNESS, 6).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, FIGHT);
        builder.define(HIDDEN, false);
        builder.define(ENRAGED, false);
    }

    public int state() {
        return entityData.get(STATE);
    }

    private void setState(int s) {
        entityData.set(STATE, s);
    }

    @Override
    public boolean isHidden() {
        return entityData.get(HIDDEN);
    }

    protected void setHidden(boolean h) {
        entityData.set(HIDDEN, h);
    }

    /** Second phase: set once below {@link #enrageAt()} health. */
    public boolean enraged() {
        return entityData.get(ENRAGED);
    }

    protected float enrageAt() {
        return 0.5f;
    }

    /** Called once when the boss drops below {@link #enrageAt()}. */
    protected void onEnrage() {}

    /** Shown under the name in the cinematic. */
    public Component subtitle() {
        return Component.translatable(getType().getDescriptionId() + ".subtitle");
    }

    /** Arena this boss guards; the sigil socket sets it. */
    public void setHome(BlockPos centre, @Nullable Direction door, boolean dormant) {
        this.home = centre;
        this.door = door;
        setState(dormant ? DORMANT : FIGHT);
        restrictTo(centre, ArenaLayout.INNER_R - 1);
    }

    /** Testing: skip straight to the fight. */
    public void forceFight() {
        setState(FIGHT);
        closeSeal();
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home == null) setHome(blockPosition(), null, false);
        for (int i = 0; i < cooldowns.length; i++) if (cooldowns[i] > 0) cooldowns[i]--;
        bossBar.setProgress(getHealth() / getMaxHealth());
        switch (state()) {
            case DORMANT -> {
                getNavigation().stop();
                setTarget(null);
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                if (tickCount % 10 == 0) {
                    List<Player> in = arenaPlayers(ArenaLayout.INNER_R - 2, false);
                    if (!in.isEmpty()) startIntro(in);
                }
            }
            case INTRO -> {
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                Player p = level().getNearestPlayer(this, 40);
                if (p != null) getLookControl().setLookAt(p, 10, 10);
                closeSeal();
                introTick(introTicks);
                if (++introTicks >= INTRO_TICKS) {
                    setState(FIGHT);
                    idleTicks = 0;
                    level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.6f);
                }
            }
            default -> {
                if (tickCount % 10 == 0) updateBar();
                if (tickCount % 20 == 0 && door != null) closeSeal();
                if (!enraged() && getHealth() < getMaxHealth() * enrageAt()) {
                    entityData.set(ENRAGED, true);
                    onEnrage();
                }
                LivingEntity target = pickTarget();
                if (target == null) {
                    if (++idleTicks >= RESET_AFTER && door != null) resetFight();
                } else {
                    idleTicks = 0;
                }
                leash();
                if (move != 0) {
                    tickMove(move, moveTick++, target);
                } else {
                    fightTick(target);
                }
            }
        }
    }

    private void startIntro(List<Player> players) {
        setState(INTRO);
        introTicks = 0;
        closeSeal();
        boolean anyNew = false;
        for (Player p : arenaPlayers(ArenaLayout.INNER_R + 4, false)) {
            if (p instanceof ServerPlayer sp && sawIntro.add(sp.getUUID())) {
                anyNew = true;
                BossCinematics.begin(sp, INTRO_TICKS);
                PacketDistributor.sendToPlayer(sp, new CinematicPayload(getId(), INTRO_TICKS));
            }
        }
        if (!anyNew) introTicks = INTRO_TICKS - 25;   // everyone has seen it: just the roar
        level().playSound(null, blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2f, 0.8f);
    }

    /** Sounds and effects during the intro (ticks 0..{@link #INTRO_TICKS}). */
    protected void introTick(int t) {
        if (t == 45) level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.5f);
    }

    private void resetFight() {
        setHealth(getMaxHealth());
        removeAllEffects();
        setTarget(null);
        endMove();
        setHidden(false);
        entityData.set(ENRAGED, false);
        if (home != null) teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        bossBar.removeAllPlayers();
        openSeal();
        setState(DORMANT);
        onReset();
    }

    /** Clean up summons or other fight state when the fight resets. */
    protected void onReset() {}

    private void leash() {
        if (home == null) return;
        double dx = getX() - (home.getX() + 0.5), dz = getZ() - (home.getZ() + 0.5);
        double max = door != null ? ArenaLayout.INNER_R + 3 : 32;
        if (dx * dx + dz * dz > max * max || getY() < home.getY() - 8) {
            teleportTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5);
            endMove();
        }
    }

    private void updateBar() {
        Set<ServerPlayer> want = new HashSet<>();
        for (Player p : arenaPlayers(ArenaLayout.INNER_R + 6, true)) if (p instanceof ServerPlayer sp) want.add(sp);
        for (ServerPlayer sp : new ArrayList<>(bossBar.getPlayers())) if (!want.contains(sp)) bossBar.removePlayer(sp);
        for (ServerPlayer sp : want) bossBar.addPlayer(sp);
    }

    /** Players (any game mode but spectator) within {@code radius} horizontally of home, near the arena floor. */
    protected List<Player> arenaPlayers(double radius, boolean includeCreative) {
        BlockPos c = home != null ? home : blockPosition();
        AABB box = new AABB(c).inflate(radius, 0, radius).expandTowards(0, ArenaLayout.INNER_V + 2, 0).move(0, -3, 0).expandTowards(0, 3, 0);
        return level().getEntitiesOfClass(Player.class, box, p -> !p.isSpectator() && p.isAlive()
                && (includeCreative || !p.isCreative() || state() != FIGHT)
                && p.distanceToSqr(c.getX() + 0.5, p.getY(), c.getZ() + 0.5) <= radius * radius);
    }

    @Nullable
    private LivingEntity pickTarget() {
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || !canTarget(t) || (home != null && door != null && !ArenaLayout.inside(home, t.position())))) {
            setTarget(null);
            t = null;
        }
        if (--retargetIn <= 0 || t == null) {
            retargetIn = 40;
            Player best = null;
            for (Player p : arenaPlayers(ArenaLayout.INNER_R, false)) {
                if (!p.isCreative() && (best == null || distanceToSqr(p) < distanceToSqr(best))) best = p;
            }
            if (best != null) t = best;
            else if (t == null && getLastHurtByMob() != null && canTarget(getLastHurtByMob())) t = getLastHurtByMob();
            if (t == null) {   // pets and golems that are after the boss
                BlockPos c = home != null ? home : blockPosition();
                for (Mob m : level().getEntitiesOfClass(Mob.class, ArenaLayout.bounds(c), m -> m.getTarget() == this && canTarget(m))) {
                    if (t == null || distanceToSqr(m) < distanceToSqr(t)) t = m;
                }
            }
            setTarget(t);
        }
        return t;
    }

    private boolean canTarget(LivingEntity e) {
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return !(e instanceof Bug);
    }

    // ---------------------------------------------------------------- seal

    protected void closeSeal() {
        if (home == null || door == null) return;
        BlockState seal = ModBlocks.SIGIL_SEAL.get().defaultBlockState();
        for (BlockPos p : ArenaLayout.seal(home, door)) {
            if (!level().getBlockState(p).canBeReplaced()) continue;
            if (!level().getEntitiesOfClass(LivingEntity.class, new AABB(p)).isEmpty()) continue;
            level().setBlockAndUpdate(p, seal);
            ((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        }
    }

    protected void openSeal() {
        if (home == null || door == null) return;
        Arenas.openSeal((ServerLevel) level(), home, door);
    }

    // ---------------------------------------------------------------- moves

    /** No move running: pick one (via {@link #startMove}) or walk toward the target. */
    protected abstract void fightTick(@Nullable LivingEntity target);

    /** Run the current move; call {@link #endMove} when it is done. */
    protected abstract void tickMove(int move, int t, @Nullable LivingEntity target);

    protected void startMove(int id, @Nullable String anim) {
        move = id;
        moveTick = 0;
        getNavigation().stop();
        if (anim != null) triggerAnim("action", anim);
    }

    protected void endMove() {
        move = 0;
        moveTick = 0;
    }

    /** Cooldowns run faster once enraged. */
    protected void cooldown(int slot, int ticks) {
        cooldowns[slot] = enraged() ? (int) (ticks * 0.65) : ticks;
    }

    protected boolean ready(int slot) {
        return cooldowns[slot] <= 0;
    }

    protected void faceYaw(float yaw) {
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    protected float yawTo(Vec3 p) {
        return (float) (Mth.atan2(p.z - getZ(), p.x - getX()) * Mth.RAD_TO_DEG) - 90f;
    }

    protected Vec3 forward(float yaw) {
        return Vec3.directionFromRotation(0, yaw);
    }

    /** Whether this entity may be hit by the boss's attacks. */
    protected boolean hittable(Entity e) {
        if (e == this || !(e instanceof LivingEntity le) || !le.isAlive()) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        if (e instanceof Mob m && m.getTarget() == this) return true;
        return e instanceof net.minecraft.world.entity.animal.IronGolem || (e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame());
    }

    /** Damage everything in a cone in front (yaw), out to range. Returns how many were hit. */
    protected int hitCone(float yaw, double range, double halfAngleDeg, float damage, @Nullable Consumer<LivingEntity> extra) {
        Vec3 f = forward(yaw);
        double cos = Math.cos(Math.toRadians(halfAngleDeg));
        int n = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range), this::hittable)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(position().add(0, getBbHeight() / 2, 0));
            double dist = Math.sqrt(to.x * to.x + to.z * to.z) - e.getBbWidth() / 2 - getBbWidth() / 2;
            if (dist > range) continue;
            Vec3 flat = new Vec3(to.x, 0, to.z).normalize();
            if (dist > 0.5 && flat.dot(f) < cos) continue;
            if (hurtTarget(e, damage)) {
                n++;
                if (extra != null) extra.accept(e);
            }
        }
        return n;
    }

    /** Damage everything within radius of a point. */
    protected int hitRadius(Vec3 c, double radius, float damage, @Nullable Consumer<LivingEntity> extra) {
        int n = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius + 1), this::hittable)) {
            if (e.position().distanceToSqr(c) > (radius + e.getBbWidth() / 2) * (radius + e.getBbWidth() / 2)) continue;
            if (hurtTarget(e, damage)) {
                n++;
                if (extra != null) extra.accept(e);
            }
        }
        return n;
    }

    protected boolean hurtTarget(LivingEntity e, float damage) {
        boolean hit = e.hurt(damageSources().mobAttack(this), damage * (enraged() ? 1.15f : 1f));
        return hit;
    }

    protected static Consumer<LivingEntity> effect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> eff, int ticks, int amp) {
        return e -> e.addEffect(new MobEffectInstance(eff, ticks, amp));
    }

    protected static Consumer<LivingEntity> knock(ExofallBoss from, double strength, double up) {
        return e -> {
            Vec3 d = e.position().subtract(from.position());
            d = new Vec3(d.x, 0, d.z).normalize().scale(strength);
            e.push(d.x, up, d.z);
            e.hurtMarked = true;
        };
    }

    /** Spawns a helper mob next to the boss, already set on the boss's target. */
    @Nullable
    protected <M extends Mob> M summon(EntityType<M> type, Vec3 at, @Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return null;
        M m = type.create(sl);
        if (m == null) return null;
        m.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
        if (target != null) m.setTarget(target);
        sl.addFreshEntity(m);
        sl.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.5, at.z, 8, 0.3, 0.3, 0.3, 0.02);
        return m;
    }

    /** A point near the boss on the arena floor, a few blocks out in a random direction. */
    protected Vec3 nearbyFloor(double min, double max) {
        double a = random.nextDouble() * Math.PI * 2, r = min + random.nextDouble() * (max - min);
        BlockPos c = home != null ? home : blockPosition();
        Vec3 p = position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
        if (home != null && !ArenaLayout.inside(home, p)) p = new Vec3(c.getX() + 0.5 + Math.cos(a) * 6, c.getY(), c.getZ() + 0.5 + Math.sin(a) * 6);
        return new Vec3(p.x, c.getY() + 0.1, p.z);
    }

    protected void particlesAt(net.minecraft.core.particles.ParticleOptions p, Vec3 at, int n, double spread, double speed) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(p, at.x, at.y, at.z, n, spread, spread * 0.4, spread, speed);
    }

    // ---------------------------------------------------------------- rules

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (state() != FIGHT) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossBar.removeAllPlayers();
        openSeal();
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override public boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean requiresCustomPersistence() { return true; }
    @Override public boolean canChangeDimensions(Level from, Level to) { return false; }
    @Override protected boolean canRide(Entity e) { return false; }
    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void pushEntities() {}
    @Override public boolean addEffect(MobEffectInstance e, @Nullable Entity src) {
        return !e.getEffect().value().isBeneficial() && e.getDuration() > 100 ? super.addEffect(new MobEffectInstance(e.getEffect(), 100, Math.min(1, e.getAmplifier())), src) : super.addEffect(e, src);
    }

    @Override
    protected RawAnimation baseAnimation(AnimationState<? extends Bug> state) {
        return switch (state()) {
            case DORMANT -> DORMANT_ANIM;
            case INTRO -> INTRO_ANIM;
            default -> fightAnimation(state);
        };
    }

    protected RawAnimation fightAnimation(AnimationState<? extends Bug> state) {
        return super.baseAnimation(state);
    }

    // ---------------------------------------------------------------- save

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        if (door != null) tag.putString("Door", door.getSerializedName());
        tag.putInt("BossState", state() == INTRO ? DORMANT : state());
        tag.putBoolean("Enraged", enraged());
        ListTag saw = new ListTag();
        for (UUID u : sawIntro) saw.add(NbtUtils.createUUID(u));
        tag.put("SawIntro", saw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(h -> home = h);
        if (tag.contains("Door")) door = Direction.byName(tag.getString("Door"));
        if (tag.contains("BossState")) setState(tag.getInt("BossState"));
        entityData.set(ENRAGED, tag.getBoolean("Enraged"));
        sawIntro.clear();
        for (Tag t : tag.getList("SawIntro", Tag.TAG_INT_ARRAY)) sawIntro.add(NbtUtils.loadUUID(t));
        if (home != null) restrictTo(home, ArenaLayout.INNER_R - 1);
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }
}
