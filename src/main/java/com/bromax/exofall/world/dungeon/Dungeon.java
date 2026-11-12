package com.bromax.exofall.world.dungeon;

import com.bromax.exofall.world.ArenaGen;
import com.bromax.exofall.world.ArenaLayout;
import com.bromax.exofall.world.MeteorPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Warrens under a meteor's floor: two levels of mineshaft-style tunnels joining rooms, spiral stair shafts
 * between the levels and up to the surface, and shacks on the interior floor (entrance huts over the shafts, and
 * nest huts with a spawner inside). Built once per meteor from its seed; {@link DungeonGen} turns it into blocks.
 *
 * <pre>
 *   surface  shacks (floor = local ground)              entrance shafts come down from here
 *   level 0  floor = baseFloor - 12, rooms 5 tall       nest, store, hall, entry and stair rooms
 *   level 1  floor = baseFloor - 24, rooms 5 tall       one vault (7 tall) with the best loot
 * </pre>
 * Everything is a pure function of the seed, so every chunk sees the same layout.
 */
public final class Dungeon {
    public static final int[] DEPTH = {12, 24};
    /** How far the cleared path runs out from a shack door: far enough in the hive to cut through one comb sheet. */
    static int porch(MeteorPiece m) {
        return m.style() == com.bromax.exofall.world.MeteorStyle.HIVE ? 9 : 4;
    }

    /** Shack foundations reach this far under the lowest ground of their footprint. */
    static final int FOOTING = 4;
    static final int ROOM_H = 5, VAULT_H = 7, TUNNEL_H = 3;
    /** Room/tunnel cells must sit this deep inside the shell surface (crust is 5 thick). */
    private static final double SAFE = 7.0;

    public enum Kind { NEST, STORE, HALL, STAIR, VAULT, ENTRY }

    public enum PropKind { CHEST, BARREL, NEST, RUBBLE }

    /** Interior air box: x in [cx-hx, cx+hx], z in [cz-hz, cz+hz], y in [floor, floor+height-1]; walls one block out. */
    public static final class Room {
        public final int level, cx, cz, hx, hz, floor, height;
        public Kind kind;

        Room(int level, int cx, int cz, int hx, int hz, int floor, int height, Kind kind) {
            this.level = level; this.cx = cx; this.cz = cz; this.hx = hx; this.hz = hz;
            this.floor = floor; this.height = height; this.kind = kind;
        }

        public boolean inside(int x, int y, int z) {
            return Math.abs(x - cx) <= hx && Math.abs(z - cz) <= hz && y >= floor && y < floor + height;
        }

        BoundingBox box() {
            return new BoundingBox(cx - hx - 1, floor - 1, cz - hz - 1, cx + hx + 1, floor + height, cz + hz + 1);
        }

        boolean overlaps(int ocx, int ocz, int ohx, int ohz, int gap) {
            return Math.abs(ocx - cx) <= ohx + hx + 2 + gap && Math.abs(ocz - cz) <= ohz + hz + 2 + gap;
        }
    }

    /** Straight tunnel, 3 wide and 3 tall, centre line from (x0, z0) to (x1, z1) along one axis (x0 <= x1, z0 <= z1). */
    public static final class Tunnel {
        public final int x0, z0, x1, z1, floor;
        public final boolean alongX, rails, deadEnd;
        /** The rooms this tunnel helps join (null for dead ends). */
        Room a, b;

        Tunnel(int x0, int z0, int x1, int z1, int floor, boolean rails, boolean deadEnd) {
            this.x0 = Math.min(x0, x1); this.x1 = Math.max(x0, x1);
            this.z0 = Math.min(z0, z1); this.z1 = Math.max(z0, z1);
            this.alongX = z0 == z1;
            this.floor = floor; this.rails = rails; this.deadEnd = deadEnd;
        }

        int length() {
            return (x1 - x0) + (z1 - z0);
        }

        BoundingBox box() {
            return new BoundingBox(x0 - 1, floor - 1, z0 - 1, x1 + 1, floor + TUNNEL_H, z1 + 1);
        }
    }

