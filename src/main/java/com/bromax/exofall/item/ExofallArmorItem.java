package com.bromax.exofall.item;

import com.bromax.exofall.Exofall;
import com.google.common.base.Suppliers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;
import java.util.function.Supplier;

/** A piece of a meteor {@link ArmorSet}. Set perks live in {@link ArmorEvents}; Ancientstone's slowdown is per piece here. */
public class ExofallArmorItem extends ArmorItem {
    public static final double ANCIENTSTONE_SLOW = -0.025;
    private final ArmorSet set;
    private final Supplier<ItemAttributeModifiers> modifiers;

    public ExofallArmorItem(ArmorSet set, Type type, Properties props) {
        super(set.material(), type, props);
        this.set = set;
        this.modifiers = Suppliers.memoize(() -> {
            ItemAttributeModifiers base = super.getDefaultAttributeModifiers();
            if (set != ArmorSet.ANCIENTSTONE) return base;
            return base.withModifierAdded(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(Exofall.id("ancientstone_" + type.getName()), ANCIENTSTONE_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    EquipmentSlotGroup.bySlot(type.getSlot()));
        });
    }

    public ArmorSet set() {
        return set;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return modifiers.get();
    }

    /** How many pieces of {@code set} the entity wears. */
    public static int worn(LivingEntity e, ArmorSet set) {
        int n = 0;
        for (ItemStack s : e.getArmorSlots()) if (s.getItem() instanceof ExofallArmorItem a && a.set == set) n++;
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.exofall." + set.id + ".perk").withStyle(ChatFormatting.GRAY));
    }
}
