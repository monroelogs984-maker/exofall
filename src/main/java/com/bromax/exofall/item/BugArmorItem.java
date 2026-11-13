package com.bromax.exofall.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Armour for a tamed meteor bug, one per meteor armour set. Any of the eight pets can wear any of them (right-click with
 * it; shears take it off). Worn in the body slot (like wolf armour), so the armour values and the drop on death come from
 * vanilla; the small perks live in entity/mob/pet/PetEvents and PetGoals. Never wears out.
 */
public class BugArmorItem extends ArmorItem {
    private final ArmorSet set;

    public BugArmorItem(ArmorSet set, Properties props) {
        super(set.material(), Type.BODY, props);
        this.set = set;
    }

    public ArmorSet set() {
        return set;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.exofall.bug_armour.desc").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.exofall." + set.id + "_bug_armour.perk").withStyle(ChatFormatting.GRAY));
    }
}
