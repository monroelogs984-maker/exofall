package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.world.MeteorPiece;
import com.bromax.exofall.world.MeteorStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Exofall.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Exofall.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<MeteorStructure>> METEOR =
            STRUCTURE_TYPES.register("meteor", () -> () -> MeteorStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> METEOR_PIECE =
            STRUCTURE_PIECES.register("meteor", () -> (StructurePieceType.ContextlessType) MeteorPiece::new);

    /** Interior biome of the swamp meteor. Never placed by the biome source, only written inside meteors. */
    public static final ResourceKey<Biome> SWAMP_METEOR_BIOME = ResourceKey.create(Registries.BIOME, Exofall.id("swamp_meteor"));

    private ModWorldgen() {}
}
