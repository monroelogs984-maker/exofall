package com.bromax.exofall.entity.mob.pet;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.config.ExofallConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Tamed bugs are tougher than wild ones: max health and (fighters) melee damage times the config multipliers. Permanent
 * modifiers, so the extra health survives a reload (health is read after attributes); re-applied on every load, so a
 * config change takes effect.
 */
public final class PetStats {
    public static final ResourceLocation HEALTH = Exofall.id("pet_health"), DAMAGE = Exofall.id("pet_damage");

    public static void apply(Mob mob) {
        if (!(mob instanceof Pet pet) || !pet.isTamedPet()) return;
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        boolean fresh = hp != null && !hp.hasModifier(HEALTH);
        set(hp, HEALTH, ExofallConfig.PET_HEALTH_MULTIPLIER.get());
        PetRole role = PetRole.of(mob.getType());
        if (role != null && role.fighter) set(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE, ExofallConfig.PET_DAMAGE_MULTIPLIER.get());
        if (hp != null) hp.removeModifier(com.bromax.exofall.config.MobScaling.HEALTH);   // pets use the pet multiplier, not the meteor-mob one
        if (fresh) mob.setHealth(mob.getMaxHealth());
        else if (mob.getHealth() > mob.getMaxHealth()) mob.setHealth(mob.getMaxHealth());
    }

    private static void set(AttributeInstance attr, ResourceLocation id, double multiplier) {
        if (attr == null) return;
        attr.addOrReplacePermanentModifier(new AttributeModifier(id, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private PetStats() {}
}
