package com.bromax.exofall.world;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One meteor: a noise-warped ellipsoid shell of crust around a hollow interior. The interior is filled
 * by the meteor's {@link MeteorStyle}. Every block is a pure function of (position, seed), so each chunk
 * carves its own slice without caring which neighbours already generated.
 *
 * <p>Interior biome: biomes are already fixed when structures place, and the decoration pass only runs
 * features for biomes the dimension's biome source knows. So the style decorates the interior itself and
 * the piece then rewrites the 4x4x4 biome cells inside the interior (what /fillbiome does). That gives the
 * meteor its own fog, sky, music, particles and spawns, and keeps biome-filtered overworld features out.
 */
public class MeteorPiece extends StructurePiece {
    private static final double Y_SQUASH = 1.0;      // vertical radius as a fraction of horizontal (1.0 = round)
    private static final double SHELL_WARP = 0.13;   // radius varies by up to ±13%
    private static final double CRUST_THICKNESS = 5.0;
    private static final double FLOOR_FRACTION = 0.30; // base floor sits 30% of the vertical radius below centre

    // Crater, in multiples of the radius (t = horizontal distance / radius, warped by noise).
    private static final double BOWL_DEPTH = 0.15;   // bowl floor sits this far below the centre height
    private static final double BOWL_FLAT = 1.15;    // ground fully levelled to the bowl floor inside this
    private static final double BOWL_EDGE = 1.45;    // levelling fades out by here
    private static final double RIM_PEAK = 1.45, RIM_HALF_WIDTH = 0.40, RIM_HEIGHT = 0.12;
    private static final double CRATER_END = RIM_PEAK + RIM_HALF_WIDTH; // ejecta fades out here too
    private static final double RING_WARP = 0.10;
    /**
     * Hard cap on how far the piece may reach from the centre. Chunks only reference structure starts up to
     * 8 chunks away, so blocks past ~135 never place. Big meteors get a crater squeezed into what's left.
     */
    private static final int MAX_REACH = 132;

    public static final int OUTSIDE = 0, CRUST = 1, INTERIOR = 2;

    private final BlockPos centre;
    private final int radius;
    private final long seed;
    private final ResourceKey<Biome> interiorBiome;
    private final MeteorStyle style;

    private volatile ImprovedNoise shellNoise, noiseA, noiseB;
    /** Per-meteor cache for styles (e.g. tree layouts). Chunks generate on several threads. */
    public final Map<Long, Object> cache = new ConcurrentHashMap<>();

    public MeteorPiece(BlockPos centre, int radius, long seed, ResourceKey<Biome> interiorBiome, MeteorStyle style) {
        super(ModWorldgen.METEOR_PIECE.get(), 0, box(centre, radius));
        this.centre = centre;
        this.radius = radius;
        this.seed = seed;
        this.interiorBiome = interiorBiome;
        this.style = style;
    }

