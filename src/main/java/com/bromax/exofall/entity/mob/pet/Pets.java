package com.bromax.exofall.entity.mob.pet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.bromax.exofall.config.ExofallConfig;
import com.bromax.exofall.item.BugArmorItem;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Taming and breeding for the eight {@link PetPair} bugs (most aren't vanilla animals, so this is done by hand):
 * <ul>
 *   <li>Feed a wild one its pair's treat: 1 in 3 tames it. Tamed bugs are passive (they never target players or other
 *       tamed bugs) and never despawn.</li>
 *   <li>Feed two tamed adults of one species: they find each other and make a tamed baby (half size, grows up in one
 *       Minecraft day). Then 5 minutes before they breed again.</li>
 * </ul>
 * Each entity calls {@link #interact} from mobInteract, {@link #tick} from its server tick, {@link #refuses} from
 * setTarget, and saves {@link PetState}. Baby size goes through {@code setBaby}.
 */
public final class Pets {
    private static int tameOdds() { return ExofallConfig.PET_TAME_ODDS.get(); }
    private static int loveTicks() { return ExofallConfig.PET_LOVE_TICKS.get(); }
    private static int cooldown() { return ExofallConfig.PET_BREED_COOLDOWN.get(); }
    public static int growTicks() { return ExofallConfig.PET_GROW_TICKS.get(); }

    public interface Breeder {
        /** Makes a baby of this mob's type (not yet in the world). */
        Mob makeBaby(ServerLevel level);
    }

    /** Right-click with a treat. Returns PASS when it isn't this pet's treat, so the mob's own interaction runs. */
    /** Grants one of Exofall's code-awarded advancements (criterion trigger "minecraft:impossible"). */
    public static void award(Player player, String advancement, String criterion) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp) || sp.getServer() == null) return;
        var adv = sp.getServer().getAdvancements().get(com.bromax.exofall.Exofall.id(advancement));
        if (adv != null) sp.getAdvancements().award(adv, criterion);
    }

    /** Last player who fed this pet a treat, so a birth can credit them. */
    private static final java.util.Map<java.util.UUID, java.util.UUID> FEEDER = new java.util.concurrent.ConcurrentHashMap<>();

    public static InteractionResult interact(Mob mob, Pet pet, Player player, InteractionHand hand, boolean baby) {
        PetPair pair = PetPair.of(mob.getType());
        ItemStack stack = player.getItemInHand(hand);
        if (pair == null) return InteractionResult.PASS;
        if (!stack.is(pair.treat())) return ownerInteract(mob, pet, player, hand);
        PetState st = pet.petState();
        if (mob.level().isClientSide) return InteractionResult.SUCCESS;
        if (!st.tamed) {
            use(player, stack);
            if (mob.getRandom().nextInt(tameOdds()) == 0) {
                tame(mob, pet, player);
                award(player, "tame", "tamed");
                mob.setTarget(null);
                mob.getNavigation().stop();
                particles(mob, true);
            } else {
                particles(mob, false);
            }
            return InteractionResult.CONSUME;
        }
        if (baby) {   // treats speed a baby up, like vanilla animals
            use(player, stack);
            st.age = Math.min(0, st.age + growTicks() / 10);
            particles(mob, true);
            return InteractionResult.CONSUME;
        }
        if (st.love > 0 || st.cooldown > 0) return InteractionResult.PASS;
        use(player, stack);
        st.love = loveTicks();
        FEEDER.put(mob.getUUID(), player.getUUID());
        particles(mob, true);
        return InteractionResult.CONSUME;
    }

    private static void use(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    /** Hearts on success, smoke on a failed taming try. */
    public static void particles(Mob mob, boolean hearts) {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(hearts ? ParticleTypes.HEART : ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.8, mob.getZ(),
                hearts ? 5 : 7, mob.getBbWidth() * 0.4, 0.3, mob.getBbWidth() * 0.4, 0.02);
    }

    /** Server tick: growing up, cooldowns, and looking for a mate while in the mood. */
    public static void tick(Mob mob, Pet pet, Breeder breeder, Consumer<Boolean> setBaby) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        PetState st = pet.petState();
        if (st.age < 0 && ++st.age == 0) setBaby.accept(false);
        if (st.cooldown > 0) st.cooldown--;
        if (st.love <= 0) return;
        st.love--;
        if (mob.tickCount % 10 == 0) particlesSoft(level, mob);
        Mob mate = null;
        for (Mob m : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(8), e -> e != mob && e.getType() == mob.getType())) {
            if (m instanceof Pet p && p.petState().love > 0 && p.petState().tamed) { mate = m; break; }
        }
        if (mate == null) return;
        mob.getNavigation().moveTo(mate, 1.0);
        mob.getLookControl().setLookAt(mate);
        if (mob.distanceTo(mate) > 2.5 || mob.getId() > mate.getId()) return; // the lower id does the birth, once
        Mob baby = breeder.makeBaby(level);
        if (baby == null) return;
        PetState bs = ((Pet) baby).petState();
        bs.tamed = true;
        bs.owner = pet.petState().owner;
        bs.age = -growTicks();
        baby.setPersistenceRequired();
        baby.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0);
        level.addFreshEntityWithPassengers(baby);
        if (baby instanceof BabySize b) b.setBabySize(true);
        PetStats.apply(baby);
        for (Mob parent : new Mob[]{mob, mate}) {
            PetState ps = ((Pet) parent).petState();
            ps.love = 0;
            ps.cooldown = cooldown();
        }
        particles(mob, true);
        java.util.UUID who = FEEDER.remove(mob.getUUID());
        if (who != null && level.getPlayerByUUID(who) instanceof Player p) award(p, "breed", "bred");
        level.addFreshEntity(new ExperienceOrb(level, mob.getX(), mob.getY(), mob.getZ(), 1 + mob.getRandom().nextInt(7)));
    }

    private static void particlesSoft(ServerLevel level, Mob mob) {
        level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 1, 0.2, 0.1, 0.2, 0);
    }

    /** A tamed pet won't target players, other tamed pets, or anything else its owner owns (wolves, cats...). */
    public static boolean refuses(Pet pet, LivingEntity target) {
        if (!pet.isTamedPet() || target == null) return false;
        if (target instanceof Player || target instanceof Pet p && p.isTamedPet()) return true;
        return target instanceof OwnableEntity o && o.getOwnerUUID() != null && o.getOwnerUUID().equals(pet.petState().owner);
    }

    // ---- owners

    /** Makes it a tamed pet of {@code player}: persistent, passive, tamed stats. */
    public static void tame(Mob mob, Pet pet, Player player) {
        PetState st = pet.petState();
        st.tamed = true;
        st.owner = player.getUUID();
        st.mode = PetState.Mode.FOLLOW;
        mob.setPersistenceRequired();
        PetStats.apply(mob);
    }

    @Nullable
    public static Player owner(Mob mob, Pet pet) {
        UUID id = pet.petState().owner;
        return id == null ? null : mob.level().getPlayerByUUID(id);
    }

    /** Is {@code e} owned by the same player as this pet (the owner, one of the owner's pets, or a vanilla tamable)? */
    public static boolean sameSide(Pet pet, @Nullable net.minecraft.world.entity.Entity e) {
        UUID owner = pet.petState().owner;
        if (owner == null || e == null) return false;
        if (e.getUUID().equals(owner)) return true;
        if (e instanceof Pet p && p.isTamedPet() && owner.equals(p.petState().owner)) return true;
        return e instanceof OwnableEntity o && owner.equals(o.getOwnerUUID());
    }

    /**
     * The owner's right-click without a treat: bug armour puts it on (adults only), shears take it off, an empty hand
     * cycles Follow / Stay / Wander. An ownerless tamed pet (tamed before owners existed) is claimed by whoever clicks it.
     */
    public static InteractionResult ownerInteract(Mob mob, Pet pet, Player player, InteractionHand hand) {
        PetState st = pet.petState();
        if (!st.tamed) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        boolean armour = stack.getItem() instanceof BugArmorItem, shears = stack.is(Items.SHEARS);
        if (!armour && !shears && !stack.isEmpty()) return InteractionResult.PASS;
        if (st.owner != null && !st.owner.equals(player.getUUID())) {
            if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND && !mob.level().isClientSide)
                player.displayClientMessage(Component.translatable("pet.exofall.not_yours", mob.getDisplayName()), true);
            return stack.isEmpty() ? InteractionResult.sidedSuccess(mob.level().isClientSide) : InteractionResult.PASS;
        }
        if (mob.level().isClientSide) return InteractionResult.SUCCESS;
        if (st.owner == null) st.owner = player.getUUID();
        if (armour) {
            if (mob.isBaby()) {
                player.displayClientMessage(Component.translatable("pet.exofall.too_young"), true);
                return InteractionResult.CONSUME;
            }
            ItemStack old = mob.getItemBySlot(EquipmentSlot.BODY);
            mob.setItemSlot(EquipmentSlot.BODY, stack.copyWithCount(1));
            mob.setGuaranteedDrop(EquipmentSlot.BODY);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!old.isEmpty() && !player.getInventory().add(old)) mob.spawnAtLocation(old);
            mob.level().playSound(null, mob, ((BugArmorItem) mob.getItemBySlot(EquipmentSlot.BODY).getItem()).getEquipSound().value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            return InteractionResult.CONSUME;
        }
        if (shears) {
            ItemStack old = mob.getItemBySlot(EquipmentSlot.BODY);
            if (old.isEmpty()) return InteractionResult.PASS;
            mob.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
            if (!player.getInventory().add(old)) mob.spawnAtLocation(old);
            mob.level().playSound(null, mob, SoundEvents.ARMOR_UNEQUIP_WOLF, SoundSource.NEUTRAL, 1.0f, 1.0f);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            return InteractionResult.CONSUME;
        }
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        setMode(mob, st, st.mode.next());
        player.displayClientMessage(Component.translatable("pet.exofall.mode." + st.mode.name().toLowerCase(java.util.Locale.ROOT),
                mob.getDisplayName(), (int) Math.ceil(mob.getHealth()), (int) Math.ceil(mob.getMaxHealth())), true);
        return InteractionResult.CONSUME;
    }

    public static void setMode(Mob mob, PetState st, PetState.Mode mode) {
        st.mode = mode;
        if (mode == PetState.Mode.STAY) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    /** Implemented by pets whose baby size is synced data. */
    public interface BabySize {
        void setBabySize(boolean baby);
    }

    private Pets() {}
}
