package com.bromax.exofall.world.interior;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.world.MeteorInterior;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Wasp hive: parallel comb sheets running floor to ceiling (like a real nest), torn open by noise holes
 * plus regular low doorways so every gap can be walked into. Honey patches and Glowmarl beds on the floor
 * with Nectar Bells and Waxcaps, Royal Amber crystals, and thick Combwood trunks growing up through the comb,
 * their hollow cores broken by solid nodes every few blocks.
 */
public class HiveInterior implements MeteorInterior {
    private static final int SHEET_SPACING = 7, SHEET_THICKNESS = 2, DOOR_SPACING = 13, DOOR_WIDTH = 3, DOOR_HEIGHT = 4;

    private record Trunk(int x, int z, int base, int top) implements TreeGrid.Placed {}

    private record Extra(List<Trunk> trunks, int plant) {}

    private static final TreeGrid<Trunk> TRUNKS = new TreeGrid<>(18, 2, 0.5, 200, HiveInterior::makeTrunk);

    private static int floorTop(MeteorPiece m, int x, int z) {
        return m.baseFloor() + (int) Math.floor(1.5 * m.noiseA(x * 0.08, 3.3, z * 0.08));
    }

    private static Trunk makeTrunk(MeteorPiece m, int tx, int tz) {
        int ft = floorTop(m, tx, tz);
        int ceiling = m.ceilingAbove(tx, ft, tz, Interiors.CEILING_SCAN);
        if (ceiling < 0 || ceiling - ft < 12) return null;
        return new Trunk(tx, tz, ft + 1, ceiling - 1);
    }

    @Override
    public int surface(MeteorPiece m, int x, int z) {
        return floorTop(m, x, z);
    }

    @Override
    public Column column(MeteorPiece m, int x, int z) {
        int ft = floorTop(m, x, z);
        boolean alongX = (m.seed() & 1) == 0;
        int u = (alongX ? x - m.centre().getX() : z - m.centre().getZ());
        int v = (alongX ? z - m.centre().getZ() : x - m.centre().getX());
        boolean sheet = Mth.positiveModulo(u, SHEET_SPACING) < SHEET_THICKNESS;
        boolean door = Mth.positiveModulo(v, DOOR_SPACING) < DOOR_WIDTH;
        double ph = m.hash(x, 0, z, 2);
        int plant = ph < 0.10 ? 1 : ph < 0.16 ? 2 : ph < 0.166 ? 3 : 0; // 1 nectar bell, 2 waxcap, 3 royal amber
        return new Column(ft, sheet ? 1 : 0, door ? 1 : 0, new Extra(TRUNKS.near(m, x, z), plant));
    }

    private static boolean glowBed(MeteorPiece m, int x, int z) {
        return m.noiseA(x * 0.05, 17.0, z * 0.05) > 0.25;
    }

    @Override
    public BlockState block(MeteorPiece m, Column col, int x, int y, int z) {
        int ft = col.floorTop();
        Extra e = (Extra) col.extra();
        if (y <= ft) {
            if (ft - y >= Interiors.SURFACE_DEPTH) return Interiors.chitinstone();
            if (y == ft && glowBed(m, x, z)) return Interiors.glowmarl();
            if (y == ft && m.noiseB(x * 0.15, 9.1, z * 0.15) > 0.35) return Blocks.HONEY_BLOCK.defaultBlockState();
            return Blocks.HONEYCOMB_BLOCK.defaultBlockState();
        }

        for (Trunk t : e.trunks()) {
            int dx = Math.abs(x - t.x), dz = Math.abs(z - t.z);
            if (y < t.base || y > t.top) continue;
            if (dx <= 1 && dz <= 1) {
                boolean core = dx == 0 && dz == 0;
                if (!core) return ModBlocks.COMBWOOD.log.get().defaultBlockState();
                boolean node = Math.floorMod(y - t.base, 7) == 0 || y == t.top;
                return node ? ModBlocks.COMBWOOD.planks.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
            }
            // Root flare at the base.
            if (y < t.base + 2 && ((dx == 2 && dz == 0) || (dx == 0 && dz == 2))) return ModBlocks.COMBWOOD.wood.get().defaultBlockState();
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
        if (y == ft + 1) {
            boolean bed = glowBed(m, x, z);
            if (bed && e.plant() == 1) return ModBlocks.NECTAR_BELL.get().defaultBlockState();
            if (bed && e.plant() == 2) return ModBlocks.WAXCAP.get().defaultBlockState();
            if (e.plant() == 3) return ModBlocks.ROYAL_AMBER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
        }
        if (m.hash(x, y, z, 4) < 0.03 && m.classify(x, y + 1, z) == MeteorPiece.CRUST) return Blocks.SHROOMLIGHT.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }
}
