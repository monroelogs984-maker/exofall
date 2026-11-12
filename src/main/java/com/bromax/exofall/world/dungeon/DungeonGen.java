package com.bromax.exofall.world.dungeon;

import com.bromax.exofall.block.NestBlock;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.WoodSet;
import com.bromax.exofall.world.ArenaGen;
import com.bromax.exofall.world.MeteorPiece;
import com.bromax.exofall.world.MeteorStyle;
import com.bromax.exofall.world.dungeon.Dungeon.Feature;
import com.bromax.exofall.world.dungeon.Dungeon.Prop;
import com.bromax.exofall.world.dungeon.Dungeon.PropKind;
import com.bromax.exofall.world.dungeon.Dungeon.Room;
import com.bromax.exofall.world.dungeon.Dungeon.Shack;
import com.bromax.exofall.world.dungeon.Dungeon.Shaft;
import com.bromax.exofall.world.dungeon.Dungeon.Tunnel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;

/**
 * Turns a {@link Dungeon} layout into blocks. Several features can claim one cell (a tunnel running into a room,
 * a shaft through a ceiling); the claim with the highest priority wins:
 * <pre>
 *   1 room walls/ceiling   2 tunnel frames, floor patches, shaft walls   3 tunnel air
 *   4 room floor + inside  5 shaft core (steps, pillar, railing)         6 shacks   7 props
 * </pre>
 * So tunnels cut doorways through room walls, rooms swallow shaft walls, and nothing overwrites a chest.
 */
public final class DungeonGen {
    private static final int P_WALL = 1, P_FRAME = 2, P_TUNNEL = 3, P_ROOM = 4, P_SHAFT = 5, P_SHACK = 6, P_PROP = 7;

    /** Ring cells round a shaft's pillar, clockwise from the north-west corner. */
    private static final int[][] RING = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    /** One chunk's view of the dungeon. */
    public static final class Local {
        private final MeteorPiece m;
        private final Dungeon d;
        private final Feature[] features;
        private final int minY, maxY;

        private Local(MeteorPiece m, int chunkX, int chunkZ) {
            this.m = m;
            this.d = Dungeon.of(m);
            this.features = d.slice(chunkX, chunkZ);
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            for (Feature f : features) {
                lo = Math.min(lo, f.box().minY());
                hi = Math.max(hi, f.box().maxY());
            }
            this.minY = lo;
            this.maxY = hi;
        }

        public boolean empty() {
            return features.length == 0;
        }

        /** The block at (x, y, z) once the dungeon is built over {@code base}. */
        public BlockState apply(int x, int y, int z, BlockState base) {
            if (features.length == 0 || y < minY || y > maxY) return base;
            int best = 0;
            BlockState out = base;
            for (Feature f : features) {
                if (!f.box().isInside(x, y, z)) continue;
                Hit h = new Hit();
                Object w = f.what();
                if (w instanceof Room r) room(m, r, x, y, z, h);
                else if (w instanceof Tunnel t) tunnel(m, d, t, x, y, z, h);
                else if (w instanceof Shaft s) shaft(m, s, x, y, z, h);
                else if (w instanceof Shack s) shack(m, d, s, x, y, z, base, h);
                if (h.state != null && h.prio > best) {
                    best = h.prio;
                    out = h.state;
                }
            }
            if (best > 0) {
                Prop p = d.props.get(BlockPos.asLong(x, y, z));
                if (p != null) out = prop(m, p, out);
            }
            return out;
        }
    }

    private static final class Hit {
        int prio;
        BlockState state;

        void set(int p, BlockState s) {
            if (p > prio) {
                prio = p;
                state = s;
            }
        }
    }

    public static Local forChunk(MeteorPiece m, int chunkX, int chunkZ) {
        return new Local(m, chunkX, chunkZ);
    }

    // ------------------------------------------------------------------------------------------------ palette

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    public static WoodSet wood(MeteorStyle st) {
        return switch (st) {
            case SWAMP -> ModBlocks.STILTREED;
            case HIVE -> ModBlocks.COMBWOOD;
            case DESERT -> ModBlocks.RIBWOOD;
            case RAINFOREST -> ModBlocks.VEILPINE;
        };
    }

