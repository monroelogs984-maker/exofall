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
 * of its footprint, so roughly half of it is buried. Water-covered spots are rejected.
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

        // Reject water: surface (incl. water) clearly above the solid floor.
        int surface = height(ctx, x, z, Heightmap.Types.WORLD_SURFACE_WG);
        int floor = height(ctx, x, z, Heightmap.Types.OCEAN_FLOOR_WG);
        if (surface - floor > 2) return Optional.empty();

        // Median ground height over the footprint (centre + 8 points around the rim).
        int[] h = new int[9];
        h[0] = floor;
        int r = (int) (radius * 0.8);
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 4 * k;
            int px = x + (int) Math.round(Math.cos(a) * r);
            int pz = z + (int) Math.round(Math.sin(a) * r);
            h[k + 1] = height(ctx, px, pz, Heightmap.Types.OCEAN_FLOOR_WG);
        }
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

    private static int height(GenerationContext ctx, int x, int z, Heightmap.Types type) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, type, ctx.heightAccessor(), ctx.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.METEOR.get();
    }
}
