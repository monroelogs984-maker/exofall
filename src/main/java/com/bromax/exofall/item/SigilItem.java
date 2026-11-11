package com.bromax.exofall.item;

import com.bromax.exofall.registry.ModItems;
import com.bromax.exofall.world.MeteorStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Opens one meteor's boss arena: used on that arena's Sigil Socket. */
public class SigilItem extends Item {
    private final MeteorStyle style;

    public SigilItem(MeteorStyle style, Properties props) {
        super(props);
        this.style = style;
    }

    public MeteorStyle style() {
        return style;
    }

    public static Component nameFor(MeteorStyle style) {
        return ModItems.SIGILS.get(style).get().getDescription();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.exofall.sigil.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
