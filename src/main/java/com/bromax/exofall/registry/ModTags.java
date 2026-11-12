package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Meteor soil. Alien plants only grow on these, so they never spread in the Overworld. */
    public static final TagKey<Block> ALIEN_SOIL = TagKey.create(Registries.BLOCK, Exofall.id("alien_soil"));

    /** Nursemaid's Wax Charm heals its wearer while standing on these. */
    public static final TagKey<Block> WAX_BLOCKS = TagKey.create(Registries.BLOCK, Exofall.id("wax_blocks"));

    /** Leafcutter Charm: its wearer isn't slowed by these (cobwebs, berry bushes). */
    public static final TagKey<Block> LEAFCUTTER_PASSABLE = TagKey.create(Registries.BLOCK, Exofall.id("leafcutter_passable"));

    /** Leafcutter Charm: its wearer breaks these instantly (leaves, vines, webs, bushes). */
    public static final TagKey<Block> LEAFCUTTER_CUTTABLE = TagKey.create(Registries.BLOCK, Exofall.id("leafcutter_cuttable"));

    private ModTags() {}
}
