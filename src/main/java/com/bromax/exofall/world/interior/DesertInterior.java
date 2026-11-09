package com.bromax.exofall.world.interior;

import com.bromax.exofall.world.MeteorInterior;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Beetle desert: dune floor of sand and red sand over sandstone with buried bone, banded sandstone pillars. */
public class DesertInterior implements MeteorInterior {
    private static final int PILLAR_GRID = 12;

    @Override
    public Column column(MeteorPiece m, int x, int z) {
        int ft = m.baseFloor() + 2 + (int) Math.floor(4.5 * m.noiseA(x * 0.045, 1.1, z * 0.045) + 1.5 * m.noiseA(x * 0.13, 5.2, z * 0.13));
        return new Column(ft, pillar(m, x, z) ? 1 : 0, m.hash(x, 0, z, 9), null);
    }

    private static boolean pillar(MeteorPiece m, int x, int z) {
        int gx = Math.floorDiv(x, PILLAR_GRID), gz = Math.floorDiv(z, PILLAR_GRID);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int cx = gx + i, cz = gz + j;
                if (m.hash(cx, 0, cz, 20) > 0.5) continue;
                double px = cx * PILLAR_GRID + 2 + m.hash(cx, 1, cz, 21) * (PILLAR_GRID - 4);
                double pz = cz * PILLAR_GRID + 2 + m.hash(cx, 2, cz, 22) * (PILLAR_GRID - 4);
                double r = 1.6 + m.hash(cx, 3, cz, 23) * 1.4;
                double dx = x + 0.5 - px, dz = z + 0.5 - pz;
                if (dx * dx + dz * dz <= r * r) return true;
            }
        }
        return false;
    }

    @Override
    public BlockState block(MeteorPiece m, Column col, int x, int y, int z) {
        int ft = col.floorTop();
        if (y <= ft) {
            if (ft - y < 3) {
                return (m.noiseB(x * 0.06, 2.0, z * 0.06) > 0.35 ? Blocks.RED_SAND : Blocks.SAND).defaultBlockState();
            }
            if (m.noiseB(x * 0.2, y * 0.2, z * 0.2) > 0.55) return Blocks.BONE_BLOCK.defaultBlockState();
            return Blocks.SANDSTONE.defaultBlockState();
        }
        if (col.a() > 0) {
            return (Math.floorMod(y, 5) == 0 ? Blocks.CHISELED_SANDSTONE : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
        }
        double colHash = col.b();
        if (colHash < 0.006 && y <= ft + 1 + (int) (colHash * 400)) return Blocks.CACTUS.defaultBlockState(); // 1-3 tall
        if (y == ft + 1 && m.hash(x, y, z, 2) < 0.04) return Blocks.DEAD_BUSH.defaultBlockState();
        if (m.hash(x, y, z, 4) < 0.03 && m.classify(x, y + 1, z) == MeteorPiece.CRUST) return Blocks.OCHRE_FROGLIGHT.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
}
