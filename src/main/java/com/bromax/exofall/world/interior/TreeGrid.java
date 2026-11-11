package com.bromax.exofall.world.interior;

import com.bromax.exofall.world.MeteorPiece;

import java.util.ArrayList;
import java.util.List;

/**
 * Jittered grid of trees (or any big feature) for one meteor. Each grid cell holds at most one tree,
 * decided purely from the meteor seed, so trees cross chunk borders cleanly. Results are cached per meteor.
 */
final class TreeGrid<T extends TreeGrid.Placed> {
    interface Placed { int x(); int z(); }
    interface Factory<T> { T make(MeteorPiece m, int tx, int tz); }

    private final int grid, reach, salt;
    private final double chance;
    private final Factory<T> factory;

    TreeGrid(int grid, int reach, double chance, int salt, Factory<T> factory) {
        this.grid = grid;
        this.reach = reach;
        this.chance = chance;
        this.salt = salt;
        this.factory = factory;
    }

    /** Trees whose blocks may reach column (x, z). */
    List<T> near(MeteorPiece m, int x, int z) {
        List<T> out = new ArrayList<>(2);
        int gx0 = Math.floorDiv(x - reach, grid), gx1 = Math.floorDiv(x + reach, grid);
        int gz0 = Math.floorDiv(z - reach, grid), gz1 = Math.floorDiv(z + reach, grid);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                T t = at(m, gx, gz);
                if (t != null && Math.abs(t.x() - x) <= reach && Math.abs(t.z() - z) <= reach) out.add(t);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private T at(MeteorPiece m, int gx, int gz) {
        long key = (((long) gx << 32) ^ (gz & 0xFFFFFFFFL)) * 31 + salt;
        Object cached = m.cache.get(key);
        if (cached != null) return cached == Boolean.FALSE ? null : (T) cached;
        T result = null;
        if (m.hash(gx, salt, gz, 40) < chance) {
            int margin = Math.min(2, grid / 4);
            int tx = gx * grid + margin + (int) (m.hash(gx, salt + 1, gz, 41) * (grid - 2 * margin));
            int tz = gz * grid + margin + (int) (m.hash(gx, salt + 2, gz, 42) * (grid - 2 * margin));
            result = com.bromax.exofall.world.ArenaGen.excludes(m, tx, tz) ? null : factory.make(m, tx, tz);
        }
        m.cache.put(key, result == null ? Boolean.FALSE : result);
        return result;
    }
}
