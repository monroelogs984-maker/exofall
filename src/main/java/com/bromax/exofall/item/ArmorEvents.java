package com.bromax.exofall.item;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.boss.ExofallBoss;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Armour set perks:
 * <ul>
 *   <li>Mireweave: each piece shrinks how far mobs notice you by 12.5% (full set: half range) and adds 7.5% damage
 *       against mobs that aren't targeting you (full set: +30%). Bosses see through it.</li>
 *   <li>Crystallized Honey, full set only: +15% movement speed and constant Regeneration I.</li>
 *   <li>Reinforced Carapace: each piece blocks 10% of projectile damage (full set: 40%).</li>
 *   <li>Ancientstone: -2.5% speed per piece (an item attribute, see {@link ExofallArmorItem}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class ArmorEvents {
    // perk values are in the config (config/ExofallConfig "gear.armour")
    private static final net.minecraft.resources.ResourceLocation HONEY_SPEED_ID = Exofall.id("crystallized_honey_set");

    static boolean isBoss(Entity e) {
        return e instanceof ExofallBoss || e.getType().is(Tags.EntityTypes.BOSSES);
    }

    @SubscribeEvent
    public static void visibility(LivingEvent.LivingVisibilityEvent event) {
        Entity looker = event.getLookingEntity();
        if (looker == null || isBoss(looker)) return;
        int n = ExofallArmorItem.worn(event.getEntity(), ArmorSet.MIREWEAVE);
        if (n > 0) event.modifyVisibility(1 - com.bromax.exofall.config.ExofallConfig.MIRE_SIGHT.get() * n);
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        var src = event.getSource();
        if (src.is(DamageTypeTags.IS_PROJECTILE)) {
            int n = ExofallArmorItem.worn(target, ArmorSet.REINFORCED_CARAPACE);
            if (n > 0) event.setAmount(event.getAmount() * (float) (1 - com.bromax.exofall.config.ExofallConfig.CARAPACE_BLOCK.get() * n));
        }
        if (src.getEntity() instanceof LivingEntity attacker && src.getDirectEntity() == attacker
                && target instanceof Mob mob && !isBoss(mob) && mob.getTarget() != attacker) {
            int n = ExofallArmorItem.worn(attacker, ArmorSet.MIREWEAVE);
            if (n > 0) event.setAmount(event.getAmount() * (float) (1 + com.bromax.exofall.config.ExofallConfig.MIRE_AMBUSH.get() * n));
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || p.tickCount % 10 != 0) return;
        honey(p);
    }

    /** Applies or removes the Crystallized Honey full-set bonus. Public for the self-test. */
    public static void honey(LivingEntity p) {
        boolean full = ExofallArmorItem.worn(p, ArmorSet.CRYSTALLIZED_HONEY) == 4;
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (full) speed.addOrUpdateTransientModifier(new AttributeModifier(HONEY_SPEED_ID, com.bromax.exofall.config.ExofallConfig.HONEY_SPEED.get(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            else speed.removeModifier(HONEY_SPEED_ID);
        }
        if (full && com.bromax.exofall.config.ExofallConfig.HONEY_REGEN.get()) {
            MobEffectInstance cur = p.getEffect(MobEffects.REGENERATION);
            // ticks down from 80 through 50, where Regeneration I heals, before it's topped up again
            if (cur == null || cur.getAmplifier() == 0 && cur.getDuration() < 25) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
            }
        }
    }

    private ArmorEvents() {}
}
