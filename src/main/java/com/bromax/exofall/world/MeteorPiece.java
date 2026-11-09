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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

import java.util.List;

/**
 * One meteor: a noise-warped ellipsoid shell of crust around a hollow interior with a mud floor,
 * shallow pools, plants and ceiling lights. Placement is a pure function of (position, seed), so
 * every chunk carves its own slice without caring which neighbours already generated.
 *
 * <p>Interior biome: biomes are already fixed when structures place, and the decoration pass only
 * runs features for biomes the dimension's biome source knows about. So the piece decorates the
 * interior itself and then rewrites the 4x4x4 biome cells inside the interior (the same thing
 * /fillbiome does). That gives the meteor its own fog, sky, music, particles and mob spawns, and it
 * keeps overworld features (which are biome-filtered) out of the interior.
 */
public class MeteorPiece extends StructurePiece {
    private static final double Y_SQUASH = 0.8;      // vertical radius = 80% of horizontal
    private static final double SHELL_WARP = 0.13;   // radius varies by up to ±13%
    private static final double CRUST_THICKNESS = 5.0;
    private static final double FLOOR_FRACTION = 0.30; // floor sits 30% of the vertical radius below centre

    private static final int OUTSIDE = 0, CRUST = 1, INTERIOR = 2;

    private final BlockPos centre;
    private final int radius;
    private final long seed;
    private final ResourceKey<Biome> interiorBiome;

    private ImprovedNoise shellNoise;
    private ImprovedNoise floorNoise;

    public MeteorPiece(BlockPos centre, int radius, long seed, ResourceKey<Biome> interiorBiome) {
        super(ModWorldgen.METEOR_PIECE.get(), 0, box(centre, radius));
        this.centre = centre;
        this.radius = radius;
        this.seed = seed;
        this.interiorBiome = interiorBiome;
    }

    public MeteorPiece(CompoundTag tag) {
        super(ModWorldgen.METEOR_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.radius = tag.getInt("R");
        this.seed = tag.getLong("Seed");
        this.interiorBiome = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(tag.getString("Biome")));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("CX", centre.getX());
        tag.putInt("CY", centre.getY());
        tag.putInt("CZ", centre.getZ());
        tag.putInt("R", radius);
        tag.putLong("Seed", seed);
        tag.putString("Biome", interiorBiome.location().toString());
    }

    static int horizontalReach(int radius) {
        return (int) Math.ceil(radius * (1 + SHELL_WARP)) + 1;
    }

    static int verticalReach(int radius) {
        return (int) Math.ceil(radius * Y_SQUASH * (1 + SHELL_WARP)) + 1;
    }

    private static BoundingBox box(BlockPos c, int radius) {
        int h = horizontalReach(radius), v = verticalReach(radius);
        return new BoundingBox(c.getX() - h, c.getY() - v, c.getZ() - h, c.getX() + h, c.getY() + v, c.getZ() + h);
    }

    private void initNoise() {
        if (shellNoise == null) {
            RandomSource r = RandomSource.create(seed);
            shellNoise = new ImprovedNoise(r);
            floorNoise = new ImprovedNoise(r);
        }
    }

    private int classify(int x, int y, int z) {
        double dx = x - centre.getX();
        double dy = (y - centre.getY()) / Y_SQUASH;
        double dz = z - centre.getZ();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double outer = radius * (1 + SHELL_WARP * shellNoise.noise(x * 0.045, y * 0.045, z * 0.045));
        if (d > outer) return OUTSIDE;
        return d > outer - CRUST_THICKNESS ? CRUST : INTERIOR;
    }

    private int waterLevel() {
        return centre.getY() - (int) (radius * Y_SQUASH * FLOOR_FRACTION);
    }

    private int floorTop(int x, int z) {
        return waterLevel() + (int) Math.floor(2.6 * floorNoise.noise(x * 0.07, 7.5, z * 0.07) + 0.4);
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

        BlockState crust = ModBlocks.PLACEHOLDER_CRUST.get().defaultBlockState();
        BlockState plant = ModBlocks.PLACEHOLDER_BOG_PLANT.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState mud = Blocks.MUD.defaultBlockState();
        BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
        BlockState light = Blocks.VERDANT_FROGLIGHT.defaultBlockState();
        BlockState lily = Blocks.LILY_PAD.defaultBlockState();
        int waterLevel = waterLevel();
        boolean anyInterior = false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int ft = floorTop(x, z);
                for (int y = minY; y <= maxY; y++) {
                    int c = classify(x, y, z);
                    pos.set(x, y, z);
                    if (c == OUTSIDE) {
                        continue;
                    }
                    if (c == CRUST) {
                        level.setBlock(pos, crust, 2);
                        continue;
                    }
                    anyInterior = true;
                    if (y <= ft) {
                        boolean top = y == ft && y > waterLevel;
                        level.setBlock(pos, top && hash(x, y, z, 1) < 0.4 ? moss : mud, 2);
                    } else if (y <= waterLevel) {
                        level.setBlock(pos, water, 2);
                    } else {
                        BlockState s = air;
                        if (y == ft + 1 && ft > waterLevel && hash(x, y, z, 2) < 0.16) {
                            s = plant;
                        } else if (y == waterLevel + 1 && ft < waterLevel && hash(x, y, z, 3) < 0.07) {
                            s = lily;
                        } else if (classify(x, y + 1, z) == CRUST && hash(x, y, z, 4) < 0.025) {
                            s = light;
                        }
                        level.setBlock(pos, s, 2);
                    }
                }
            }
        }

        if (anyInterior) {
            writeInteriorBiome(level, chunkPos);
        }
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

    private double hash(int x, int y, int z, int salt) {
        long h = seed + x * 0x9E3779B97F4A7C15L + y * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L + salt * 0xD6E8FEB86659FD93L;
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
