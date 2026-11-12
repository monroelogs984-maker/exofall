package com.bromax.exofall.world.interior;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.world.MeteorInterior;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Beetle desert: sand and red-sand dunes over Chitinstone with buried bone. The troughs between dunes are
 * Glowmarl flats where everything grows: Rattlegrass, Dunespine, Nightbloom and spiralling Ribwood trees.
 * Banded Chitinstone pillars hold up the dome.
 */
public class DesertInterior implements MeteorInterior {
    private static final int PILLAR_GRID = 12;

    /** Ribwood: two log strands twisting round each other, with a crown of 4 ribs curling out and down. */
    private record Tree(int x, int z, int base, int top, double phase) implements TreeGrid.Placed {}

    private record Extra(List<Tree> trees, int plant) {}

    private static final TreeGrid<Tree> TREES = new TreeGrid<>(14, 6, 0.45, 300, DesertInterior::makeTree);

    private static int floorTop(MeteorPiece m, int x, int z) {
        return m.baseFloor() + 2 + (int) Math.floor(4.5 * m.noiseA(x * 0.045, 1.1, z * 0.045) + 1.5 * m.noiseA(x * 0.13, 5.2, z * 0.13));
    }

    private static boolean flat(MeteorPiece m, int ft) {
        return ft <= m.baseFloor() + 1;
    }

    private static Tree makeTree(MeteorPiece m, int tx, int tz) {
        int ft = floorTop(m, tx, tz);
        if (!flat(m, ft) || pillar(m, tx, tz)) return null;
        int base = ft + 1, top = base + 8 + (int) (m.hash(tx, 1, tz, 301) * 7);
        int ceiling = m.ceilingAbove(tx, ft, tz, Interiors.CEILING_SCAN);
        if (ceiling > 0) top = Math.min(top, ceiling - 4);
        if (top - base < 6) return null;
        return new Tree(tx, tz, base, top, m.hash(tx, 2, tz, 302) * Math.PI * 2);
    }

    @Override
    public int surface(MeteorPiece m, int x, int z) {
        return floorTop(m, x, z);
    }

    @Override
    public Column column(MeteorPiece m, int x, int z) {
        int ft = floorTop(m, x, z);
        double ph = m.hash(x, 0, z, 9);
        int plant = ph < 0.12 ? 1 : ph < 0.16 ? 2 : ph < 0.19 ? 3 : 0; // 1 rattlegrass, 2 dunespine, 3 nightbloom
        return new Column(ft, pillar(m, x, z) ? 1 : 0, 0, new Extra(TREES.near(m, x, z), plant));
    }

    private static boolean pillar(MeteorPiece m, int x, int z) {
        int gx = Math.floorDiv(x, PILLAR_GRID), gz = Math.floorDiv(z, PILLAR_GRID);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int cx = gx + i, cz = gz + j;
                if (m.hash(cx, 0, cz, 20) > 0.5) continue;
                double px = cx * PILLAR_GRID + 2 + m.hash(cx, 1, cz, 21) * (PILLAR_GRID - 4);
                double pz = cz * PILLAR_GRID + 2 + m.hash(cx, 2, cz, 22) * (PILLAR_GRID - 4);
                if (com.bromax.exofall.world.ArenaGen.excludes(m, (int) px, (int) pz)) continue;
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
        Extra e = (Extra) col.extra();
        boolean flat = flat(m, ft);
        if (y <= ft) {
            if (ft - y < Interiors.SURFACE_DEPTH) {
                if (y == ft && flat) return Interiors.glowmarl();
                return (m.noiseB(x * 0.06, 2.0, z * 0.06) > 0.35 ? Blocks.RED_SAND : Blocks.SAND).defaultBlockState();
            }
            if (m.noiseB(x * 0.2, y * 0.2, z * 0.2) > 0.55) return Blocks.BONE_BLOCK.defaultBlockState();
            return Interiors.chitinstone();
        }
        if (col.a() > 0) {
            return (Math.floorMod(y, 5) == 0 ? ModBlocks.POLISHED_CHITINSTONE : ModBlocks.CHITINSTONE_PILLAR).get().defaultBlockState();
        }
        for (Tree t : e.trees()) {
            if (tree(t, x, y, z)) return ModBlocks.RIBWOOD.log.get().defaultBlockState();
        }
        if (flat && y == ft + 1) {
            switch (e.plant()) {
                case 1: return ModBlocks.RATTLEGRASS.get().defaultBlockState();
                case 2: return ModBlocks.DUNESPINE.get().defaultBlockState();
                case 3: return ModBlocks.NIGHTBLOOM.get().defaultBlockState();
                default: break;
            }
        }
        if (m.hash(x, y, z, 4) < 0.03 && m.classify(x, y + 1, z) == MeteorPiece.CRUST) return Blocks.OCHRE_FROGLIGHT.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }

    private static boolean tree(Tree t, int x, int y, int z) {
        if (y < t.base || y > t.top + 2) return false;
        int dx = x - t.x, dz = z - t.z;
        if (y < t.base + 3 && dx == 0 && dz == 0) return true; // solid foot
        if (y <= t.top) {
            double a = (y - t.base) * 0.45 + t.phase;
            for (int s = 0; s < 2; s++) {
                double b = a + s * Math.PI;
                if (dx == (int) Math.round(Math.cos(b) * 1.6) && dz == (int) Math.round(Math.sin(b) * 1.6)) return true;
            }
        }
        // Crown ribs: out 2-5 blocks, rising one block then falling away.
        for (int j = 0; j < 4; j++) {
            double b = t.phase + j * Math.PI / 2;
            for (int k = 2; k <= 5; k++) {
                int ry = t.top + (k <= 3 ? 1 : k == 4 ? 0 : -1);
                if (y == ry && dx == (int) Math.round(Math.cos(b) * k) && dz == (int) Math.round(Math.sin(b) * k)) return true;
            }
        }
        return false;
    }
}
