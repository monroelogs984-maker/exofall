package com.bromax.exofall.world.interior;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.world.MeteorInterior;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Hemlock rainforest: Glowmarl, podzol and moss over dirt, a few pools, Umbrella Ferns and Mistcaps, tall
 * conical Veilpines, and Glowmoss Curtains from the ceiling. Trees are built block by block from a
 * jittered grid so they cross chunk borders cleanly.
 */
public class RainforestInterior implements MeteorInterior {
    private static final int TREE_GRID = 8, TREE_REACH = 4;

    private record Tree(int x, int z, int base, int top) {}

    private record Extra(List<Tree> trees, int ceiling, int vineLength, int topKind) {}

    @Override
    public Column column(MeteorPiece m, int x, int z) {
        int ft = floorTop(m, x, z);
        List<Tree> trees = new ArrayList<>(2);
        int gx0 = Math.floorDiv(x - TREE_REACH, TREE_GRID), gx1 = Math.floorDiv(x + TREE_REACH, TREE_GRID);
        int gz0 = Math.floorDiv(z - TREE_REACH, TREE_GRID), gz1 = Math.floorDiv(z + TREE_REACH, TREE_GRID);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                Tree t = tree(m, gx, gz);
                if (t != null && Math.abs(t.x - x) <= TREE_REACH && Math.abs(t.z - z) <= TREE_REACH) trees.add(t);
            }
        }
        int ceiling = m.ceilingAbove(x, ft, z, Interiors.CEILING_SCAN);
        double vh = m.hash(x, 0, z, 30);
        int vineLength = vh < 0.03 ? 3 + (int) (vh / 0.03 * 10) : 0;
        double kind = m.noiseB(x * 0.09, 6.6, z * 0.09);
        int topKind = kind > 0.35 ? 1 : kind < -0.4 ? 2 : 0; // 0 glowmarl, 1 podzol, 2 moss
        return new Column(ft, 0, 0, new Extra(trees, ceiling, vineLength, topKind));
    }

    private static int floorTop(MeteorPiece m, int x, int z) {
        return m.baseFloor() + 1 + (int) Math.floor(3.0 * m.noiseA(x * 0.06, 4.4, z * 0.06));
    }

    private static Tree tree(MeteorPiece m, int gx, int gz) {
        long key = ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
        Object cached = m.cache.get(key);
        if (cached != null) return cached instanceof Tree t ? t : null;
        Tree result = null;
        if (m.hash(gx, 0, gz, 40) < 0.7) {
            int tx = gx * TREE_GRID + 1 + (int) (m.hash(gx, 1, gz, 41) * (TREE_GRID - 2));
            int tz = gz * TREE_GRID + 1 + (int) (m.hash(gx, 2, gz, 42) * (TREE_GRID - 2));
            int ft = floorTop(m, tx, tz);
            if (ft > m.baseFloor() && m.classify(tx, ft + 1, tz) == MeteorPiece.INTERIOR) {
                int ceiling = m.ceilingAbove(tx, ft, tz, Interiors.CEILING_SCAN);
                int base = ft + 1;
                int top = base + 10 + (int) (m.hash(gx, 3, gz, 43) * 9);
                if (ceiling > 0) top = Math.min(top, ceiling - 3);
                if (top - base >= 5) result = new Tree(tx, tz, base, top);
            }
        }
        m.cache.put(key, result == null ? Boolean.FALSE : result);
        return result;
    }

    @Override
    public BlockState block(MeteorPiece m, Column col, int x, int y, int z) {
        int ft = col.floorTop(), water = m.baseFloor();
        Extra e = (Extra) col.extra();
        if (y <= ft) {
            if (y == ft) {
                if (y < water) return Blocks.MUD.defaultBlockState();
                return switch (e.topKind()) {
                    case 1 -> Blocks.PODZOL.defaultBlockState();
                    case 2 -> Blocks.MOSS_BLOCK.defaultBlockState();
                    default -> Interiors.glowmarl();
                };
            }
            if (ft - y >= Interiors.SURFACE_DEPTH) return Interiors.chitinstone();
            return Blocks.DIRT.defaultBlockState();
        }
        if (y <= water) return Blocks.WATER.defaultBlockState();

        for (Tree t : e.trees()) {
            if (x == t.x && z == t.z && y >= t.base && y <= t.top) return ModBlocks.VEILPINE.log.get().defaultBlockState();
        }
        for (Tree t : e.trees()) {
            if (leaf(m, t, x, y, z)) return ModBlocks.VEILPINE.leaves.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        }

        int ceil = e.ceiling();
        if (ceil > 0) {
            if (e.vineLength() > 0 && y < ceil && y >= ceil - e.vineLength()) {
                return ModBlocks.GLOWMOSS_CURTAIN.get().defaultBlockState();
            }
            if (y == ceil - 1 && m.hash(x, y, z, 4) < 0.008) return Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
        }

        if (ft >= water && e.topKind() == 0) { // Glowmarl: alien plants
            double h = m.hash(x, ft + 1, z, 2);
            if (h < 0.08) {
                if (y == ft + 1) return Interiors.lower(ModBlocks.UMBRELLA_FERN.get());
                if (y == ft + 2) return Interiors.upper(ModBlocks.UMBRELLA_FERN.get());
            } else if (h < 0.13 && y == ft + 1) {
                return ModBlocks.MISTCAP.get().defaultBlockState();
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    /** Tiered cone: radius grows downward from the tip, every third layer pulled in for the drooping look. */
    private static boolean leaf(MeteorPiece m, Tree t, int x, int y, int z) {
        if (y < t.base + 3 || y > t.top + 1) return false;
        int depth = t.top + 1 - y;
        double r = Math.min(3.6, 0.5 + depth * 0.33);
        if (depth % 3 == 2) r *= 0.55;
        int dx = x - t.x, dz = z - t.z;
        if (dx == 0 && dz == 0) return y == t.top + 1;
        return dx * dx + dz * dz <= r * r && m.hash(x, y, z, 44) < 0.92;
    }
}
