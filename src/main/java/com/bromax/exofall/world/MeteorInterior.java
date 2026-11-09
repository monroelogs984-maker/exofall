package com.bromax.exofall.world;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Fills a meteor's interior. Implementations must be stateless and deterministic: the same
 * (meteor, x, y, z) always gives the same block, whichever chunk asks first.
 */
public interface MeteorInterior {
    /** Per-column data computed once per (x, z), e.g. floor height. */
    record Column(int floorTop, double a, double b, Object extra) {}

    Column column(MeteorPiece m, int x, int z);

    /** Block for an interior cell (never called for crust or outside cells). */
    BlockState block(MeteorPiece m, Column col, int x, int y, int z);
}
