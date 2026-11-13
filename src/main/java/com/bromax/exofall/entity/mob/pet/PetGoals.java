package com.bromax.exofall.entity.mob.pet;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.config.ExofallConfig;
import com.bromax.exofall.entity.boss.ExofallBoss;
import com.bromax.exofall.item.ArmorSet;
import com.bromax.exofall.item.BugArmorItem;
import com.bromax.exofall.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Pet AI, added to every {@link PetPair} bug when it joins a level (so the eight mob classes stay as they were):
 * <ul>
 *   <li>Wild-only goals (hunting players and prey, hiding for an ambush, tending the hive) switch off once tamed.
 *       Fighting back when hurt switches off while the pet is told to Stay.</li>
 *   <li>All pets: Stay, follow the owner (teleporting when far), Crystallized Honey armour regen.</li>
 *   <li>Fighters attack whatever hurts their owner and whatever their owner hits. The Shieldback also taunts.</li>
 *   <li>Helpers: the Nursemaid heals, the Mudgrub digs, the Golden Scarab senses ores and digs up gold.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class PetGoals {
    public static final ResourceKey<LootTable> MUDGRUB_DIG = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Exofall.id("gameplay/mudgrub_dig"));
    public static final ResourceKey<LootTable> SCARAB_DIG = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, Exofall.id("gameplay/scarab_dig"));

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob) || !(mob instanceof Pet pet)) return;
        if (PetPair.of(mob.getType()) == null) return;
        install(mob, pet);
        PetStats.apply(mob);
    }

    static void install(PathfinderMob mob, Pet pet) {
        GoalSelector goals = mob.goalSelector, targets = mob.targetSelector;
        for (WrappedGoal w : goals.getAvailableGoals()) if (w.getGoal() instanceof Upkeep) return;   // already done
        PetState st = pet.petState();
        BooleanSupplier wild = () -> !st.tamed;
        BooleanSupplier notStaying = () -> !st.tamed || st.mode != PetState.Mode.STAY;
        gate(goals, g -> g instanceof WildOnly, wild);
        gate(targets, g -> g instanceof NearestAttackableTargetGoal<?>, wild);
        gate(targets, g -> g instanceof HurtByTargetGoal, notStaying);

        PetRole role = PetRole.of(mob.getType());
        goals.addGoal(0, new StayGoal(mob, st));
        goals.addGoal(0, new Upkeep(mob, pet, role));
        goals.addGoal(3, new FollowOwnerGoal(mob, pet));
        if (role == PetRole.NURSEMAID) goals.addGoal(2, new NurseGoal(mob, pet));
        if (role != null && role.fighter) {
            targets.addGoal(1, new OwnerTargetGoal(mob, pet, true));
            targets.addGoal(2, new OwnerTargetGoal(mob, pet, false));
        }
    }

    /** Wraps every goal matching {@code which} so it only runs while {@code allowed}. */
    private static void gate(GoalSelector sel, java.util.function.Predicate<Goal> which, BooleanSupplier allowed) {
        List<WrappedGoal> found = new ArrayList<>();
        for (WrappedGoal w : sel.getAvailableGoals()) if (which.test(w.getGoal())) found.add(w);
        for (WrappedGoal w : found) {
            sel.removeGoal(w.getGoal());
            sel.addGoal(w.getPriority(), new Gated(w.getGoal(), allowed));
        }
    }

    static final class Gated extends Goal {
        final Goal inner;
        private final BooleanSupplier allowed;

        Gated(Goal inner, BooleanSupplier allowed) {
            this.inner = inner;
            this.allowed = allowed;
            setFlags(inner.getFlags());
        }

        @Override public boolean canUse() { return allowed.getAsBoolean() && inner.canUse(); }
        @Override public boolean canContinueToUse() { return allowed.getAsBoolean() && inner.canContinueToUse(); }
        @Override public boolean isInterruptable() { return inner.isInterruptable(); }
        @Override public void start() { inner.start(); }
        @Override public void stop() { inner.stop(); }
        @Override public boolean requiresUpdateEveryTick() { return inner.requiresUpdateEveryTick(); }
        @Override public void tick() { inner.tick(); }
    }

    @Nullable
    static ArmorSet armour(Mob mob) {
        return mob.getItemBySlot(EquipmentSlot.BODY).getItem() instanceof BugArmorItem b ? b.set() : null;
    }

    // ---------------------------------------------------------------- all pets

    /** Stay: stand (or hover) still. */
    static final class StayGoal extends Goal {
        private final Mob mob;
        private final PetState st;

        StayGoal(Mob mob, PetState st) {
            this.mob = mob;
            this.st = st;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() { return st.tamed && st.mode == PetState.Mode.STAY; }
        @Override public void start() { mob.getNavigation().stop(); }
        @Override public void tick() { mob.getNavigation().stop(); }
    }

    /** Follow: come back when further than followDistance, stop at half of it. */
    static final class FollowOwnerGoal extends Goal {
        private final PathfinderMob mob;
        private final Pet pet;
        @Nullable private Player owner;
        private int repath;

        FollowOwnerGoal(PathfinderMob mob, Pet pet) {
            this.mob = mob;
            this.pet = pet;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean following() {
            PetState st = pet.petState();
            return st.tamed && st.mode == PetState.Mode.FOLLOW && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        public boolean canUse() {
            if (!following()) return false;
            owner = Pets.owner(mob, pet);
            double d = ExofallConfig.PET_FOLLOW_DISTANCE.get();
            return owner != null && !owner.isSpectator() && mob.distanceToSqr(owner) > d * d;
        }

        @Override
        public boolean canContinueToUse() {
            double d = ExofallConfig.PET_FOLLOW_DISTANCE.get() / 2;
            return following() && owner != null && owner.isAlive() && owner.level() == mob.level() && mob.distanceToSqr(owner) > d * d;
        }

        @Override public void start() { repath = 0; }
        @Override public void stop() { owner = null; mob.getNavigation().stop(); }

        @Override
        public void tick() {
            if (owner == null) return;
            mob.getLookControl().setLookAt(owner, 10, mob.getMaxHeadXRot());
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(owner, 1.2);
            }
        }
    }

    /**
     * Flagless, runs alongside everything: teleport to a far owner, armour upkeep, and each helper's job (Shieldback
     * taunt, Mudgrub dig, Golden Scarab ore sense + dig).
     */
    static final class Upkeep extends Goal {
        private final PathfinderMob mob;
        private final Pet pet;
        @Nullable private final PetRole role;
        private int ticks;

        Upkeep(PathfinderMob mob, Pet pet, @Nullable PetRole role) {
            this.mob = mob;
            this.pet = pet;
            this.role = role;
        }

        @Override public boolean canUse() { return pet.isTamedPet(); }
        @Override public boolean canContinueToUse() { return pet.isTamedPet(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (!(mob.level() instanceof ServerLevel level)) return;
            ticks++;
            PetState st = pet.petState();
            Player owner = Pets.owner(mob, pet);
            if (owner != null && owner.level() == level && st.mode == PetState.Mode.FOLLOW && ticks % 10 == 0) teleportIfFar(owner);
            ArmorSet set = armour(mob);
            int regen = ExofallConfig.PET_HONEY_REGEN_INTERVAL.get();
            if (set == ArmorSet.CRYSTALLIZED_HONEY && regen > 0 && ticks % regen == 0 && mob.getHealth() < mob.getMaxHealth()) mob.heal(1);
            if (role == null || mob.isBaby()) return;
            switch (role) {
                case SHIELDBACK -> { if (st.mode != PetState.Mode.STAY && ticks % ExofallConfig.PET_TAUNT_INTERVAL.get() == 0) taunt(level); }
                case MUDGRUB -> { if (job(st, true, ExofallConfig.PET_DIG_INTERVAL.get()) && !dig(level, MUDGRUB_DIG, true)) st.jobTimer = RETRY; }
                case GOLDEN_SCARAB -> {
                    int sense = ExofallConfig.PET_SENSE_INTERVAL.get();
                    if (sense > 0 && ticks % sense == 0 && owner != null && owner.level() == level && mob.distanceToSqr(owner) < 32 * 32) sense(level, owner);
                    if (job(st, false, ExofallConfig.PET_NUGGET_INTERVAL.get()) && !dig(level, SCARAB_DIG, false)) st.nuggetTimer = RETRY;
                }
                default -> {}
            }
        }

        /** Counts down a helper's dig timer; true (and restarts at 75-125% of the interval) when it's time. */
        private boolean job(PetState st, boolean mudgrub, int interval) {
            if (interval <= 0) return false;
            int t = mudgrub ? st.jobTimer : st.nuggetTimer;
            if (t < 0 || t > interval * 2) t = interval / 2 + mob.getRandom().nextInt(interval);   // first find comes sooner
            boolean due = --t <= 0;
            if (due) t = interval * 3 / 4 + mob.getRandom().nextInt(Math.max(1, interval / 2));
            if (mudgrub) st.jobTimer = t; else st.nuggetTimer = t;
            return due;
        }

        private void teleportIfFar(Player owner) {
            double tp = ExofallConfig.PET_TELEPORT_DISTANCE.get();
            if (tp <= 0 || owner.isSpectator() || mob.distanceToSqr(owner) < tp * tp || mob.isLeashed() || mob.isPassenger()) return;
            boolean flyer = mob.isNoGravity();
            for (int i = 0; i < 12; i++) {
                int dx = mob.getRandom().nextIntBetweenInclusive(-3, 3), dz = mob.getRandom().nextIntBetweenInclusive(-3, 3);
                if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                int dy = flyer ? mob.getRandom().nextIntBetweenInclusive(0, 2) : mob.getRandom().nextIntBetweenInclusive(-1, 1);
                BlockPos p = owner.blockPosition().offset(dx, dy, dz);
                if (!flyer && !mob.level().getBlockState(p.below()).isFaceSturdy(mob.level(), p.below(), Direction.UP)) continue;
                if (!mob.level().getFluidState(p).isEmpty()) continue;
                Vec3 to = Vec3.atBottomCenterOf(p);
                if (!mob.level().noCollision(mob, mob.getBoundingBox().move(to.subtract(mob.position())))) continue;
                mob.setTarget(null);
                mob.moveTo(to.x, to.y, to.z, mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                return;
            }
        }

        /** Shieldback: monsters near it that are after its owner (or the owner's pets) come for the Shieldback instead. */
        private void taunt(ServerLevel level) {
            double r = ExofallConfig.PET_TAUNT_RADIUS.get();
            if (r <= 0) return;
            boolean bosses = ExofallConfig.PET_TAUNT_BOSSES.get();
            int n = 0;
            for (Mob m : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(r), e -> e instanceof Enemy && e != mob && e.isAlive())) {
                LivingEntity t = m.getTarget();
                if (t == null || t == mob || !Pets.sameSide(pet, t)) continue;
                if (!bosses && (m instanceof ExofallBoss || m.getType().is(Tags.EntityTypes.BOSSES))) continue;
                if (m instanceof Pet p && p.isTamedPet()) continue;
                m.setTarget(mob);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getEyeY() + 0.4, m.getZ(), 1, 0.1, 0.1, 0.1, 0);
                n++;
            }
            if (n > 0) {
                level.playSound(null, mob, SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1.0f, 0.5f);
                if (mob instanceof software.bernie.geckolib.animatable.GeoEntity g) g.triggerAnim("action", "block");
            }
        }

        /** A dig that couldn't happen (in the air, wrong ground) tries again this soon. */
        static final int RETRY = 100;

        /** Rolls a dig loot table at its feet. The Mudgrub only digs in soft ground (dirt, mud, sand, moss, meteor soil). */
        private boolean dig(ServerLevel level, ResourceKey<LootTable> table, boolean softOnly) {
            BlockPos below = mob.getOnPos();   // the supporting block (mud and soul sand are lower than a full block)
            BlockState ground = level.getBlockState(below);
            if (!mob.onGround() || ground.isAir()) return false;
            if (softOnly && !(ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND) || ground.is(ModTags.ALIEN_SOIL)
                    || ground.is(Blocks.MUD) || ground.is(Blocks.CLAY) || ground.is(BlockTags.MOSS_REPLACEABLE))) return false;
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, mob.position())
                    .withParameter(LootContextParams.THIS_ENTITY, mob).create(LootContextParamSets.GIFT);
            List<ItemStack> loot = level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params);
            if (loot.isEmpty()) return true;
            for (ItemStack s : loot) mob.spawnAtLocation(s, 0.3f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), mob.getX(), mob.getY() + 0.1, mob.getZ(),
                    20, mob.getBbWidth() * 0.4, 0.1, mob.getBbWidth() * 0.4, 0.1);
            level.playSound(null, mob, ground.getSoundType(level, below, mob).getBreakSound(), SoundSource.NEUTRAL, 0.8f, 0.9f);
            return true;
        }

        /** Golden Scarab: finds the nearest ore (gold first), tells the owner where, and points with a glint trail. */
        /** Returns the ore it found (null if none). */
        @Nullable
        BlockPos sense(ServerLevel level, Player owner) {
            int r = ExofallConfig.PET_SENSE_RADIUS.get();
            BlockPos me = mob.blockPosition();
            BlockPos best = null;
            boolean bestGold = false;
            double bestD = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -r, -r), me.offset(r, r, r))) {
                BlockState s = level.getBlockState(p);
                if (!s.is(Tags.Blocks.ORES)) continue;
                boolean gold = s.is(Tags.Blocks.ORES_GOLD);
                double d = p.distSqr(me);
                if (gold && !bestGold || gold == bestGold && d < bestD) {
                    best = p.immutable();
                    bestGold = gold;
                    bestD = d;
                }
            }
            if (best == null) return null;
            BlockPos d = best.subtract(me);
            Component where = direction(d.getX(), d.getY(), d.getZ());
            owner.displayClientMessage(Component.translatable("pet.exofall.scarab.sense", mob.getDisplayName(),
                    level.getBlockState(best).getBlock().getName(), (int) Math.round(Math.sqrt(bestD)), where), true);
            Vec3 from = mob.position().add(0, mob.getBbHeight() * 0.6, 0), dir = Vec3.atCenterOf(best).subtract(from).normalize();
            for (int i = 1; i <= 8; i++) {
                Vec3 p = from.add(dir.scale(i * 0.4));
                level.sendParticles(ParticleTypes.WAX_ON, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            }
            level.playSound(null, mob, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0f, 1.5f);
            return best;
        }

        /** "below, north-east" style direction words. */
        static Component direction(int dx, int dy, int dz) {
            String v = dy < -1 ? "below" : dy > 1 ? "above" : "level";
            String h;
            if (dx * dx + dz * dz <= 2) h = "here";
            else {
                double a = Math.toDegrees(Math.atan2(dx, -dz));   // 0 = north (-z), 90 = east (+x)
                String[] names = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
                h = names[(int) Math.floorMod(Math.round(a / 45.0), 8)];
            }
            return Component.translatable("pet.exofall.dir." + v, Component.translatable("pet.exofall.dir." + h));
        }
    }

    // ---------------------------------------------------------------- fighters

    /** Owner-hurt-by (true) or owner-hurt (false): attack whatever is fighting the owner. */
    static final class OwnerTargetGoal extends TargetGoal {
        private final Pet pet;
        private final boolean defend;
        @Nullable private LivingEntity found;
        private int lastStamp = Integer.MIN_VALUE;

        OwnerTargetGoal(Mob mob, Pet pet, boolean defend) {
            super(mob, false);
            this.pet = pet;
            this.defend = defend;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            PetState st = pet.petState();
            if (!st.tamed || st.mode == PetState.Mode.STAY) return false;
            Player owner = Pets.owner(mob, pet);
            if (owner == null || owner.level() != mob.level()) return false;
            LivingEntity t = defend ? owner.getLastHurtByMob() : owner.getLastHurtMob();
            int stamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
            if (t == null || stamp == lastStamp || !t.isAlive() || Pets.sameSide(pet, t) || Pets.refuses(pet, t)) return false;
            if (mob.distanceToSqr(t) > 32 * 32) return false;
            found = t;
            return canAttack(t, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(found);
            Player owner = Pets.owner(mob, pet);
            if (owner != null) lastStamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Nursemaid: flies to the most hurt of her owner and the owner's pets and feeds them wax. */
    static final class NurseGoal extends Goal {
        private final PathfinderMob mob;
        private final Pet pet;
        @Nullable private LivingEntity patient;
        private int ticks, searchDelay;

        NurseGoal(PathfinderMob mob, Pet pet) {
            this.mob = mob;
            this.pet = pet;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean working() {
            return pet.isTamedPet() && pet.petState().mode != PetState.Mode.STAY && !mob.isBaby();
        }

        @Override
        public boolean canUse() {
            if (!working() || --searchDelay > 0) return false;
            searchDelay = 10;
            double r = ExofallConfig.PET_HEAL_RANGE.get();
            patient = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(r),
                            e -> e != mob && e.isAlive() && e.getHealth() < e.getMaxHealth() && Pets.sameSide(pet, e)
                                    && !(e instanceof Player p && (p.isSpectator() || p.isCreative())))
                    .stream().min(Comparator.comparingDouble(e -> e.getHealth() / e.getMaxHealth())).orElse(null);
            return patient != null;
        }

        @Override
        public boolean canContinueToUse() {
            return working() && patient != null && patient.isAlive() && patient.getHealth() < patient.getMaxHealth() && ticks < 400
                    && mob.distanceToSqr(patient) < 32 * 32;
        }

        @Override public void start() { ticks = 0; }
        @Override public void stop() { patient = null; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (patient == null) return;
            ticks++;
            mob.getLookControl().setLookAt(patient, 30, 30);
            if (mob.distanceTo(patient) > 2.5) {
                mob.getMoveControl().setWantedPosition(patient.getX(), patient.getY() + patient.getBbHeight() + 0.8, patient.getZ(), 1.0);
            } else if (ticks % ExofallConfig.PET_HEAL_INTERVAL.get() == 0 && mob.level() instanceof ServerLevel sl) {
                patient.heal((patient instanceof Player ? ExofallConfig.PET_HEAL_PLAYER.get() : ExofallConfig.PET_HEAL_PET.get()).floatValue());
                if (mob instanceof software.bernie.geckolib.animatable.GeoEntity g) g.triggerAnim("action", "tend");
                mob.playSound(SoundEvents.HONEY_DRINK, 0.8f, 1.3f);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getY() + patient.getBbHeight() * 0.6, patient.getZ(),
                        6, 0.4, 0.4, 0.4, 0);
            }
        }
    }

    private PetGoals() {}
}
