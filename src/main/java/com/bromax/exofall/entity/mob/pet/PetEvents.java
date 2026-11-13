package com.bromax.exofall.entity.mob.pet;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.config.ExofallConfig;
import com.bromax.exofall.entity.mob.AmbushMantis;
import com.bromax.exofall.item.ArmorSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Pet combat rules that hang off events: friendly fire, the mantis first strike, bug armour perks, golems, death notice. */
@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class PetEvents {
    private static final ResourceLocation ANCIENTSTONE_KB = Exofall.id("ancientstone_bug_armour");

    private static boolean tamed(Entity e) {
        return e instanceof Pet p && p.isTamedPet() && PetPair.of(e.getType()) != null;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();
        Entity attacker = src.getEntity();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        // friendly fire: an owner and their pets can't hurt each other
        if (!ExofallConfig.PET_OWNER_FRIENDLY_FIRE.get() && attacker != null && attacker != victim) {
            if (tamed(victim) && Pets.sameSide((Pet) victim, attacker) || tamed(attacker) && Pets.sameSide((Pet) attacker, victim)) {
                event.setCanceled(true);
                return;
            }
        }
        // mantis pets: the first hit on each new target is an ambush strike
        if (attacker instanceof AmbushMantis m && tamed(m) && src.getDirectEntity() == m) {
            PetState st = m.petState();
            if (st.lastStruck != victim.getId()) {
                st.lastStruck = victim.getId();
                event.setAmount(event.getAmount() * ExofallConfig.PET_AMBUSH_MULTIPLIER.get().floatValue());
                if (m.level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            }
        }
        // Reinforced Carapace bug armour: projectile resistance
        if (tamed(victim) && victim instanceof Mob mob && PetGoals.armour(mob) == ArmorSet.REINFORCED_CARAPACE && src.is(DamageTypeTags.IS_PROJECTILE)) {
            event.setAmount(event.getAmount() * ExofallConfig.PET_CARAPACE_PROJECTILE.get().floatValue());
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity t = event.getNewAboutToBeSetTarget();
        if (t == null || !tamed(t) || !(t instanceof Mob pet)) return;
        LivingEntity hunter = event.getEntity();
        // iron and snow golems hunt Enemy mobs: not someone's pet
        if (hunter instanceof AbstractGolem) {
            event.setCanceled(true);
            return;
        }
        // Mireweave bug armour: hard to spot, unless it hit you
        if (PetGoals.armour(pet) == ArmorSet.MIREWEAVE && hunter instanceof Mob m && m.getLastHurtByMob() != t) {
            AttributeInstance range = m.getAttribute(Attributes.FOLLOW_RANGE);
            double r = (range == null ? 16 : range.getValue()) * ExofallConfig.PET_MIREWEAVE_DETECTION.get();
            if (m.distanceToSqr(t) > r * r) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEquip(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.BODY || !(event.getEntity() instanceof Mob mob) || PetPair.of(mob.getType()) == null) return;
        AttributeInstance kb = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb == null) return;
        kb.removeModifier(ANCIENTSTONE_KB);
        if (PetGoals.armour(mob) == ArmorSet.ANCIENTSTONE)
            kb.addTransientModifier(new AttributeModifier(ANCIENTSTONE_KB, ExofallConfig.PET_ANCIENTSTONE_KNOCKBACK.get(), AttributeModifier.Operation.ADD_VALUE));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!tamed(e) || e.level().isClientSide || !ExofallConfig.PET_DEATH_MESSAGE.get()) return;
        Player owner = Pets.owner((Mob) e, (Pet) e);
        if (owner != null) owner.sendSystemMessage(Component.translatable("pet.exofall.died", e.getCombatTracker().getDeathMessage()));
    }

    private PetEvents() {}
}