    private static BlockState wall(MeteorPiece m, int x, int y, int z) {
        double h = m.hash(x, y, z, 501);
        return switch (m.style()) {
            case SWAMP -> h < 0.45 ? s(Blocks.MUD_BRICKS) : h < 0.55 ? s(Blocks.PACKED_MUD) : s(ModBlocks.CHITINSTONE_BRICKS.get());
            case HIVE -> h < 0.3 ? s(Blocks.HONEYCOMB_BLOCK) : s(ModBlocks.CHITINSTONE_BRICKS.get());
            case DESERT -> Math.floorMod(y, 4) == 0 ? s(ModBlocks.POLISHED_CHITINSTONE.get())
                    : h < 0.15 ? s(Blocks.SMOOTH_SANDSTONE) : s(ModBlocks.CHITINSTONE_BRICKS.get());
            case RAINFOREST -> h < 0.4 ? s(Blocks.MOSSY_STONE_BRICKS) : h < 0.5 ? s(Blocks.MOSSY_COBBLESTONE) : s(ModBlocks.CHITINSTONE_BRICKS.get());
        };
    }

    private static BlockState roomFloor(MeteorPiece m, Room r, int x, int z) {
        int dx = Math.abs(x - r.cx), dz = Math.abs(z - r.cz);
        boolean border = dx == r.hx || dz == r.hz;
        boolean vault = r.kind == Dungeon.Kind.VAULT;
        if (vault && (dx + dz) % 4 == 0) return s(ModBlocks.CHITINSTONE_BRICKS.get());
        if (border || vault) return s(ModBlocks.POLISHED_CHITINSTONE.get());
        double h = m.hash(x, r.floor, z, 502);
        return switch (m.style()) {
            case SWAMP -> h < 0.5 ? s(Blocks.PACKED_MUD) : h < 0.8 ? s(Blocks.MUD_BRICKS) : s(Blocks.MUD);
            case HIVE -> h < 0.6 ? s(Blocks.HONEYCOMB_BLOCK) : s(ModBlocks.CHITINSTONE_BRICKS.get());
            case DESERT -> h < 0.5 ? s(Blocks.SMOOTH_SANDSTONE) : h < 0.75 ? s(Blocks.SANDSTONE) : s(Blocks.SAND);
            case RAINFOREST -> h < 0.5 ? s(Blocks.MOSS_BLOCK) : h < 0.8 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.ROOTED_DIRT);
        };
    }

    private static BlockState path(MeteorStyle st) {
        return switch (st) {
            case SWAMP -> s(Blocks.MUD);
            case HIVE -> s(Blocks.HONEYCOMB_BLOCK);
            case DESERT -> s(Blocks.SAND);
            case RAINFOREST -> s(Blocks.MOSS_BLOCK);
        };
    }

    private static BlockState rubble(MeteorStyle st, double h) {
        return switch (st) {
            case SWAMP -> h < 0.5 ? s(Blocks.GRAVEL) : s(Blocks.MUD);
            case HIVE -> h < 0.5 ? s(Blocks.GRAVEL) : s(Blocks.HONEYCOMB_BLOCK);
            case DESERT -> h < 0.6 ? s(Blocks.SAND) : s(Blocks.GRAVEL);
            case RAINFOREST -> h < 0.5 ? s(Blocks.GRAVEL) : s(Blocks.COARSE_DIRT);
        };
    }

    private static BlockState light(MeteorStyle st) {
        return ArenaGen.light(st);
    }

    private static BlockState web(MeteorPiece m, int x, int y, int z) {
        return s(Blocks.COBWEB);
    }

    // ------------------------------------------------------------------------------------------------ features

    private static void room(MeteorPiece m, Room r, int x, int y, int z, Hit h) {
        int dx = x - r.cx, dz = z - r.cz, adx = Math.abs(dx), adz = Math.abs(dz);
        boolean inner = adx <= r.hx && adz <= r.hz;
        int top = r.floor + r.height;
        if (y == r.floor - 1) {
            h.set(inner ? P_ROOM : P_WALL, inner ? roomFloor(m, r, x, z) : wall(m, x, y, z));
            return;
        }
        if (y == top) {
            boolean lamp = inner && dx % 4 == 0 && dz % 4 == 0;
            h.set(P_WALL, lamp ? light(m.style()) : wall(m, x, y, z));
            return;
        }
        if (!inner) {
            h.set(P_WALL, wall(m, x, y, z));
            return;
        }
        BlockState in = s(Blocks.AIR);
        boolean lane = adx <= 1 || adz <= 1;
        boolean corner = adx == r.hx && adz == r.hz;
        switch (r.kind) {
            case HALL -> {
                if (adx == r.hx - 1 && adz == r.hz - 1) {
                    in = y == r.floor || y == top - 1 ? s(ModBlocks.POLISHED_CHITINSTONE.get())
                            : ModBlocks.CHITINSTONE_PILLAR.get().defaultBlockState();
                }
            }
            case NEST -> {
                if (y == r.floor && !lane && m.hash(x, y, z, 503) < 0.07) in = s(Blocks.BONE_BLOCK);
                else if (y >= top - 2 && (adx == r.hx || adz == r.hz) && m.hash(x, y, z, 504) < 0.25) in = web(m, x, y, z);
            }
            case VAULT -> {
                if (y == r.floor && !lane && (adx == r.hx || adz == r.hz) && m.hash(x, y, z, 505) < 0.2) {
                    in = s(m.style() == MeteorStyle.HIVE ? Blocks.HONEY_BLOCK : Blocks.BONE_BLOCK);
                }
            }
            default -> {}
        }
        if (in.isAir() && corner && y == top - 1 && m.hash(x, y, z, 506) < 0.6) in = web(m, x, y, z);
        h.set(P_ROOM, in);
    }

    private static void tunnel(MeteorPiece m, Dungeon d, Tunnel t, int x, int y, int z, Hit h) {
        int a = t.alongX ? x : z, l = t.alongX ? z - t.z0 : x - t.x0;
        if (Math.abs(l) > 1) return;
        int a0 = t.alongX ? t.x0 : t.z0, a1 = t.alongX ? t.x1 : t.z1;
        if (a < a0 - 1 || a > a1 + 1) return;
        // Ends one past the centre line so L corners join cleanly; dead ends stop at their end.
        if (t.deadEnd && (a < a0 || a > a1)) return;
        int f = t.floor;
        if (y == f - 1) {
            if (m.hash(x, y, z, 510) < 0.3) h.set(P_FRAME, path(m.style()));
            return;
        }
        if (y < f || y > f + 2) return;

        // Cave-in at a dead end: a pile sloping down from the end wall.
        if (t.deadEnd) {
            for (int e : new int[]{a0, a1}) {
                int ex = t.alongX ? e : t.x0, ez = t.alongX ? t.z0 : e;
                Prop p = d.props.get(BlockPos.asLong(ex, f, ez));
                if (p == null || p.kind() != PropKind.RUBBLE) continue;
                int dist = Math.abs(a - e);
                if (dist <= 2 && y < f + 3 - dist) {
                    h.set(P_TUNNEL, rubble(m.style(), m.hash(x, y, z, 511)));
                    return;
                }
            }
        }

        boolean frame = Math.floorMod(a, 4) == 0 && a > a0 && a < a1;
        WoodSet w = wood(m.style());
        if (frame) {
            if (y == f + 2) {
                boolean lamp = l == 0 && Math.floorMod(a, 12) == 0;
                h.set(P_FRAME, lamp ? light(m.style()) : s(w.planks.get()));
                return;
            }
            if (l != 0) {
                h.set(P_FRAME, s(w.fence.get()));
                return;
            }
        }
        BlockState air = s(Blocks.AIR);
        if (y == f && l == 0 && t.rails && a > a0 + 1 && a < a1 - 1 && m.hash(x, y, z, 512) < 0.8) {
            air = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, t.alongX ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
        } else if (y == f + 2 && l != 0 && m.hash(x, y, z, 513) < (m.style() == MeteorStyle.SWAMP ? 0.12 : 0.05)) {
            air = web(m, x, y, z);
        }
        h.set(P_TUNNEL, air);
    }

    private static Direction dirTo(int[] from, int[] to) {
        int dx = to[0] - from[0], dz = to[1] - from[1];
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static void shaft(MeteorPiece m, Shaft s, int x, int y, int z, Hit h) {
        int dx = x - s.x, dz = z - s.z;
        int adx = Math.abs(dx), adz = Math.abs(dz);
        if (adx > 2 || adz > 2) return;
        boolean core = adx <= 1 && adz <= 1;
        if (y == s.top + 1) {
            // Railing round the hole, open where step 0 meets the floor.
            if (core) return;
            int[] c0 = RING[s.phase];
            boolean opening = dx == 2 * c0[0] && dz == 2 * c0[1];
            if (!opening) h.set(P_SHAFT, s(ModBlocks.CHITINSTONE_BRICK_WALL.get()));
            return;
        }
        if (y < s.bottom - 1 || y > s.top) return;
        if (!core) {
            if (y >= s.bottom) h.set(P_FRAME, wall(m, x, y, z));
            return;
        }
        if (dx == 0 && dz == 0) {
            h.set(P_SHAFT, ModBlocks.CHITINSTONE_PILLAR.get().defaultBlockState());
            return;
        }
        if (y < s.bottom) return;
        int i = 0;
        while (RING[i][0] != dx || RING[i][1] != dz) i++;
        if (Math.floorMod(s.top - y - (i - s.phase), 8) == 0) {
            Direction up = dirTo(RING[i], RING[(i + 7) % 8]);
            h.set(P_SHAFT, ModBlocks.CHITINSTONE_BRICK_STAIRS.get().defaultBlockState()
                    .setValue(StairBlock.FACING, up).setValue(StairBlock.HALF, Half.BOTTOM));
        } else {
            h.set(P_SHAFT, s(Blocks.AIR));
        }
    }

    private static void shack(MeteorPiece m, Dungeon d, Shack s, int x, int y, int z, BlockState base, Hit h) {
        int dx = x - s.x, dz = z - s.z, adx = Math.abs(dx), adz = Math.abs(dz);
        int hf = s.half, f = s.floor;
        WoodSet w = wood(m.style());
        Direction door = s.door;
        int out = dx * door.getStepX() + dz * door.getStepZ();          // towards the door
        int lat = dx * door.getClockWise().getStepX() + dz * door.getClockWise().getStepZ();

        // Porch: a landing outside the door, then steps down to the ground.
        if (out > hf && out <= hf + Dungeon.porch(m) && Math.abs(lat) <= 1) {
            int dist = out - hf;
            int step = f - dist;            // top of this porch block
            int ground = m.style().interior().surface(m, x, z);
            if (step <= ground) {
                if (y > ground && y <= f + 2) h.set(P_SHACK, s(Blocks.AIR)); // keep the way clear of plants
                return;
            }
            if (y == step) {
                h.set(P_SHACK, dist == 1 ? s(w.planks.get())
                        : w.stairs.get().defaultBlockState().setValue(StairBlock.FACING, door.getOpposite()));
            } else if (y < step && y > ground - Dungeon.FOOTING) {
                if (!solid(base) || y > ground) h.set(P_SHACK, s(ModBlocks.CHITINSTONE_BRICKS.get()));
            } else if (y > step && y <= f + 2) {
                h.set(P_SHACK, s(Blocks.AIR));
            }
            return;
        }
        if (adx > hf + 1 || adz > hf + 1) return;

        // Entrance shacks leave the shaft to the shaft.
        if (s.entry && adx <= 2 && adz <= 2) {
            boolean core = adx <= 1 && adz <= 1;
            if (core && y <= f - 1) return;
            if (!core && (y == f || y < f - 1)) return;
        }

        boolean onFoot = adx <= hf && adz <= hf;
        boolean corner = adx == hf && adz == hf;
        boolean perim = onFoot && (adx == hf || adz == hf);
        BlockState log = w.log.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);

        if (y < f - 1) {
            if (onFoot && y >= s.ground - Dungeon.FOOTING && !solid(base)) h.set(P_SHACK, corner ? log : s(ModBlocks.CHITINSTONE_BRICKS.get()));
            return;
        }
        if (y == f - 1) {
            if (onFoot) h.set(P_SHACK, corner ? log : s(w.planks.get()));
            return;
        }

        int u = s.ridgeX ? dz : dx, v = s.ridgeX ? dx : dz;
        int au = Math.abs(u), av = Math.abs(v);
        int roofY = f + 4 + (hf + 1 - au);
        if (y == roofY) {
            if (u == 0) {
                h.set(P_SHACK, w.slab.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            } else {
                Direction across = s.ridgeX ? Direction.SOUTH : Direction.EAST;
                h.set(P_SHACK, w.stairs.get().defaultBlockState().setValue(StairBlock.FACING, u < 0 ? across : across.getOpposite()));
            }
            return;
        }
        if (y > roofY) return;
        if (!onFoot) return; // under the eaves

        if (y >= f + 4) {
            // gable ends and the strip between wall top and roof
            boolean gable = av == hf || au == hf;
            if (gable) h.set(P_SHACK, s(w.planks.get()));
            else if (u == 0 && y == roofY - 1 && v == 1) h.set(P_SHACK, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            else h.set(P_SHACK, s(Blocks.AIR));
            return;
        }
        // walls: y in [f, f + 3]
        if (!perim) {
            h.set(P_SHACK, s(Blocks.AIR));
            return;
        }
        if (corner) {
            h.set(P_SHACK, log);
            return;
        }
        boolean doorWall = out == hf;
        if (doorWall && lat == 0 && y <= f + 1) {
            if (s.entry) {
                h.set(P_SHACK, w.door.get().defaultBlockState().setValue(DoorBlock.FACING, door.getOpposite())
                        .setValue(DoorBlock.HALF, y == f ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER));
            } else {
                h.set(P_SHACK, s(Blocks.AIR));
            }
            return;
        }
        // windows: openings on the other three walls
        int along = doorWall ? lat : (adx == hf ? dz : dx);
        boolean window = !doorWall && (y == f + 1 || y == f + 2) && (hf <= 3 ? along == 0 : Math.abs(along) == 2);
        h.set(P_SHACK, window ? s(Blocks.AIR) : (y == f + 3 ? log.setValue(RotatedPillarBlock.AXIS, adx == hf ? Direction.Axis.Z : Direction.Axis.X)
                : s(w.planks.get())));
    }

    private static boolean solid(BlockState b) {
        return b.isSolid() && b.getFluidState().isEmpty();
    }

    private static BlockState prop(MeteorPiece m, Prop p, BlockState under) {
        return switch (p.kind()) {
            case CHEST -> Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, p.facing());
            case BARREL -> s(Blocks.BARREL);
            case NEST -> NestBlock.forStyle(m.style());
            case RUBBLE -> under;
        };
    }

    // ------------------------------------------------------------------------------------------------ loot

    public static ResourceKey<LootTable> lootFor(MeteorPiece m, BlockPos pos, BlockState placed) {
        Prop p = Dungeon.of(m).props.get(pos.asLong());
        String st = m.style().getSerializedName();
        String name;
        if (placed.is(Blocks.BARREL)) name = "chests/warren_supplies";
        else if (p != null && p.vault()) name = "chests/warren_vault_" + st;
        else name = "chests/warren_" + st;
        return ResourceKey.create(Registries.LOOT_TABLE, com.bromax.exofall.Exofall.id(name));
    }
}
