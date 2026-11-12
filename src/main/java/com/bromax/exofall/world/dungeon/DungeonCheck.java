package com.bromax.exofall.world.dungeon;

import com.bromax.exofall.block.NestBlock;
import com.bromax.exofall.world.MeteorPiece;
import com.bromax.exofall.world.dungeon.Dungeon.Prop;
import com.bromax.exofall.world.dungeon.Dungeon.PropKind;
import com.bromax.exofall.world.dungeon.Dungeon.Room;
import com.bromax.exofall.world.dungeon.Dungeon.Shack;
import com.bromax.exofall.world.dungeon.Dungeon.Tunnel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * /exofall dungeon check: walks a generated Warren the way a player could (step up 1, drop up to 3, 2 blocks of
 * headroom, doors count as open) starting inside every shack, then reports anything a player can't get to:
 * rooms, tunnels, chests/barrels/nests (must have a reachable cell next to them; chests also need a free lid),
 * and shack doors that don't lead back out. Also writes a top-down map per level to the run folder.
 */
public final class DungeonCheck {
    private final ServerLevel level;
    private final MeteorPiece m;
    private final Dungeon d;
    private final List<BoundingBox> region = new ArrayList<>();
    private final Set<Long> reached = new HashSet<>();

    private DungeonCheck(ServerLevel level, MeteorPiece m) {
        this.level = level;
        this.m = m;
        m.initNoise();
        this.d = Dungeon.of(m);
        for (BoundingBox b : d.boxes()) region.add(b.inflatedBy(2));
    }

    public static List<String> run(ServerLevel level, MeteorPiece m) {
        return new DungeonCheck(level, m).check();
    }

    private boolean inRegion(BlockPos p) {
        for (BoundingBox b : region) if (b.isInside(p)) return true;
        return false;
    }

