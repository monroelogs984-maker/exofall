package com.bromax.exofall.config;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.boss.ExofallBoss;
import com.bromax.exofall.entity.mob.pet.Pet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The config's mob and boss multipliers. Health: a permanent max-health modifier, refreshed every time the mob loads
 * (so a config change reaches mobs already in the world). Damage: every hit whose attacker is an Exofall mob (melee,
 * stings, spit, rolled boulders, anything with the mob as its source) is scaled, which also covers attacks that don't
 * use the attack-damage attribute. Tamed pets are skipped (they have their own multipliers).
 */
@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class MobScaling {
    public static final ResourceLocation HEALTH = Exofall.id("config_health");

    private static boolean exofall(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(Exofall.MOD_ID);
    }

    private static boolean tamed(Entity e) {
        return e instanceof Pet p && p.isTamedPet();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !exofall(mob) || tamed(mob)) return;
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        double mult = mob instanceof ExofallBoss ? ExofallConfig.BOSS_HEALTH_MULTIPLIER.get() : ExofallConfig.MOB_HEALTH_MULTIPLIER.get();
        boolean fresh = !hp.hasModifier(HEALTH);
        if (mult == 1.0) {
            hp.removeModifier(HEALTH);
        } else {
            hp.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if (fresh && mob.tickCount == 0 && !mob.isAddedToLevel()) mob.setHealth(mob.getMaxHealth());   // newly spawned: start full
        }
        if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        Entity src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker) || !exofall(attacker) || tamed(attacker)) return;
        double mult = attacker instanceof ExofallBoss ? ExofallConfig.BOSS_DAMAGE_MULTIPLIER.get() : ExofallConfig.MOB_DAMAGE_MULTIPLIER.get();
        if (mult != 1.0) event.setAmount((float) (event.getAmount() * mult));
    }

    private MobScaling() {}
}
