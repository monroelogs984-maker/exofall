package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Meteor soil. Alien plants only grow on these, so they never spread in the Overworld. */
    public static final TagKey<Block> ALIEN_SOIL = TagKey.create(Registries.BLOCK, Exofall.id("alien_soil"));

    private ModTags() {}
}
