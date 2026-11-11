package com.bromax.exofall.entity.mob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;

/** Hive meteor mobs defend each other: hurting one sets every fighter within 16 blocks on the attacker. */
public interface HiveMob {
    /** Whether this hive mob joins fights (nursemaids and cocoons don't). */
    default boolean defendsHive() {
        return true;
    }

    static void alarm(Mob victim, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker instanceof HiveMob) return;
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        for (Mob m : victim.level().getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(16),
                e -> e instanceof HiveMob h && h.defendsHive() && e != victim && e.getTarget() == null)) {
            m.setTarget(attacker);
        }
    }
}
