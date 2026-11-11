package com.bromax.exofall.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Shape of the boss arena at the middle of every meteor, shared by world generation ({@link ArenaGen})
 * and the boss fight (sealing the door, keeping the boss home). All positions are relative to the arena
 * centre: the block the boss stands in, on the arena floor. The door faces {@code door} (outward).
 *
 * <pre>
 *   dome: inner radius 15 x 12 tall, outer 17 x 14; floor 5 deep under it
 *   door: 3 wide x 4 tall through the wall; the seal fills it at 17 out, the socket sits in the
 *         right-hand door post at 18 out, one block up
 * </pre>
 */
public final class ArenaLayout {
    public static final int INNER_R = 15, INNER_V = 12, OUTER_R = 17, OUTER_V = 14;
    public static final int PLAZA_R = 24, BLEND_R = 32, CLEAR_H = 16;
    /** Trees and pillars whose trunk is closer than this would hang over the cleared plaza. */
    public static final int EXCLUDE_R = 38;
    public static final int DOOR_IN = 13, SEAL_A = 17, SOCKET_A = 18, SOCKET_L = 2, SOCKET_DY = 1;
    public static final int DOOR_HALF_W = 1, DOOR_H = 4;

    public static BlockPos centreFromSocket(BlockPos socket, Direction door) {
        return socket.relative(door, -SOCKET_A).relative(door.getClockWise(), -SOCKET_L).below(SOCKET_DY);
    }

    public static BlockPos socket(BlockPos centre, Direction door) {
        return centre.relative(door, SOCKET_A).relative(door.getClockWise(), SOCKET_L).above(SOCKET_DY);
    }

    /** The door blocks the seal fills. */
    public static List<BlockPos> seal(BlockPos centre, Direction door) {
        List<BlockPos> out = new ArrayList<>();
        for (int l = -DOOR_HALF_W; l <= DOOR_HALF_W; l++) {
            for (int dy = 0; dy < DOOR_H; dy++) {
                out.add(centre.relative(door, SEAL_A).relative(door.getClockWise(), l).above(dy));
            }
        }
        return out;
    }

    /** Whether a point is inside the arena dome (the fighting space). */
    public static boolean inside(BlockPos centre, Vec3 p) {
        double dx = p.x - (centre.getX() + 0.5), dz = p.z - (centre.getZ() + 0.5), dy = p.y - centre.getY();
        if (dy < -2 || dy > INNER_V) return false;
        double e = (dx * dx + dz * dz) / (INNER_R * INNER_R) + Math.max(0, dy) * Math.max(0, dy) / (double) (INNER_V * INNER_V);
        return e < 1;
    }

    public static AABB bounds(BlockPos centre) {
        return new AABB(centre).inflate(INNER_R, 0, INNER_R).expandTowards(0, INNER_V, 0).move(0, -2, 0).expandTowards(0, 2, 0);
    }

    private ArenaLayout() {}
}
