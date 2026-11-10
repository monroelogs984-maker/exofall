package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Meteor stones, woods and plants. "placeholder_*" blocks are prototype stand-ins with vanilla textures. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Exofall.MOD_ID);

    /** Meteor shell. Blast-proof, harder than obsidian; only drops to the meteoric tier (see ModTiers). */
    public static final DeferredBlock<Block> PLACEHOLDER_CRUST = BLOCKS.register("placeholder_crust",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE)
                    .strength(60.0F, 3_600_000.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    /** Test plant for the swamp meteor floor. */
    public static final DeferredBlock<FlowerBlock> PLACEHOLDER_BOG_PLANT = BLOCKS.register("placeholder_bog_plant",
            () -> new FlowerBlock(MobEffects.POISON, 5.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)));

    private ModBlocks() {}
}