    private boolean passable(BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof DoorBlock) return true;
        if (s.is(Blocks.LAVA)) return false;
        return s.getCollisionShape(level, p).isEmpty();
    }

    private boolean support(BlockPos p) {
        BlockState s = level.getBlockState(p);
        return !s.getCollisionShape(level, p).isEmpty() && !(s.getBlock() instanceof DoorBlock) || !s.getFluidState().isEmpty();
    }

    private boolean node(BlockPos feet) {
        return passable(feet) && passable(feet.above()) && support(feet.below());
    }

    private void flood(List<BlockPos> starts) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        for (BlockPos s : starts) if (node(s) && reached.add(s.asLong())) q.add(s);
        while (!q.isEmpty()) {
            BlockPos c = q.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(dir);
                // up one (needs room over our head to jump), level, or down up to 3
                if (passable(c.above(2)) && node(n.above()) ) add(q, n.above());
                if (node(n)) { add(q, n); continue; }
                BlockPos t = n;
                for (int drop = 1; drop <= 3; drop++) {
                    if (!passable(t) || !passable(t.above())) break;
                    t = t.below();
                    if (node(t)) { add(q, t); break; }
                }
            }
        }
    }

    private void add(ArrayDeque<BlockPos> q, BlockPos p) {
        if (inRegion(p) && reached.add(p.asLong())) q.add(p);
    }

    private boolean at(int x, int y, int z) {
        return reached.contains(BlockPos.asLong(x, y, z));
    }

    /** A reachable standing cell beside the block (same height, one up or one down), or on top of it. */
    private boolean reachableNear(BlockPos p) {
        if (reached.contains(p.above().asLong())) return true;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos n = p.relative(dir);
            for (int dy = -1; dy <= 1; dy++) if (reached.contains(n.above(dy).asLong())) return true;
        }
        return false;
    }

    /** Within 2 cells of a dead end's far end that holds a prop (cave-in, chest or nest). */
    private boolean nearEndProp(Tunnel t, int x, int z) {
        int[][] ends = {{t.x0, t.z0}, {t.x1, t.z1}};
        for (int[] e : ends) {
            if (!d.props.containsKey(BlockPos.asLong(e[0], t.floor, e[1]))) continue;
            if (Math.abs(x - e[0]) + Math.abs(z - e[1]) <= 2) return true;
        }
        return false;
    }

    private List<String> check() {
        List<String> out = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        List<BlockPos> starts = new ArrayList<>();
        for (Shack s : d.shacks) starts.add(new BlockPos(s.x + s.door.getStepX() * (s.half - 1), s.floor, s.z + s.door.getStepZ() * (s.half - 1)));
        flood(starts);

        int rooms = 0, roomsOk = 0;
        for (Room r : d.rooms) {
            rooms++;
            boolean ok = false;
            for (int x = r.cx - r.hx; x <= r.cx + r.hx && !ok; x++)
                for (int z = r.cz - r.hz; z <= r.cz + r.hz && !ok; z++)
                    for (int y = r.floor; y < r.floor + r.height && !ok; y++) ok = at(x, y, z);
            if (ok) roomsOk++;
            else bad.add("room L" + r.level + " " + r.kind + " at " + r.cx + " " + r.floor + " " + r.cz + " unreachable");
        }
        int tun = 0, tunOk = 0;
        for (Tunnel t : d.tunnels) {
            tun++;
            String fail = null;
            for (int x = t.x0; x <= t.x1 && fail == null; x++) {
                for (int z = t.z0; z <= t.z1 && fail == null; z++) {
                    boolean inRoom = false;
                    for (Room r : d.rooms) if (r.floor == t.floor && Math.abs(x - r.cx) <= r.hx + 1 && Math.abs(z - r.cz) <= r.hz + 1) inRoom = true;
                    if (inRoom) continue;
                    if (t.deadEnd && nearEndProp(t, x, z)) continue; // the cave-in pile / the chest itself
                    boolean ok = false;
                    for (int dy = 0; dy <= 2 && !ok; dy++) for (int l = -1; l <= 1 && !ok; l++)
                        ok = t.alongX ? at(x, t.floor + dy, z + l) : at(x + l, t.floor + dy, z);
                    if (!ok) {
                        StringBuilder sb = new StringBuilder();
                        for (int dy = -1; dy <= 2; dy++) sb.append(' ').append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(x, t.floor + dy, z)).getBlock()).getPath());
                        fail = "tunnel " + (t.deadEnd ? "dead end " : "") + "cell " + x + " " + t.floor + " " + z + " unreachable:" + sb;
                    }
                }
            }
            if (fail == null) tunOk++;
            else bad.add(fail);
        }
        // Water that got into the Warren (from swamp pools above, say).
        int wet = 0;
        BlockPos firstWet = null;
        for (Room r : d.rooms) for (int x = r.cx - r.hx; x <= r.cx + r.hx; x++) for (int z = r.cz - r.hz; z <= r.cz + r.hz; z++)
            for (int y = r.floor; y < r.floor + r.height; y++) {
                BlockPos p = new BlockPos(x, y, z);
                if (!level.getFluidState(p).isEmpty()) { wet++; if (firstWet == null) firstWet = p; }
            }
        if (wet > 0) bad.add(wet + " water cells inside rooms, first at " + firstWet.toShortString());
        // Shacks must stand on something: no air straight under the floor.
        for (Shack s : d.shacks) {
            int gaps = 0;
            for (int x = s.x - s.half; x <= s.x + s.half; x++) for (int z = s.z - s.half; z <= s.z + s.half; z++) {
                if (s.entry && Math.abs(x - s.x) <= 2 && Math.abs(z - s.z) <= 2) continue;
                if (level.getBlockState(new BlockPos(x, s.floor - 2, z)).isAir()) gaps++;
            }
            if (gaps > 0) bad.add("shack at " + s.x + " " + s.floor + " " + s.z + " has " + gaps + " air gaps under its floor");
        }
        int props = 0, propsOk = 0;
        for (var e : d.props.entrySet()) {
            Prop p = e.getValue();
            if (p.kind() == PropKind.RUBBLE) continue;
            BlockPos pos = BlockPos.of(e.getKey());
            BlockState s = level.getBlockState(pos);
            props++;
            boolean right = switch (p.kind()) {
                case CHEST -> s.is(Blocks.CHEST);
                case BARREL -> s.is(Blocks.BARREL);
                case NEST -> s.getBlock() instanceof NestBlock;
                default -> true;
            };
            String what = p.kind() + " at " + pos.toShortString();
            if (!right) { bad.add(what + " is " + s.getBlock().getName().getString()); continue; }
            if (!reachableNear(pos) && !(p.kind() == PropKind.BARREL && reachableNear(pos.below()))) { bad.add(what + " out of reach"); continue; }
            if (p.kind() == PropKind.CHEST && level.getBlockState(pos.above()).isRedstoneConductor(level, pos.above())) { bad.add(what + " lid blocked"); continue; }
            if (p.kind() == PropKind.CHEST && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c && c.getLootTable() == null) {
                bad.add(what + " has no loot table"); continue;
            }
            propsOk++;
        }
        int shacks = 0, doorsOk = 0;
        for (Shack s : d.shacks) {
            shacks++;
            int ox = s.x + s.door.getStepX() * (s.half + 5), oz = s.z + s.door.getStepZ() * (s.half + 5);
            boolean ok = false;
            for (int y = s.floor + 2; y >= s.ground - 6 && !ok; y--) ok = at(ox, y, oz);
            if (ok) doorsOk++;
            else bad.add((s.entry ? "entry" : "nest") + " shack at " + s.x + " " + s.floor + " " + s.z + ": door doesn't lead out");
        }
        out.add(m.style().getSerializedName() + " Warren, r=" + m.radius() + ": " + d.shacks.stream().filter(s -> s.entry).count()
                + " entrances, " + d.shafts.size() + " shafts, " + reached.size() + " walkable cells");
        out.add("rooms " + roomsOk + "/" + rooms + ", tunnels " + tunOk + "/" + tun + ", chests/barrels/nests " + propsOk + "/" + props
                + ", shack doors " + doorsOk + "/" + shacks);
        out.addAll(bad.size() > 12 ? bad.subList(0, 12) : bad);
        if (bad.size() > 12) out.add("... " + (bad.size() - 12) + " more");
        out.add(bad.isEmpty() ? "PASS" : "FAIL (" + bad.size() + ")");
        for (int lv = 0; lv < 2; lv++) map(lv);
        // Nests: one must let a bug out.
        for (var e : d.props.entrySet()) {
            if (e.getValue().kind() != PropKind.NEST) continue;
            BlockPos np = BlockPos.of(e.getKey());
            boolean ok = false;
            for (int i = 0; i < 5 && !ok; i++) ok = com.bromax.exofall.block.NestBlockEntity.spawnFor(level, np, m.style());
            out.add(ok ? "nest at " + np.toShortString() + " spawned a bug" : "nest at " + np.toShortString() + " could not spawn");
            break;
        }
        for (Shack s : d.shacks) { shackSlices(s); break; }
        return out;
    }

    /** Two cut-aways through a shack's middle (across and along the ridge), a block per 6 px. */
    private void shackSlices(Shack s) {
        int w = 2 * (s.half + 6) + 1, h = s.roofTop() - (s.ground - 6) + 3, sc = 6;
        BufferedImage img = new BufferedImage((2 * w + 2) * sc, h * sc, BufferedImage.TYPE_INT_RGB);
        for (int view = 0; view < 2; view++) {
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int off = i - w / 2, y = s.roofTop() + 2 - j;
                    BlockPos p = view == 0 ? new BlockPos(s.x + off, y, s.z) : new BlockPos(s.x, y, s.z + off);
                    BlockState b = level.getBlockState(p);
                    String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath();
                    int c;
                    if (b.isAir()) c = 0x0c0c14;
                    else if (b.getBlock() instanceof NestBlock) c = 0xFF30C0;
                    else if (b.is(Blocks.CHEST)) c = 0xFFD020;
                    else if (b.is(Blocks.LANTERN)) c = 0xFFF080;
                    else if (id.endsWith("_stairs")) c = 0xD8A060;
                    else if (id.endsWith("_slab")) c = 0xF0C890;
                    else if (id.endsWith("_door")) c = 0x6040A0;
                    else if (id.endsWith("_log")) c = 0x5A3418;
                    else if (id.endsWith("_planks")) c = 0xA06A34;
                    else if (id.endsWith("_wall") || id.endsWith("_fence")) c = 0x9090A0;
                    else if (!b.getFluidState().isEmpty()) c = 0x2050C0;
                    else c = 0x505860;
                    for (int a = 0; a < sc; a++) for (int bb = 0; bb < sc; bb++) img.setRGB((view * (w + 2) + i) * sc + a, j * sc + bb, c);
                }
            }
        }
        try {
            ImageIO.write(img, "png", new File("shack_" + m.style().getSerializedName() + (s.entry ? "_entry" : "_nest") + ".png"));
        } catch (Exception ignored) {
        }
    }

    private void map(int lv) {
        int y = d.floors[lv];
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
        for (Room r : d.rooms) {
            if (r.level != lv) continue;
            x0 = Math.min(x0, r.cx - r.hx - 4); x1 = Math.max(x1, r.cx + r.hx + 4);
            z0 = Math.min(z0, r.cz - r.hz - 4); z1 = Math.max(z1, r.cz + r.hz + 4);
        }
        for (Tunnel t : d.tunnels) {
            if (t.floor != y) continue;
            x0 = Math.min(x0, t.x0 - 4); x1 = Math.max(x1, t.x1 + 4);
            z0 = Math.min(z0, t.z0 - 4); z1 = Math.max(z1, t.z1 + 4);
        }
        if (x0 > x1) return;
        int sc = 4, w = x1 - x0 + 1, h = z1 - z0 + 1;
        BufferedImage img = new BufferedImage(w * sc, h * sc, BufferedImage.TYPE_INT_RGB);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = level.getBlockState(p);
                int c;
                if (s.is(Blocks.CHEST)) c = 0xFFD020;
                else if (s.is(Blocks.BARREL)) c = 0x9A6A30;
                else if (s.getBlock() instanceof NestBlock) c = 0xFF30C0;
                else if (s.getBlock() instanceof BaseRailBlock) c = at(x, y, z) ? 0x6AA0A0 : 0xA04040;
                else if (at(x, y, z) || at(x, y + 1, z) || at(x, y - 1, z)) c = 0x3C9A48;
                else if (passable(p) && passable(p.above())) c = 0xC03030;   // open but unreachable
                else if (passable(p) || passable(p.above())) c = 0x303844;  // low/blocked cell
                else c = 0x101014;
                for (int i = 0; i < sc; i++) for (int j = 0; j < sc; j++) img.setRGB((x - x0) * sc + i, (z - z0) * sc + j, c);
            }
        }
        try {
            ImageIO.write(img, "png", new File("dungeon_" + m.style().getSerializedName() + "_L" + lv + ".png"));
        } catch (Exception ignored) {
        }
    }
}
