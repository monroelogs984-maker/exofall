package com.bromax.exofall.world.interior;

import com.bromax.exofall.registry.ModBlocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Small helpers shared by the interiors. */
final class Interiors {
    /** Scan distance for ceilings: floor sits ~23 blocks under the centre, the shell top up to ~90 above. */
    static final int CEILING_SCAN = 160;
    /** Surface layers keep their meteor's material; anything deeper is Chitinstone. */
    static final int SURFACE_DEPTH = 3;

    static BlockState chitinstone() {
        return ModBlocks.CHITINSTONE.get().defaultBlockState();
    }

    static BlockState glowmarl() {
        return ModBlocks.GLOWMARL.get().defaultBlockState();
    }

    static BlockState lower(DoublePlantBlock b) {
        return b.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
    }

    static BlockState upper(DoublePlantBlock b) {
        return b.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
    }

    private Interiors() {}
}
