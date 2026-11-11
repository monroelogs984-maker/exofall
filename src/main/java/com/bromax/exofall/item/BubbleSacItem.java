package com.bromax.exofall.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Divebell Beetle air bubble. Placeholder use: pop it for 30 s of Water Breathing. */
public class BubbleSacItem extends Item {
    public BubbleSacItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.playSound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.0f);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0));
            stack.consume(1, player);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
