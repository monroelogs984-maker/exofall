package com.bromax.exofall.world.interior;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.world.MeteorInterior;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Bug swamp: mud floor with Glowmarl banks, shallow pools under Driftmoss and Bladderpads, Stiltreed trees
 * standing on arching roots, Sapcane and Snaretongue on the banks, Lanternpods and Weepvines from the ceiling.
 */
public class SwampInterior implements MeteorInterior {
    /** Stiltreed: trunk starts {@code ROOT_RISE} above the floor on 4 diagonal stilt roots; frond cap on top. */
    private record Tree(int x, int z, int floor, int rootTop, int top) implements TreeGrid.Placed {}

    private record Extra(List<Tree> trees, int ceiling, int vine, int plant) {}

    private static final int ROOT_RISE = 4;
    private static final TreeGrid<Tree> TREES = new TreeGrid<>(11, 4, 0.45, 100, SwampInterior::makeTree);

    private static int floorTop(MeteorPiece m, int x, int z) {
        return m.baseFloor() + (int) Math.floor(2.6 * m.noiseA(x * 0.07, 7.5, z * 0.07) + 0.4);
    }

    private static Tree makeTree(MeteorPiece m, int tx, int tz) {
        int ft = floorTop(m, tx, tz);
        int floor = Math.max(ft, m.baseFloor());
        int rootTop = floor + ROOT_RISE;
        int top = rootTop + 8 + (int) (m.hash(tx, 1, tz, 101) * 9);
        int ceiling = m.ceilingAbove(tx, floor, tz, Interiors.CEILING_SCAN);
        if (ceiling > 0) top = Math.min(top, ceiling - 4);
        if (top - rootTop < 6 || m.classify(tx, top + 2, tz) != MeteorPiece.INTERIOR) return null;
        return new Tree(tx, tz, ft, rootTop, top);
    }

    @Override
    public int surface(MeteorPiece m, int x, int z) {
        return floorTop(m, x, z);
    }

    @Override
    public Column column(MeteorPiece m, int x, int z) {
        int ft = floorTop(m, x, z);
        int water = m.baseFloor();
        int ceiling = m.ceilingAbove(x, Math.max(ft, water), z, Interiors.CEILING_SCAN);
        double vh = m.hash(x, 0, z, 30);
        int vine = vh < 0.03 ? 3 + (int) (vh / 0.03 * 10) : 0;
        double ph = m.hash(x, 0, z, 2);
        int plant = ph < 0.07 ? 1 : ph < 0.10 ? 2 : 0; // 1 sapcane, 2 snaretongue
        boolean bank = ft > water && m.noiseB(x * 0.05, 3.7, z * 0.05) > -0.1;
        return new Column(ft, bank ? 1 : 0, 0, new Extra(TREES.near(m, x, z), ceiling, vine, plant));
    }

    @Override
    public BlockState block(MeteorPiece m, Column col, int x, int y, int z) {
        int ft = col.floorTop(), water = m.baseFloor();
        Extra e = (Extra) col.extra();
        boolean bank = col.a() > 0;
        if (y <= ft) {
            if (ft - y >= Interiors.SURFACE_DEPTH) return Interiors.chitinstone();
            if (y == ft && y > water) {
                if (bank) return Interiors.glowmarl();
                return (m.hash(x, y, z, 1) < 0.4 ? Blocks.MOSS_BLOCK : Blocks.MUD).defaultBlockState();
            }
            return Blocks.MUD.defaultBlockState();
        }

        for (Tree t : e.trees()) {
            BlockState s = tree(m, t, x, y, z);
            if (s != null) return s;
        }
        if (y <= water) return Blocks.WATER.defaultBlockState();

        int ceil = e.ceiling();
        if (ceil > 0) {
            if (e.vine() > 0 && y < ceil && y >= ceil - e.vine()) return ModBlocks.WEEPVINE.get().defaultBlockState();
            if (y == ceil - 1 && m.hash(x, y, z, 4) < 0.02) return ModBlocks.LANTERNPOD.get().defaultBlockState();
        }

        if (ft < water && y == water + 1) {
            if (m.noiseB(x * 0.12, 8.8, z * 0.12) > 0.3) return ModBlocks.DRIFTMOSS.get().defaultBlockState();
            if (m.hash(x, y, z, 3) < 0.04) return ModBlocks.BLADDERPAD.get().defaultBlockState();
        }
        if (bank && e.plant() == 1) {
            if (y == ft + 1) return Interiors.lower(ModBlocks.SAPCANE.get());
            if (y == ft + 2) return Interiors.upper(ModBlocks.SAPCANE.get());
        }
        if (bank && e.plant() == 2 && y == ft + 1) return ModBlocks.SNARETONGUE.get().defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState tree(MeteorPiece m, Tree t, int x, int y, int z) {
        int dx = x - t.x, dz = z - t.z;
        var w = ModBlocks.STILTREED;
        // Trunk, ringed every 4 blocks with stripped log for a segmented look.
        if (dx == 0 && dz == 0 && y >= t.rootTop && y <= t.top) {
            return (Math.floorMod(y - t.rootTop, 4) == 3 ? w.strippedLog : w.log).get().defaultBlockState();
        }
        // Stilt roots: 4 diagonal legs arching out and down to the floor 3 blocks away.
        if (Math.abs(dx) == Math.abs(dz) && dx != 0 && Math.abs(dx) <= 3) {
            int k = Math.abs(dx);
            if (k < 3 && y == t.rootTop - k) return w.wood.get().defaultBlockState();
            if (k == 3 && y <= t.rootTop - 3 && y > floorTop(m, x, z)) return w.wood.get().defaultBlockState();
        }
        // Frond cap: a flat top disc and a drooping ring around it.
        int dy = y - t.top, r2 = dx * dx + dz * dz;
        boolean leaf = switch (dy) {
            case 1 -> r2 <= 6;
            case 0 -> r2 > 2 && r2 <= 13;
            case -1 -> r2 > 8 && r2 <= 17 && m.hash(x, y, z, 102) < 0.65;
            case -2 -> r2 > 10 && r2 <= 17 && m.hash(x, y, z, 103) < 0.35;
            default -> false;
        };
        return leaf ? w.leaves.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true) : null;
    }
}
