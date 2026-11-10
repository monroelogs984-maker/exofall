package com.bromax.exofall.world;

import com.bromax.exofall.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * A huge meteor sunk about halfway into the terrain. The sphere's centre sits at the median ground height
 * of its footprint, so roughly half of it is buried. Spots with water in the footprint or crater ring are rejected.
 */
public class MeteorStructure extends Structure {
    public static final MapCodec<MeteorStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.intRange(12, 56).fieldOf("min_radius").forGetter(s -> s.minRadius),
            Codec.intRange(12, 56).fieldOf("max_radius").forGetter(s -> s.maxRadius),
            ResourceKey.codec(Registries.BIOME).fieldOf("interior_biome").forGetter(s -> s.interiorBiome),
            MeteorStyle.CODEC.fieldOf("style").forGetter(s -> s.style)
    ).apply(i, MeteorStructure::new));

    private final int minRadius;
    private final int maxRadius;
    private final ResourceKey<Biome> interiorBiome;
    private final MeteorStyle style;

    public MeteorStructure(StructureSettings settings, int minRadius, int maxRadius, ResourceKey<Biome> interiorBiome, MeteorStyle style) {
        super(settings);
        this.minRadius = minRadius;
        this.maxRadius = Math.max(minRadius, maxRadius);
        this.interiorBiome = interiorBiome;
        this.style = style;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x = ctx.chunkPos().getMiddleBlockX();
        int z = ctx.chunkPos().getMiddleBlockZ();
        int radius = minRadius + ctx.random().nextInt(maxRadius - minRadius + 1);

        // Reject water anywhere near: footprint (8 points at 0.8 r) plus crater ring (8 points at 1.5 r).
        // At most one wet sample is allowed (a stream or puddle); lakes, rivers and coasts fail.
        int[] h = new int[9];
        h[0] = height(ctx, x, z, Heightmap.Types.OCEAN_FLOOR_WG);
        int wet = isWet(ctx, x, z, h[0]) ? 1 : 0;
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 4 * k;
            int px = x + (int) Math.round(Math.cos(a) * radius * 0.8);
            int pz = z + (int) Math.round(Math.sin(a) * radius * 0.8);
            h[k + 1] = height(ctx, px, pz, Heightmap.Types.OCEAN_FLOOR_WG);
            if (isWet(ctx, px, pz, h[k + 1])) wet++;
            double b = a + Math.PI / 8;
            int qx = x + (int) Math.round(Math.cos(b) * radius * 1.5);
            int qz = z + (int) Math.round(Math.sin(b) * radius * 1.5);
            if (isWet(ctx, qx, qz, height(ctx, qx, qz, Heightmap.Types.OCEAN_FLOOR_WG))) wet++;
            if (wet > 1) return Optional.empty();
        }
        // Median ground height over the footprint.
        Arrays.sort(h);
        if (h[8] - h[0] > radius * 1.2) return Optional.empty(); // too steep
        int y = h[4];

        int bottom = y - MeteorPiece.verticalReach(radius);
        if (bottom < ctx.heightAccessor().getMinBuildHeight() + 8) return Optional.empty();

        BlockPos centre = new BlockPos(x, y, z);
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(centre, builder ->
                builder.addPiece(new MeteorPiece(centre, radius, seed, interiorBiome, style))));
    }

    private static boolean isWet(GenerationContext ctx, int x, int z, int floor) {
        return height(ctx, x, z, Heightmap.Types.WORLD_SURFACE_WG) - floor > 1;
    }

    private static int height(GenerationContext ctx, int x, int z, Heightmap.Types type) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, type, ctx.heightAccessor(), ctx.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.METEOR.get();
    }
}