    /**
     * Spiral stair: a 1x1 pillar at (x, z) with 8 step cells round it, one step down per cell, inside a 5x5 walled
     * shaft. Step 0 is at {@code top} (flush with the floor it leaves), the last at {@code bottom}.
     */
    public static final class Shaft {
        public final int x, z, top, bottom, phase;

        Shaft(int x, int z, int top, int bottom, int phase) {
            this.x = x; this.z = z; this.top = top; this.bottom = bottom; this.phase = phase;
        }

        BoundingBox box() {
            return new BoundingBox(x - 2, bottom - 1, z - 2, x + 2, top + 1, z + 2);
        }
    }

    /**
     * Wooden shack on the interior floor. Air from {@code floor} up; walls 4 high at |dx| or |dz| == half; a gable roof
     * whose ridge runs along x when {@code ridgeX}. Entry shacks hold a shaft, the others a nest.
     */
    public static final class Shack {
        public final int x, z, floor, half, ground;
        public final Direction door;
        public final boolean entry, ridgeX;

        Shack(int x, int z, int floor, int half, int ground, Direction door, boolean entry) {
            this.x = x; this.z = z; this.floor = floor; this.half = half; this.ground = ground; this.door = door; this.entry = entry;
            this.ridgeX = door.getAxis() == Direction.Axis.Z; // door on a gable-free long side
        }

        int roofTop() {
            return floor + 4 + half + 1;
        }

        BoundingBox box() {
            return new BoundingBox(x - half - 9, ground - 2 * FOOTING, z - half - 9, x + half + 9, roofTop(), z + half + 9);
        }
    }

    public record Prop(PropKind kind, Direction facing, boolean vault) {}

    /** Something that places blocks, with the box it can touch. */
    record Feature(BoundingBox box, Object what) {}

    public final List<Room> rooms = new ArrayList<>();
    public final List<Tunnel> tunnels = new ArrayList<>();
    public final List<Shaft> shafts = new ArrayList<>();
    public final List<Shack> shacks = new ArrayList<>();
    public final Map<Long, Prop> props = new HashMap<>();
    public final int[] floors = new int[2];
    private final List<Feature> features = new ArrayList<>();
    private final Map<Long, Feature[]> slices = new ConcurrentHashMap<>();

    private final MeteorPiece m;
    private final RandomSource rnd;

    private Dungeon(MeteorPiece m) {
        this.m = m;
        this.rnd = RandomSource.create(m.seed() ^ 0x5DEECE66DL * 31);
    }

    public static Dungeon of(MeteorPiece m) {
        Dungeon d = (Dungeon) m.dungeon;
        if (d == null) {
            synchronized (m) {
                d = (Dungeon) m.dungeon;
                if (d == null) {
                    d = new Dungeon(m);
                    d.build();
                    m.dungeon = d;
                }
            }
        }
        return d;
    }

    /** Whether a tree or pillar rooted at (x, z) would grow into a shack. */
    public static boolean excludes(MeteorPiece m, int x, int z) {
        for (Shack s : of(m).shacks) {
            if (Math.abs(x - s.x) <= s.half + 6 && Math.abs(z - s.z) <= s.half + 6) return true;
        }
        return false;
    }

    /** Every feature's box (for the self-test). */
    public List<BoundingBox> boxes() {
        List<BoundingBox> out = new ArrayList<>();
        for (Feature f : features) out.add(f.box());
        return out;
    }

    /** Features whose boxes reach into the chunk column at (chunkX, chunkZ). */
    Feature[] slice(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        return slices.computeIfAbsent(key, k -> {
            int x0 = chunkX << 4, z0 = chunkZ << 4;
            List<Feature> out = new ArrayList<>();
            for (Feature f : features) {
                BoundingBox b = f.box();
                if (b.maxX() >= x0 && b.minX() <= x0 + 15 && b.maxZ() >= z0 && b.minZ() <= z0 + 15) out.add(f);
            }
            return out.toArray(new Feature[0]);
        });
    }

    // ------------------------------------------------------------------------------------------------ building

