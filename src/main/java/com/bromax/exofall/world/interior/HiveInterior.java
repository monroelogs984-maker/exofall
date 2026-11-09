package com.bromax.exofall.world.interior;

import com.bromax.exofall.world.MeteorInterior;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wasp hive: parallel comb sheets running floor to ceiling (like a real nest), torn open by noise
 * holes plus regular low doorways so every gap can be walked into. Sticky honey patches on the floor.
 */
public class HiveInterior implements MeteorInterior {
    private static final int SHEET_SPACING = 7, SHEET_THICKNESS = 2, DOOR_SPACING = 13, DOOR_WIDTH = 3, DOOR_HEIGHT = 4;

    @Override
    public Column column(MeteorPiece m, int x, int z) {
        int ft = m.baseFloor() + (int) Math.floor(1.5 * m.noiseA(x * 0.08, 3.3, z * 0.08));
        boolean alongX = (m.seed() & 1) == 0;
        int u = (alongX ? x - m.centre().getX() : z - m.centre().getZ());
        int v = (alongX ? z - m.centre().getZ() : x - m.centre().getX());
        boolean sheet = Mth.positiveModulo(u, SHEET_SPACING) < SHEET_THICKNESS;
        boolean door = Mth.positiveModulo(v, DOOR_SPACING) < DOOR_WIDTH;
        return new Column(ft, sheet ? 1 : 0, door ? 1 : 0, null);
    }

    @Override
    public BlockState block(MeteorPiece m, Column col, int x, int y, int z) {
        int ft = col.floorTop();
        if (y <= ft) {
            if (y == ft && m.noiseB(x * 0.15, 9.1, z * 0.15) > 0.35) return Blocks.HONEY_BLOCK.defaultBlockState();
            return Blocks.HONEYCOMB_BLOCK.defaultBlockState();
        }
        boolean sheet = col.a() > 0;
        boolean doorway = col.b() > 0 && y <= ft + DOOR_HEIGHT;
        boolean hole = m.noiseB(x * 0.09, y * 0.09, z * 0.09) > 0.28;
        if (sheet && !doorway && !hole) {
            double h = m.hash(x, y, z, 5);
            if (h < 0.004) return Blocks.BEE_NEST.defaultBlockState();
            if (h < 0.02) return Blocks.SHROOMLIGHT.defaultBlockState();
            return Blocks.HONEYCOMB_BLOCK.defaultBlockState();
        }
        if (m.hash(x, y, z, 4) < 0.03 && m.classify(x, y + 1, z) == MeteorPiece.CRUST) return Blocks.SHROOMLIGHT.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
}
