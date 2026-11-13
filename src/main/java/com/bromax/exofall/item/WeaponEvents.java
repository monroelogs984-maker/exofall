package com.bromax.exofall.item;

import com.bromax.exofall.Exofall;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class WeaponEvents {
    /** Royal Grandhammer lifesteal: heal for a share of the melee damage that actually landed. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return;
        ItemStack held = attacker.getMainHandItem();
        if (held.getItem() instanceof ExofallWeaponItem w && w.kind == ExofallWeaponItem.Kind.GRANDHAMMER
                && event.getNewDamage() > 0) {
            attacker.heal(event.getNewDamage() * com.bromax.exofall.config.ExofallConfig.GRANDHAMMER_LIFESTEAL.get().floatValue());
        }
    }

    private WeaponEvents() {}
}
