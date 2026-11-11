package com.bromax.exofall.world;

import com.bromax.exofall.block.SigilSocketBlock;
import com.bromax.exofall.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import static com.bromax.exofall.world.ArenaLayout.*;

/**
 * Boss arena at the middle of every meteor: a sealed dome on a paved plaza, its door shut by a Sigil Seal
 * with the Sigil Socket in the door post. Like the rest of the meteor, every block is a pure function of
 * position, so chunks can generate in any order. {@link MeteorPiece} runs each interior block through
 * {@link #apply}. The plaza around the dome is levelled to the arena floor and blends back into the natural
 * ground by {@link ArenaLayout#BLEND_R}; trees and pillars are kept out of that ring ({@link #excludes}).
 */
public final class ArenaGen {
    /** Arena centre: the cell the boss stands in. The floor is one below, above the swamp water line. */
    public static BlockPos centre(MeteorPiece m) {
        return new BlockPos(m.centre().getX(), m.baseFloor() + 2, m.centre().getZ());
    }

    public static Direction door(MeteorPiece m) {
        return Direction.from2DDataValue((int) (m.hash(m.centre().getX(), 0, m.centre().getZ(), 77) * 4));
    }

    /** Whether a tree or pillar rooted at (x, z) would intrude on the arena or its plaza. */
    public static boolean excludes(MeteorPiece m, int x, int z) {
        int dx = x - m.centre().getX(), dz = z - m.centre().getZ();
        return dx * dx + dz * dz < EXCLUDE_R * EXCLUDE_R;
    }

