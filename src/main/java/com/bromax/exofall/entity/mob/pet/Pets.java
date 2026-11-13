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
    public static final int TAME_ODDS = 3, LOVE_TICKS = 600, COOLDOWN = 6000, GROW_TICKS = 24000;

    public interface Breeder {
        /** Makes a baby of this mob's type (not yet in the world). */
        Mob makeBaby(ServerLevel level);
    }

    /** Right-click with a treat. Returns PASS when it isn't this pet's treat, so the mob's own interaction runs. */
    public static InteractionResult interact(Mob mob, Pet pet, Player player, InteractionHand hand, boolean baby) {
        PetPair pair = PetPair.of(mob.getType());
        ItemStack stack = player.getItemInHand(hand);
        if (pair == null || !stack.is(pair.treat())) return InteractionResult.PASS;
        PetState st = pet.petState();
        if (mob.level().isClientSide) return InteractionResult.SUCCESS;
        if (!st.tamed) {
            use(player, stack);
            if (mob.getRandom().nextInt(TAME_ODDS) == 0) {
                st.tamed = true;
                mob.setPersistenceRequired();
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
            st.age = Math.min(0, st.age + GROW_TICKS / 10);
            particles(mob, true);
            return InteractionResult.CONSUME;
        }
        if (st.love > 0 || st.cooldown > 0) return InteractionResult.PASS;
        use(player, stack);
        st.love = LOVE_TICKS;
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
        bs.age = -GROW_TICKS;
        baby.setPersistenceRequired();
        baby.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0);
        level.addFreshEntityWithPassengers(baby);
        if (baby instanceof BabySize b) b.setBabySize(true);
        for (Mob parent : new Mob[]{mob, mate}) {
            PetState ps = ((Pet) parent).petState();
            ps.love = 0;
            ps.cooldown = COOLDOWN;
        }
        particles(mob, true);
        level.addFreshEntity(new ExperienceOrb(level, mob.getX(), mob.getY(), mob.getZ(), 1 + mob.getRandom().nextInt(7)));
    }

    private static void particlesSoft(ServerLevel level, Mob mob) {
        level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 1, 0.2, 0.1, 0.2, 0);
    }

    /** A tamed pet won't target players or other tamed pets. */
    public static boolean refuses(Pet pet, LivingEntity target) {
        if (!pet.isTamedPet() || target == null) return false;
        return target instanceof Player || target instanceof Pet p && p.isTamedPet();
    }

    /** Implemented by pets whose baby size is synced data. */
    public interface BabySize {
        void setBabySize(boolean baby);
    }

    private Pets() {}
}
