package com.bromax.exofall.world.interior;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.world.MeteorInterior;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Bug swamp: mud and moss floor, shallow pools with lily pads, bog plants, froglights on the ceiling. */
public class SwampInterior implements MeteorInterior {
    @Override
    public Column column(MeteorPiece m, int x, int z) {
        int ft = m.baseFloor() + (int) Math.floor(2.6 * m.noiseA(x * 0.07, 7.5, z * 0.07) + 0.4);
        return new Column(ft, 0, 0, null);
    }

    @Override
    public BlockState block(MeteorPiece m, Column col, int x, int y, int z) {
        int ft = col.floorTop(), water = m.baseFloor();
        if (y <= ft) {
            boolean top = y == ft && y > water;
            return (top && m.hash(x, y, z, 1) < 0.4 ? Blocks.MOSS_BLOCK : Blocks.MUD).defaultBlockState();
        }
        if (y <= water) return Blocks.WATER.defaultBlockState();
        if (y == ft + 1 && ft > water && m.hash(x, y, z, 2) < 0.16) return ModBlocks.PLACEHOLDER_BOG_PLANT.get().defaultBlockState();
        if (y == water + 1 && ft < water && m.hash(x, y, z, 3) < 0.07) return Blocks.LILY_PAD.defaultBlockState();
        if (m.hash(x, y, z, 4) < 0.025 && m.classify(x, y + 1, z) == MeteorPiece.CRUST) return Blocks.VERDANT_FROGLIGHT.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
}