    public MeteorPiece(CompoundTag tag) {
        super(ModWorldgen.METEOR_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.radius = tag.getInt("R");
        this.seed = tag.getLong("Seed");
        this.interiorBiome = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(tag.getString("Biome")));
        this.style = MeteorStyle.byName(tag.getString("Style"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", centre.getX());
        tag.putInt("CY", centre.getY());
        tag.putInt("CZ", centre.getZ());
        tag.putInt("R", radius);
        tag.putLong("Seed", seed);
        tag.putString("Biome", interiorBiome.location().toString());
        tag.putString("Style", style.getSerializedName());
    }

    static int horizontalReach(int radius) {
        return (int) Math.ceil(radius * (1 + SHELL_WARP)) + 1;
    }

    /** How far the crater ring reaches from the centre. */
    static int craterReach(int radius) {
        return (int) Math.ceil(radius * craterEnd(radius) * (1 + RING_WARP)) + 1;
    }

    /** Crater end in multiples of the radius: CRATER_END, or less if that would pass MAX_REACH. */
    private static double craterEnd(int radius) {
        return Math.min(CRATER_END, (MAX_REACH - 1) / (radius * (1 + RING_WARP)));
    }

    static int verticalReach(int radius) {
        return (int) Math.ceil(radius * Y_SQUASH * (1 + SHELL_WARP)) + 1;
    }

    private static BoundingBox box(BlockPos c, int radius) {
        int h = craterReach(radius), v = verticalReach(radius);
        return new BoundingBox(c.getX() - h, c.getY() - v, c.getZ() - h, c.getX() + h, c.getY() + v, c.getZ() + h);
    }

    private void initNoise() {
        if (noiseB == null) {
            synchronized (this) {
                if (noiseB == null) {
                    RandomSource r = RandomSource.create(seed);
                    shellNoise = new ImprovedNoise(r);
                    noiseA = new ImprovedNoise(r);
                    noiseB = new ImprovedNoise(r);
                }
            }
        }
    }

    // ---- helpers for styles ----

    public int classify(int x, int y, int z) {
        double dx = x - centre.getX();
        double dy = (y - centre.getY()) / Y_SQUASH;
        double dz = z - centre.getZ();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double outer = radius * (1 + SHELL_WARP * shellNoise.noise(x * 0.045, y * 0.045, z * 0.045));
        if (d > outer) return OUTSIDE;
        return d > outer - CRUST_THICKNESS ? CRUST : INTERIOR;
    }

    /** Base floor height; swamp pools fill to exactly this level. */
    public int baseFloor() {
        return centre.getY() - (int) (radius * Y_SQUASH * FLOOR_FRACTION);
    }

    public BlockPos centre() { return centre; }
    public int radius() { return radius; }
    public long seed() { return seed; }

    /** Smooth 2D/3D noise in about [-1, 1]. */
    public double noiseA(double x, double y, double z) { return noiseA.noise(x, y, z); }
    public double noiseB(double x, double y, double z) { return noiseB.noise(x, y, z); }

    /** Lowest crust block above (x, y, z), scanning at most {@code max} blocks; -1 if none. */
    public int ceilingAbove(int x, int y, int z, int max) {
        for (int k = 1; k <= max; k++) {
            int c = classify(x, y + k, z);
            if (c != INTERIOR) return c == CRUST ? y + k : -1;
        }
        return -1;
    }

    /** Deterministic hash of a position to [0, 1). */
    public double hash(int x, int y, int z, int salt) {
        long h = seed + x * 0x9E3779B97F4A7C15L + y * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L + salt * 0xD6E8FEB86659FD93L;
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        initNoise();
        BoundingBox bb = this.boundingBox;
        int minX = Math.max(bb.minX(), chunkBox.minX()), maxX = Math.min(bb.maxX(), chunkBox.maxX());
        int minZ = Math.max(bb.minZ(), chunkBox.minZ()), maxZ = Math.min(bb.maxZ(), chunkBox.maxZ());
        int minY = Math.max(bb.minY(), level.getMinBuildHeight()), maxY = Math.min(bb.maxY(), level.getMaxBuildHeight() - 1);
        if (minX > maxX || minZ > maxZ) return;

        // Crater first, so the shell pass overwrites anything the levelling touched.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                craterColumn(level, x, z, chunkBox);
            }
        }

        BlockState crust = ModBlocks.PLACEHOLDER_CRUST.get().defaultBlockState();
        MeteorInterior interior = style.interior();
        boolean anyInterior = false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                MeteorInterior.Column col = null;
                for (int y = minY; y <= maxY; y++) {
                    int c = classify(x, y, z);
                    if (c == OUTSIDE) continue;
                    pos.set(x, y, z);
                    if (c == CRUST) {
                        level.setBlock(pos, crust, 2);
                        continue;
                    }
                    anyInterior = true;
                    if (col == null) col = interior.column(this, x, z);
                    level.setBlock(pos, interior.block(this, col, x, y, z), 2);
                }
            }
        }

        if (anyInterior) {
            writeInteriorBiome(level, chunkPos);
        }
    }

    private static double smooth(double e0, double e1, double v) {
        double t = Math.clamp((v - e0) / (e1 - e0), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }

    /**
     * Impact crater around the meteor: ground near the shell is levelled to a bowl floor a little below the
     * centre (cutting uphill terrain away, filling downhill terrain in), then a raised rim, then natural
     * ground. Works on each column's own terrain, so it stays deterministic per chunk. Columns under water
     * are left alone.
     */
    private void craterColumn(WorldGenLevel level, int x, int z, BoundingBox chunkBox) {
        double dx = x - centre.getX(), dz = z - centre.getZ();
        double warp = 1 + RING_WARP * noiseB.noise(x * 0.025, 91.7, z * 0.025);
        double t = Math.sqrt(dx * dx + dz * dz) / (radius * warp);
        double end = craterEnd(radius);
        if (t >= end) return;
        // Squeeze the part of the profile outside the shell into [1, end] (no-op when end == CRATER_END).
        if (t > 1) t = 1 + (t - 1) * (CRATER_END - 1) / (end - 1);

        int floorTop = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        int surfaceTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        if (surfaceTop > floorTop) return; // water or other non-solid cover
        int minY = chunkBox.minY(), maxY = chunkBox.maxY();
        if (floorTop < minY || floorTop > maxY) return;

        double bowlFloor = centre.getY() - radius * Y_SQUASH * BOWL_DEPTH;
        double level01 = 1 - smooth(BOWL_FLAT, BOWL_EDGE, t);
        double rim = RIM_HEIGHT * radius * Math.max(0, Math.cos(Math.PI / 2 * (t - RIM_PEAK) / RIM_HALF_WIDTH));
        if (Math.abs(t - RIM_PEAK) >= RIM_HALF_WIDTH) rim = 0;
        double bump = 1.5 * noiseA.noise(x * 0.09, 13.3, z * 0.09) * Math.max(level01, rim > 0 ? 1 : 0);
        int target = (int) Math.round(floorTop + (bowlFloor - floorTop) * level01 + rim + bump);
        target = Math.clamp(target, minY, maxY);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, floorTop, z);
        BlockState top = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.setY(floorTop - 1));
        BlockState filler = below.isSolid() ? below : Blocks.DIRT.defaultBlockState();

        if (target < floorTop) {
            for (int y = target + 1; y <= floorTop; y++) {
                if (classify(x, y, z) == OUTSIDE) level.setBlock(pos.setY(y), Blocks.AIR.defaultBlockState(), 2);
            }
        } else if (target > floorTop) {
            for (int y = floorTop; y < target; y++) {
                if (classify(x, y, z) == OUTSIDE) level.setBlock(pos.setY(y), filler, 2);
            }
        }
        if (classify(x, target, z) != OUTSIDE) return;

        // Ejecta: scorched ground, thickest in the bowl, thinning out past the rim.
        double ejecta = 0.75 * (1 - smooth(1.0, CRATER_END, t));
        double roll = hash(x, 0, z, 7);
        BlockState surface = top;
        if (roll < ejecta) {
            double pick = roll / ejecta;
            surface = (pick < 0.40 ? Blocks.COARSE_DIRT : pick < 0.70 ? Blocks.GRAVEL
                    : pick < 0.92 ? Blocks.TUFF : Blocks.COBBLED_DEEPSLATE).defaultBlockState();
        }
        level.setBlock(pos.setY(target), surface, 2);
    }

    /** Rewrites the biome cells of this chunk whose centre lies inside the meteor interior. */
    private void writeInteriorBiome(WorldGenLevel level, ChunkPos chunkPos) {
        var biomeHolder = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(interiorBiome);
        if (biomeHolder.isEmpty()) return;
        Holder<Biome> biome = biomeHolder.get();
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
        ServerLevel server = level.getLevel();
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
            int bx = QuartPos.toBlock(qx) + 2, by = QuartPos.toBlock(qy) + 2, bz = QuartPos.toBlock(qz) + 2;
            return classify(bx, by, bz) == INTERIOR ? biome : chunk.getNoiseBiome(qx, qy, qz);
        }, server.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        // Only matters for /place on loaded chunks; during normal generation the chunk isn't sent yet.
        if (level instanceof ServerLevel) {
            server.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        }
    }
}