    private boolean safe(int x, int y, int z) {
        return m.shellDepth(x, y, z) >= SAFE;
    }

    private boolean roomFits(int cx, int cz, int hx, int hz, int floor, int height) {
        int[] ys = {floor - 2, floor + height + 1};
        for (int y : ys) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) continue;
                    if (!safe(cx + i * (hx + 2), y, cz + j * (hz + 2))) return false;
                }
            }
        }
        return true;
    }

    private boolean free(int cx, int cz, int hx, int hz, int level, int gap) {
        for (Room r : rooms) if (r.level == level && r.overlaps(cx, cz, hx, hz, gap)) return false;
        return true;
    }

    private boolean segmentFits(int ax, int az, int bx, int bz, int floor) {
        int len = Math.abs(bx - ax) + Math.abs(bz - az);
        int sx = Integer.signum(bx - ax), sz = Integer.signum(bz - az);
        for (int t = 0; t <= len; t += 3) {
            int x = ax + sx * t, z = az + sz * t;
            // lateral corners at floor and ceiling
            if (!safe(x + 2 * Math.abs(sz), floor - 2, z + 2 * Math.abs(sx))) return false;
            if (!safe(x - 2 * Math.abs(sz), floor - 2, z - 2 * Math.abs(sx))) return false;
            if (!safe(x, floor + TUNNEL_H + 1, z)) return false;
        }
        return true;
    }

    /** A tunnel must not run through a room other than the two it joins (it would cut across its walls). */
    private boolean segmentClear(int ax, int az, int bx, int bz, int level, Room a, Room b) {
        int x0 = Math.min(ax, bx) - 1, x1 = Math.max(ax, bx) + 1, z0 = Math.min(az, bz) - 1, z1 = Math.max(az, bz) + 1;
        for (Room r : rooms) {
            if (r.level != level || r == a || r == b) continue;
            if (x1 >= r.cx - r.hx - 2 && x0 <= r.cx + r.hx + 2 && z1 >= r.cz - r.hz - 2 && z0 <= r.cz + r.hz + 2) return false;
        }
        return true;
    }

    private void build() {
        int bf = m.baseFloor();
        floors[0] = bf - DEPTH[0];
        floors[1] = bf - DEPTH[1];
        int mx = m.centre().getX(), mz = m.centre().getZ();

        // Level 1 (deep) first: its stair room decides where level 0's stair room goes.
        gridRooms(1, 15);
        Room vault = null;
        for (Room r : rooms) if (r.level == 1 && (vault == null || r.hx * r.hz > vault.hx * vault.hz)) vault = r;
        if (vault != null) {
            vault.kind = Kind.VAULT;
            // grow it if there's room
            for (int g = 2; g >= 1; g--) {
                int hx = vault.hx + g, hz = vault.hz + g;
                Room v = vault;
                rooms.remove(v);
                if (free(v.cx, v.cz, hx, hz, 1, 2) && roomFits(v.cx, v.cz, hx, hz, v.floor, VAULT_H)) {
                    vault = new Room(1, v.cx, v.cz, hx, hz, v.floor, VAULT_H, Kind.VAULT);
                    rooms.add(vault);
                    break;
                }
                rooms.add(v);
            }
        }

        // Stair: the deep room nearest the middle that also fits a room straight above it.
        List<Room> deep = new ArrayList<>();
        for (Room r : rooms) if (r.level == 1) deep.add(r);
        deep.sort(Comparator.comparingDouble(r -> Math.hypot(r.cx - mx, r.cz - mz)));
        Room stairDeep = null;
        for (Room r : deep) {
            if (free(r.cx, r.cz, 3, 3, 0, 2) && roomFits(r.cx, r.cz, 3, 3, floors[0], ROOM_H)) {
                stairDeep = r;
                rooms.add(new Room(0, r.cx, r.cz, 3, 3, floors[0], ROOM_H, Kind.STAIR));
                if (r.kind != Kind.VAULT) r.kind = Kind.STAIR;
                shafts.add(new Shaft(r.cx, r.cz, floors[0] - 1, floors[1], 1 + 2 * rnd.nextInt(4)));
                break;
            }
        }
        if (stairDeep == null) rooms.removeIf(r -> r.level == 1); // no way down: no deep level

        gridRooms(0, 17);
        // Entrances: shacks built straight over level-0 rooms, up to 3.
        List<Room> top = new ArrayList<>();
        for (Room r : rooms) if (r.level == 0 && r.kind != Kind.STAIR) top.add(r);
        for (int i = top.size() - 1; i > 0; i--) Collections.swap(top, i, rnd.nextInt(i + 1));
        int entries = 0;
        for (Room r : top) {
            if (entries >= 3) break;
            Shack sh = site(r.cx, r.cz, 5, true);
            if (sh == null) continue;
            shacks.add(sh);
            r.kind = Kind.ENTRY;
            shafts.add(new Shaft(r.cx, r.cz, sh.floor - 1, floors[0], 1 + 2 * rnd.nextInt(4)));
            entries++;
        }
        connect(0);
        boolean stairKept = false;
        for (Room r : rooms) if (r.level == 0 && r.kind == Kind.STAIR) stairKept = true;
        if (!stairKept) { // the way down got cut off from the entrances: no deep level
            rooms.removeIf(r -> r.level == 1);
            tunnels.removeIf(t -> t.floor == floors[1]);
            shafts.removeIf(sh -> sh.bottom == floors[1]);
        }
        connect(1);
        // Nest shacks on the surface.
        int want = 4 + rnd.nextInt(3);
        for (int attempt = 0; attempt < 80 && want > 0; attempt++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double minR = ArenaLayout.PLAZA_R + 6, rad = minR + rnd.nextDouble() * Math.max(1, m.radius() - minR);
            Shack sh = site(mx + (int) Math.round(Math.cos(ang) * rad), mz + (int) Math.round(Math.sin(ang) * rad), 3, false);
            if (sh != null) { shacks.add(sh); want--; }
        }
        branches();
        for (Room r : rooms) furnish(r);
        for (Shack s : shacks) furnish(s);

        for (Room r : rooms) features.add(new Feature(r.box(), r));
        for (Tunnel t : tunnels) features.add(new Feature(t.box(), t));
        for (Shaft s : shafts) features.add(new Feature(s.box(), s));
        for (Shack s : shacks) features.add(new Feature(s.box(), s));
    }

    private void gridRooms(int level, int grid) {
        int mx = m.centre().getX(), mz = m.centre().getZ();
        int floor = floors[level];
        int n = m.radius() / grid + 1;
        for (int gx = -n; gx <= n; gx++) {
            for (int gz = -n; gz <= n; gz++) {
                float keep = rnd.nextFloat();
                int x = mx + gx * grid + rnd.nextInt(7) - 3, z = mz + gz * grid + rnd.nextInt(7) - 3;
                int hx = 3 + rnd.nextInt(3), hz = 3 + rnd.nextInt(3);
                float k = rnd.nextFloat();
                if (keep > 0.8f) continue;
                if (!free(x, z, hx, hz, level, 3) || !roomFits(x, z, hx, hz, floor, ROOM_H)) continue;
                Kind kind = k < 0.4f ? Kind.NEST : k < 0.65f ? Kind.STORE : Kind.HALL;
                rooms.add(new Room(level, x, z, hx, hz, floor, ROOM_H, kind));
            }
        }
    }

    /** Kruskal over room pairs, so every room joins up where a tunnel fits; then a few extra loops. */
    private void connect(int level) {
        List<Room> rs = new ArrayList<>();
        for (Room r : rooms) if (r.level == level) rs.add(r);
        if (rs.size() < 2) return;
        int n = rs.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int d = Math.abs(rs.get(i).cx - rs.get(j).cx) + Math.abs(rs.get(i).cz - rs.get(j).cz);
                if (d <= 64) pairs.add(new int[]{i, j, d});
            }
        }
        pairs.sort(Comparator.comparingInt(p -> p[2]));
        List<int[]> spare = new ArrayList<>();
        for (int[] p : pairs) {
            int a = find(parent, p[0]), b = find(parent, p[1]);
            if (a == b) {
                spare.add(p);
                continue;
            }
            if (link(rs.get(p[0]), rs.get(p[1]), level)) parent[a] = b;
        }
        // Loops make it a maze rather than a tree.
        int extra = Math.max(1, n / 4);
        for (int[] p : spare) {
            if (extra <= 0) break;
            if (p[2] > 40 || rnd.nextFloat() > 0.5f) continue;
            if (link(rs.get(p[0]), rs.get(p[1]), level)) extra--;
        }
        // Keep the group with the stair and the most entrances; drop rooms cut off from it.
        int[] score = new int[n];
        for (int i = 0; i < n; i++) {
            Kind k = rs.get(i).kind;
            score[find(parent, i)] += k == Kind.STAIR ? 100 : k == Kind.ENTRY ? 60 : 1;
        }
        int root = 0;
        for (int i = 0; i < n; i++) if (score[i] > score[root]) root = i;
        int rootSet = find(parent, root);
        List<Room> gone = new ArrayList<>();
        for (int i = 0; i < n; i++) if (find(parent, i) != rootSet) gone.add(rs.get(i));
        rooms.removeAll(gone);
        tunnels.removeIf(t -> gone.contains(t.a) || gone.contains(t.b));
        for (Shack s : new ArrayList<>(shacks)) {
            if (!s.entry) continue;
            boolean kept = false;
            for (Room r : rooms) if (r.kind == Kind.ENTRY && r.cx == s.x && r.cz == s.z) kept = true;
            if (!kept) { // its room got cut off: no shaft, it holds a nest instead
                shacks.remove(s);
                shacks.add(new Shack(s.x, s.z, s.floor, s.half, s.ground, s.door, false));
                shafts.removeIf(sh -> sh.x == s.x && sh.z == s.z);
            }
        }
    }

    private static int find(int[] p, int i) {
        while (p[i] != i) i = p[i] = p[p[i]];
        return i;
    }

    /** L-shaped tunnel between two room centres; tries both corner orders. */
    private boolean link(Room a, Room b, int level) {
        int floor = floors[level];
        boolean xFirst = rnd.nextBoolean();
        for (int attempt = 0; attempt < 2; attempt++, xFirst = !xFirst) {
            int kx = xFirst ? b.cx : a.cx, kz = xFirst ? a.cz : b.cz;
            if (a.cx == b.cx || a.cz == b.cz) { kx = b.cx; kz = b.cz; }
            if (!segmentFits(a.cx, a.cz, kx, kz, floor) || !segmentFits(kx, kz, b.cx, b.cz, floor)) continue;
            if (!segmentClear(a.cx, a.cz, kx, kz, level, a, b) || !segmentClear(kx, kz, b.cx, b.cz, level, a, b)) continue;
            boolean rails = rnd.nextFloat() < 0.3f;
            for (Tunnel t : new Tunnel[]{
                    a.cx != kx || a.cz != kz ? new Tunnel(a.cx, a.cz, kx, kz, floor, rails, false) : null,
                    kx != b.cx || kz != b.cz ? new Tunnel(kx, kz, b.cx, b.cz, floor, rails, false) : null}) {
                if (t == null) continue;
                t.a = a;
                t.b = b;
                tunnels.add(t);
            }
            return true;
        }
        return false;
    }

    /** Short dead ends off long tunnels, each ending in a chest, a nest or a cave-in. */
    private void branches() {
        for (Tunnel t : new ArrayList<>(tunnels)) {
            if (t.deadEnd || t.length() < 14 || rnd.nextFloat() > 0.45f) continue;
            int mid = t.length() / 2 + rnd.nextInt(5) - 2;
            int ax = t.alongX ? t.x0 + mid : t.x0, az = t.alongX ? t.z0 : t.z0 + mid;
            int side = rnd.nextBoolean() ? 1 : -1, len = 6 + rnd.nextInt(6);
            int bx = t.alongX ? ax : ax + side * len, bz = t.alongX ? az + side * len : az;
            int level = t.floor == floors[0] ? 0 : 1;
            if (!segmentFits(ax, az, bx, bz, t.floor) || !segmentClear(ax, az, bx, bz, level, null, null)) continue;
            boolean crosses = false;
            for (Tunnel o : tunnels) {
                if (o == t || o.floor != t.floor) continue;
                if (Math.max(ax, bx) + 2 >= o.x0 && Math.min(ax, bx) - 2 <= o.x1 && Math.max(az, bz) + 2 >= o.z0 && Math.min(az, bz) - 2 <= o.z1) crosses = true;
            }
            if (crosses) continue;
            tunnels.add(new Tunnel(ax, az, bx, bz, t.floor, false, true));
            float roll = rnd.nextFloat();
            PropKind end = roll < 0.4f ? PropKind.CHEST : roll < 0.65f ? PropKind.NEST : PropKind.RUBBLE;
            Direction back = t.alongX ? (side > 0 ? Direction.NORTH : Direction.SOUTH) : (side > 0 ? Direction.WEST : Direction.EAST);
            prop(bx, t.floor, bz, end, back, false);
        }
    }

    private void prop(int x, int y, int z, PropKind kind, Direction facing, boolean vault) {
        props.put(BlockPos.asLong(x, y, z), new Prop(kind, facing, vault));
    }

    private static boolean lane(Room r, int x, int z) {
        return Math.abs(x - r.cx) <= 1 || Math.abs(z - r.cz) <= 1;
    }

    private void furnish(Room r) {
        int y = r.floor;
        switch (r.kind) {
            case NEST -> {
                prop(r.cx + (r.hx >= 4 ? 2 : 0), y, r.cz + (r.hz >= 4 ? 2 : 0), PropKind.NEST, Direction.NORTH, false);
                if (r.hx >= 5 && r.hz >= 4) prop(r.cx - 3, y, r.cz - 3, PropKind.NEST, Direction.NORTH, false);
            }
            case STORE -> {
                int chests = 1 + rnd.nextInt(2);
                for (int i = 0; i < 40 && chests > 0; i++) {
                    int x = r.cx - r.hx + rnd.nextInt(2 * r.hx + 1), z = r.cz - r.hz + rnd.nextInt(2 * r.hz + 1);
                    boolean wallX = Math.abs(x - r.cx) == r.hx, wallZ = Math.abs(z - r.cz) == r.hz;
                    if (!(wallX || wallZ) || lane(r, x, z) || props.containsKey(BlockPos.asLong(x, y, z))) continue;
                    Direction f = wallX ? (x > r.cx ? Direction.WEST : Direction.EAST) : (z > r.cz ? Direction.NORTH : Direction.SOUTH);
                    prop(x, y, z, PropKind.CHEST, f, false);
                    chests--;
                }
                for (int x = r.cx - r.hx; x <= r.cx + r.hx; x++) {
                    for (int z = r.cz - r.hz; z <= r.cz + r.hz; z++) {
                        boolean wall = Math.abs(x - r.cx) == r.hx || Math.abs(z - r.cz) == r.hz;
                        if (!wall || lane(r, x, z) || props.containsKey(BlockPos.asLong(x, y, z)) || rnd.nextFloat() > 0.35f) continue;
                        prop(x, y, z, PropKind.BARREL, Direction.UP, false);
                        if (rnd.nextFloat() < 0.3f) prop(x, y + 1, z, PropKind.BARREL, Direction.UP, false);
                    }
                }
            }
            case VAULT -> {
                // Chests against the two walls across the shorter axis, nests in the corners.
                boolean alongX = r.hx >= r.hz;
                for (int s = -1; s <= 1; s += 2) {
                    int x = alongX ? r.cx + s * r.hx : r.cx + 3, z = alongX ? r.cz + 3 : r.cz + s * r.hz;
                    Direction f = alongX ? (s > 0 ? Direction.WEST : Direction.EAST) : (s > 0 ? Direction.NORTH : Direction.SOUTH);
                    prop(x, y, z, PropKind.CHEST, f, true);
                }
                prop(r.cx + r.hx - 1, y, r.cz + r.hz - 1, PropKind.NEST, Direction.NORTH, true);
                prop(r.cx - r.hx + 1, y, r.cz - r.hz + 1, PropKind.NEST, Direction.NORTH, true);
            }
            case ENTRY -> {
                for (int i = 0; i < 6; i++) {
                    int x = r.cx + (rnd.nextBoolean() ? r.hx : -r.hx), z = r.cz - r.hz + rnd.nextInt(2 * r.hz + 1);
                    if (!lane(r, x, z)) prop(x, y, z, PropKind.BARREL, Direction.UP, false);
                }
            }
            default -> {}
        }
    }

    private void furnish(Shack s) {
        if (s.entry) {
            // A chest in the back corner, away from the door.
            Direction back = s.door.getOpposite(), side = s.door.getClockWise();
            int k = s.half - 1;
            prop(s.x + back.getStepX() * k + side.getStepX() * k, s.floor, s.z + back.getStepZ() * k + side.getStepZ() * k,
                    PropKind.CHEST, s.door, false);
            return;
        }
        prop(s.x, s.floor, s.z, PropKind.NEST, Direction.NORTH, false);
        Direction back = s.door.getOpposite(), side = s.door.getCounterClockWise();
        int k = s.half - 1;
        prop(s.x + back.getStepX() * k + side.getStepX() * k, s.floor, s.z + back.getStepZ() * k + side.getStepZ() * k,
                PropKind.CHEST, s.door, false);
        int bx = s.x + back.getStepX() * k - side.getStepX() * k, bz = s.z + back.getStepZ() * k - side.getStepZ() * k;
        prop(bx, s.floor, bz, PropKind.BARREL, Direction.UP, false);
    }

    /**
     * A shack centred on (x, z) if one fits there: on the interior floor outside the arena plaza, not in front of the
     * arena door, clear of other shacks, on ground no more than 4 blocks uneven, and under the dome. Door faces the arena.
     */
    private Shack site(int x, int z, int half, boolean entry) {
        int mx = m.centre().getX(), mz = m.centre().getZ();
        double rad = Math.hypot(x - mx, z - mz);
        if (rad < ArenaLayout.PLAZA_R + half + 3) return null;
        Direction arenaDoor = ArenaGen.door(m);
        double ang = Math.atan2(z - mz, x - mx), doorAngle = Math.atan2(arenaDoor.getStepZ(), arenaDoor.getStepX());
        double da = Math.abs(Math.atan2(Math.sin(ang - doorAngle), Math.cos(ang - doorAngle)));
        if (da < Math.toRadians(30) && rad < 45) return null;
        for (Shack o : shacks) if (Math.abs(o.x - x) <= o.half + half + 6 && Math.abs(o.z - z) <= o.half + half + 6) return null;
        int hi = Integer.MIN_VALUE, lo = Integer.MAX_VALUE;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int ft = m.style().interior().surface(m, x + i * half, z + j * half);
                hi = Math.max(hi, ft);
                lo = Math.min(lo, ft);
            }
        }
        if (hi - lo > 4 || m.style().interior().surface(m, x, z) < m.baseFloor() - 1) return null;
        int floor = hi + 1, roof = floor + 4 + half + 1;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int px = x + i * (half + 2), pz = z + j * (half + 2);
                if (!safe(px, floor, pz) || m.shellDepth(px, roof + 2, pz) < 6) return null;
            }
        }
        Direction door = Direction.fromYRot(Math.toDegrees(Math.atan2(mz - z, mx - x)) - 90);
        if (m.style() == com.bromax.exofall.world.MeteorStyle.HIVE) {
            // face across the comb sheets (they run along the other axis), still towards the arena
            boolean sheetsAlongX = (m.seed() & 1) == 0; // HiveInterior: u = x then, so sheets cross the x axis
            door = sheetsAlongX ? (mx > x ? Direction.EAST : Direction.WEST) : (mz > z ? Direction.SOUTH : Direction.NORTH);
        }
        return new Shack(x, z, floor, half, lo, door, entry);
    }
}
