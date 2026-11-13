package com.bromax.exofall.config;

import com.bromax.exofall.Exofall;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the config's gear multipliers: Exofall weapons' attack damage (the whole hit, fist included) and Exofall
 * armour's armour and toughness. Done when stacks build their attributes, so a config change applies at once.
 */
@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class GearScaling {
    @SubscribeEvent
    public static void scale(ItemAttributeModifierEvent event) {
        Item item = event.getItemStack().getItem();
        if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Exofall.MOD_ID)) return;
        boolean weapon = item instanceof SwordItem, armour = item instanceof ArmorItem;
        if (!weapon && !armour) return;
        double dmg = ExofallConfig.GEAR_DAMAGE_MULTIPLIER.get(), arm = ExofallConfig.GEAR_ARMOR_MULTIPLIER.get();
        if (dmg == 1.0 && arm == 1.0) return;
        List<Runnable> changes = new ArrayList<>();
        for (var e : event.getModifiers()) {
            AttributeModifier m = e.modifier();
            double v = m.amount();
            if (weapon && e.attribute().is(Attributes.ATTACK_DAMAGE) && m.operation() == AttributeModifier.Operation.ADD_VALUE) {
                v = (v + 1) * dmg - 1;   // the base 1 (fist) is part of the hit, so scale the whole hit
            } else if (armour && (e.attribute().is(Attributes.ARMOR) || e.attribute().is(Attributes.ARMOR_TOUGHNESS))) {
                v = v * arm;
            } else continue;
            AttributeModifier scaled = new AttributeModifier(m.id(), v, m.operation());
            var attr = e.attribute();
            var slot = e.slot();
            changes.add(() -> event.replaceModifier(attr, scaled, slot));
        }
        changes.forEach(Runnable::run);
    }

    private GearScaling() {}
}
