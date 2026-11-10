package com.bromax.exofall.item;

import com.bromax.exofall.Exofall;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Tool tiers. Meteor crust is in {@link #NEEDS_METEORIC_TOOL}, which is added to every vanilla
 * {@code incorrect_for_*_tool} tag (netherite included), so only the meteoric tier drops it.
 */
public final class ModTiers {
    public static final TagKey<Block> NEEDS_METEORIC_TOOL = TagKey.create(Registries.BLOCK, Exofall.id("needs_meteoric_tool"));
    public static final TagKey<Block> INCORRECT_FOR_METEORIC_TOOL = TagKey.create(Registries.BLOCK, Exofall.id("incorrect_for_meteoric_tool"));

    /** One step above netherite (2031 uses, speed 9, +4 damage, enchant 15). Placeholder numbers until gear balancing. */
    public static final Tier METEORIC = new SimpleTier(INCORRECT_FOR_METEORIC_TOOL,
            3000, 10.0F, 4.0F, 15, () -> Ingredient.of(Items.ECHO_SHARD));

    private ModTiers() {}
}