    private static double smooth(double e0, double e1, double v) {
        double t = Math.clamp((v - e0) / (e1 - e0), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static boolean solid(BlockState b) {
        return b.isSolid() && b.getFluidState().isEmpty();
    }

    /** The block at (x, y, z) once the arena is built; {@code base} is what the interior would put there. */
    public static BlockState apply(MeteorPiece m, MeteorStyle style, int naturalFloor, int x, int y, int z, BlockState base) {
        int cx = m.centre().getX(), cz = m.centre().getZ(), floor = m.baseFloor() + 2;
        int dx = x - cx, dz = z - cz, dy = y - floor;
        int r2 = dx * dx + dz * dz;
        if (r2 > BLEND_R * BLEND_R || dy < -12 || dy > CLEAR_H + OUTER_V) return base;
        double r = Math.sqrt(r2);

        // Door and porch
        Direction d = door(m);
        Direction cw = d.getClockWise();
        int a = dx * d.getStepX() + dz * d.getStepZ(), l = dx * cw.getStepX() + dz * cw.getStepZ();
        if (a >= DOOR_IN && a <= SOCKET_A && Math.abs(l) <= DOOR_HALF_W + 1 && dy >= -1 && dy <= DOOR_H + 1) {
            if (dy == -1) return s(ModBlocks.POLISHED_CHITINSTONE.get());
            if (Math.abs(l) <= DOOR_HALF_W && dy < DOOR_H) return a == SEAL_A ? s(ModBlocks.SIGIL_SEAL.get()) : s(Blocks.AIR);
            if (dy < DOOR_H) {
                if (l == SOCKET_L && a == SOCKET_A && dy == SOCKET_DY) {
                    return ModBlocks.SIGIL_SOCKET.get().defaultBlockState().setValue(SigilSocketBlock.FACING, d).setValue(SigilSocketBlock.KIND, style);
                }
                return s(ModBlocks.CHITINSTONE_PILLAR.get());
            }
            if (dy == DOOR_H) return s(ModBlocks.POLISHED_CHITINSTONE.get());
            if (Math.abs(l) <= DOOR_HALF_W && a >= OUTER_R - 2) return s(ModBlocks.CHITINSTONE_BRICKS.get());
        }

        // Floor under the dome
        if (dy < 0 && dy >= -5 && r <= OUTER_R) return floor(m, style, x, z, dx, dz, dy, r);

        // Dome
        if (dy >= 0) {
            double eOut = r2 / (double) (OUTER_R * OUTER_R) + dy * dy / (double) (OUTER_V * OUTER_V);
            if (eOut < 1) {
                double eIn = r2 / (double) (INNER_R * INNER_R) + dy * dy / (double) (INNER_V * INNER_V);
                if (eIn < 1) {
                    double eUp = r2 / (double) (INNER_R * INNER_R) + (dy + 1) * (dy + 1) / (double) (INNER_V * INNER_V);
                    return inside(m, style, x, y, z, dx, dz, dy, r, eUp >= 1);
                }
                return wall(m, style, x, y, z, dx, dz, dy, r, eIn);
            }
        }

        // Plaza: paved and levelled, cleared of everything for CLEAR_H blocks
        if (r <= PLAZA_R) {
            if (dy == -1) return (((int) r) % 4 == 0) ? s(ModBlocks.CHITINSTONE_BRICKS.get()) : s(ModBlocks.POLISHED_CHITINSTONE.get());
            if (dy < -1 && dy >= -5) return solid(base) ? base : s(ModBlocks.CHITINSTONE.get());
            if (dy >= 0 && dy <= CLEAR_H) return s(Blocks.AIR);
            return base;
        }

        // Blend ring: ground eases from the plaza level back to the natural floor
        double t = smooth(PLAZA_R, BLEND_R, r);
        int target = (int) Math.round((floor - 1) + (naturalFloor - (floor - 1)) * t);
        int clearTop = (int) Math.round((floor - 1) + CLEAR_H * (1 - t));
        if (y <= target) {
            if (y > naturalFloor && !solid(base)) return y == target ? plazaTop(style) : s(ModBlocks.CHITINSTONE.get());
            return base;
        }
        if (y <= clearTop && base.getFluidState().isEmpty()) return s(Blocks.AIR);
        return base;
    }

    private static BlockState plazaTop(MeteorStyle style) {
        return switch (style) {
            case SWAMP -> s(Blocks.MUD);
            case HIVE -> s(Blocks.HONEYCOMB_BLOCK);
            case DESERT -> s(Blocks.SAND);
            case RAINFOREST -> s(Blocks.MOSS_BLOCK);
        };
    }

    private static boolean swampPool(MeteorPiece m, int x, int z, double r) {
        return r < 3.5 || (r < 12.5 && m.noiseA(x * 0.15, 3.3, z * 0.15) > 0.18);
    }

    private static BlockState floor(MeteorPiece m, MeteorStyle style, int x, int z, int dx, int dz, int dy, double r) {
        switch (style) {
            case SWAMP -> {
                if (swampPool(m, x, z, r)) return dy >= -2 ? s(Blocks.WATER) : s(Blocks.MUD);
                if (dy == -1) return r >= 13 ? s(Blocks.MUD_BRICKS) : s(Blocks.MUD);
                return s(Blocks.PACKED_MUD);
            }
            case HIVE -> {
                if (dy == -1) return r < 13 && m.noiseA(x * 0.2, 7.7, z * 0.2) > 0.3 ? s(Blocks.HONEY_BLOCK) : s(Blocks.HONEYCOMB_BLOCK);
                return s(ModBlocks.CHITINSTONE_BRICKS.get());
            }
            case DESERT -> {
                if (dy == -1 && r >= 12.5 && r < 14) return s(ModBlocks.POLISHED_CHITINSTONE.get());
                if (dy >= -2) return m.noiseB(x * 0.12, 4.4, z * 0.12) > 0.3 ? s(Blocks.RED_SAND) : s(Blocks.SAND);
                return s(ModBlocks.CHITINSTONE_BRICKS.get());
            }
            default -> {
                if (dy == -1) return m.hash(x, 0, z, 81) < 0.12 ? s(Blocks.ROOTED_DIRT) : s(Blocks.MOSS_BLOCK);
                return s(ModBlocks.CHITINSTONE_BRICKS.get());
            }
        }
    }

    /** One of n columns on a ring of radius rr; returns the squared distance to the nearest one. */
    private static double ringPillar(int dx, int dz, int n, double rr, double offsetDeg) {
        double best = 1e9;
        for (int k = 0; k < n; k++) {
            double ang = Math.toRadians(offsetDeg + k * 360.0 / n);
            double px = Math.cos(ang) * rr, pz = Math.sin(ang) * rr;
            best = Math.min(best, (dx - px) * (dx - px) + (dz - pz) * (dz - pz));
        }
        return best;
    }

    private static BlockState inside(MeteorPiece m, MeteorStyle style, int x, int y, int z, int dx, int dz, int dy, double r, boolean underRoof) {
        switch (style) {
            case SWAMP -> {
                if (dy == 0 && swampPool(m, x, z, r) && r >= 3.5 && m.hash(x, y, z, 82) < 0.14) return s(ModBlocks.DRIFTMOSS.get());
                if (underRoof && m.hash(x, y, z, 83) < 0.05) return s(ModBlocks.LANTERNPOD.get());
            }
            case HIVE -> {
                if (ringPillar(dx, dz, 6, 10, 30) <= 2.2 * 2.2) return s(Blocks.HONEYCOMB_BLOCK);
                if (underRoof && m.hash(x, y, z, 83) < 0.04) {
                    return ModBlocks.ROYAL_AMBER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN);
                }
            }
            case DESERT -> {
                double p = ringPillar(dx, dz, 4, 10.5, 45);
                if (dy <= 5 && p <= 1.3 * 1.3) return s(Blocks.BONE_BLOCK);
                if (dy == 6 && p <= 2.3 * 2.3) return s(Blocks.BONE_BLOCK);
            }
            case RAINFOREST -> {
                double p = ringPillar(dx, dz, 5, 10, 18);
                if (dy <= 8 && p <= 1.0) return ModBlocks.VEILPINE.log.get().defaultBlockState();
                if (dy >= 6 && dy <= 9 && p + (dy - 7.5) * (dy - 7.5) * 1.5 <= 2.8 * 2.8 && ModBlocks.VEILPINE.leaves != null) {
                    return ModBlocks.VEILPINE.leaves.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                }
                if (underRoof && m.hash(x, y, z, 83) < 0.06) return s(ModBlocks.GLOWMOSS_CURTAIN.get());
                double g = m.hash(x, 0, z, 84);
                if (r < 13.5 && dy <= 1) {
                    if (g < 0.07) return Blocks.LARGE_FERN.defaultBlockState()
                            .setValue(DoublePlantBlock.HALF, dy == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                    if (dy == 0) {
                        if (g < 0.32) return s(Blocks.SHORT_GRASS);
                        if (g < 0.42) return s(Blocks.FERN);
                    }
                }
            }
        }
        return s(Blocks.AIR);
    }

    private static BlockState wall(MeteorPiece m, MeteorStyle style, int x, int y, int z, int dx, int dz, int dy, double r, double eIn) {
        if (eIn < 1.22) {   // inner surface: lamps
            int ai = Math.floorMod((int) Math.round(Math.atan2(dz, dx) / (2 * Math.PI) * 40), 40);
            if ((dy == 3 && ai % 5 == 0) || (dy == 8 && ai % 8 == 0) || (dy >= INNER_V - 1 && r <= 2.5)) return light(style);
        }
        double h = m.hash(x, y, z, 85);
        return switch (style) {
            case SWAMP -> h < 0.3 ? s(Blocks.MUD_BRICKS) : s(ModBlocks.CHITINSTONE_BRICKS.get());
            case HIVE -> h < 0.35 ? s(Blocks.HONEYCOMB_BLOCK) : s(ModBlocks.CHITINSTONE_BRICKS.get());
            case DESERT -> dy % 4 == 0 ? s(ModBlocks.POLISHED_CHITINSTONE.get()) : s(ModBlocks.CHITINSTONE_BRICKS.get());
            case RAINFOREST -> h < 0.25 ? s(Blocks.MOSSY_STONE_BRICKS) : s(ModBlocks.CHITINSTONE_BRICKS.get());
        };
    }

    private static BlockState light(MeteorStyle style) {
        return switch (style) {
            case SWAMP -> s(Blocks.VERDANT_FROGLIGHT);
            case HIVE -> s(Blocks.SHROOMLIGHT);
            case DESERT -> s(Blocks.OCHRE_FROGLIGHT);
            case RAINFOREST -> s(Blocks.PEARLESCENT_FROGLIGHT);
        };
    }

    private ArenaGen() {}
}
