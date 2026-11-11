package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.block.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Meteor stones, woods and plants. Textures are vanilla placeholders (see tools/gen_placeholder_assets.py).
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Exofall.MOD_ID);

    // ---- stones ----

    /** Meteor crust. Blast-proof, harder than obsidian; only drops to the meteoric tier (see ModTiers). */
    public static final DeferredBlock<Block> SHELLROCK = BLOCKS.register("shellrock",
            () -> new Block(Properties.ofFullCopy(Blocks.BLACKSTONE)
                    .strength(60.0F, 3_600_000.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    public static final DeferredBlock<Block> CHITINSTONE = BLOCKS.register("chitinstone",
            () -> new Block(Properties.ofFullCopy(Blocks.TUFF).strength(2.0F, 8.0F)));
    public static final DeferredBlock<Block> POLISHED_CHITINSTONE = BLOCKS.register("polished_chitinstone",
            () -> new Block(Properties.ofFullCopy(Blocks.POLISHED_TUFF).strength(2.0F, 8.0F)));
    public static final DeferredBlock<Block> CHITINSTONE_BRICKS = BLOCKS.register("chitinstone_bricks",
            () -> new Block(Properties.ofFullCopy(Blocks.TUFF_BRICKS).strength(2.0F, 8.0F)));
    public static final DeferredBlock<RotatedPillarBlock> CHITINSTONE_PILLAR = BLOCKS.register("chitinstone_pillar",
            () -> new RotatedPillarBlock(Properties.ofFullCopy(Blocks.TUFF_BRICKS).strength(2.0F, 8.0F)));
    public static final DeferredBlock<StairBlock> CHITINSTONE_BRICK_STAIRS = BLOCKS.register("chitinstone_brick_stairs",
            () -> new StairBlock(CHITINSTONE_BRICKS.get().defaultBlockState(), Properties.ofFullCopy(CHITINSTONE_BRICKS.get())));
    public static final DeferredBlock<SlabBlock> CHITINSTONE_BRICK_SLAB = BLOCKS.register("chitinstone_brick_slab",
            () -> new SlabBlock(Properties.ofFullCopy(CHITINSTONE_BRICKS.get())));
    public static final DeferredBlock<WallBlock> CHITINSTONE_BRICK_WALL = BLOCKS.register("chitinstone_brick_wall",
            () -> new WallBlock(Properties.ofFullCopy(CHITINSTONE_BRICKS.get()).forceSolidOn()));
    public static final DeferredBlock<StairBlock> POLISHED_CHITINSTONE_STAIRS = BLOCKS.register("polished_chitinstone_stairs",
            () -> new StairBlock(POLISHED_CHITINSTONE.get().defaultBlockState(), Properties.ofFullCopy(POLISHED_CHITINSTONE.get())));
    public static final DeferredBlock<SlabBlock> POLISHED_CHITINSTONE_SLAB = BLOCKS.register("polished_chitinstone_slab",
            () -> new SlabBlock(Properties.ofFullCopy(POLISHED_CHITINSTONE.get())));

    /** Soft glowing soil-stone; the only ground alien plants grow on. */
    public static final DeferredBlock<Block> GLOWMARL = BLOCKS.register("glowmarl",
            () -> new Block(Properties.ofFullCopy(Blocks.CALCITE).strength(0.8F).lightLevel(s -> 6).sound(SoundType.MUD_BRICKS)));

    // ---- boss arenas ----

    /** Fills the arena door until the sigil opens it; closes again behind the players while the boss fight lasts. */
    public static final DeferredBlock<Block> SIGIL_SEAL = BLOCKS.register("sigil_seal",
            () -> new Block(Properties.ofFullCopy(Blocks.BEDROCK).strength(-1.0F, 3_600_000.0F).noLootTable()
                    .lightLevel(s -> 9).sound(SoundType.AMETHYST).mapColor(MapColor.COLOR_PURPLE)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static final DeferredBlock<SigilSocketBlock> SIGIL_SOCKET = BLOCKS.register("sigil_socket",
            () -> new SigilSocketBlock(Properties.ofFullCopy(Blocks.BEDROCK).strength(-1.0F, 3_600_000.0F).noLootTable()
                    .lightLevel(s -> 5).sound(SoundType.DEEPSLATE_BRICKS)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    // ---- woods ----

    public static final WoodSet STILTREED = new WoodSet(BLOCKS, "stiltreed", Blocks.MANGROVE_PLANKS, true);
    public static final WoodSet COMBWOOD = new WoodSet(BLOCKS, "combwood", Blocks.ACACIA_PLANKS, false);
    public static final WoodSet RIBWOOD = new WoodSet(BLOCKS, "ribwood", Blocks.BIRCH_PLANKS, false);
    public static final WoodSet VEILPINE = new WoodSet(BLOCKS, "veilpine", Blocks.WARPED_PLANKS, true);
    public static final List<WoodSet> WOODS = List.of(STILTREED, COMBWOOD, RIBWOOD, VEILPINE);

    // ---- plants ----

    private static Properties plant(MapColor color) {
        return Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(color).offsetType(BlockBehaviour.OffsetType.NONE);
    }

    // Swamp
    public static final DeferredBlock<HangingPlantBlock> LANTERNPOD = BLOCKS.register("lanternpod",
            () -> new HangingPlantBlock(plant(MapColor.COLOR_YELLOW).lightLevel(s -> 12).sound(SoundType.SHROOMLIGHT), false));
    public static final DeferredBlock<SurfacePadBlock> BLADDERPAD = BLOCKS.register("bladderpad",
            () -> new SurfacePadBlock(Properties.ofFullCopy(Blocks.LILY_PAD), true));
    public static final DeferredBlock<ThornPlantBlock> SNARETONGUE = BLOCKS.register("snaretongue",
            () -> new ThornPlantBlock(plant(MapColor.COLOR_RED)));
    public static final DeferredBlock<AlienDoublePlantBlock> SAPCANE = BLOCKS.register("sapcane",
            () -> new AlienDoublePlantBlock(plant(MapColor.COLOR_LIGHT_GREEN)));
    public static final DeferredBlock<SurfacePadBlock> DRIFTMOSS = BLOCKS.register("driftmoss",
            () -> new SurfacePadBlock(Properties.ofFullCopy(Blocks.LILY_PAD).sound(SoundType.MOSS_CARPET), false));
    public static final DeferredBlock<HangingPlantBlock> WEEPVINE = BLOCKS.register("weepvine",
            () -> new HangingPlantBlock(plant(MapColor.COLOR_GREEN).sound(SoundType.WEEPING_VINES), true));

    // Hive
    public static final DeferredBlock<AlienPlantBlock> NECTAR_BELL = BLOCKS.register("nectar_bell",
            () -> new AlienPlantBlock(plant(MapColor.COLOR_YELLOW).lightLevel(s -> 4)));
    public static final DeferredBlock<AlienPlantBlock> WAXCAP = BLOCKS.register("waxcap",
            () -> new AlienPlantBlock(plant(MapColor.COLOR_ORANGE).sound(SoundType.FUNGUS)));
    public static final DeferredBlock<AmethystClusterBlock> ROYAL_AMBER = BLOCKS.register("royal_amber",
            () -> new AmethystClusterBlock(7.0F, 3.0F, Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.COLOR_ORANGE).lightLevel(s -> 9)));

    // Desert
    public static final DeferredBlock<ThornPlantBlock> DUNESPINE = BLOCKS.register("dunespine",
            () -> new ThornPlantBlock(plant(MapColor.SAND)));
    public static final DeferredBlock<AlienPlantBlock> NIGHTBLOOM = BLOCKS.register("nightbloom",
            () -> new AlienPlantBlock(plant(MapColor.COLOR_BLUE).lightLevel(s -> 5)));
    public static final DeferredBlock<RattlegrassBlock> RATTLEGRASS = BLOCKS.register("rattlegrass",
            () -> new RattlegrassBlock(plant(MapColor.SAND).sound(SoundType.BAMBOO_SAPLING)));

    // Rainforest
    public static final DeferredBlock<AlienDoublePlantBlock> UMBRELLA_FERN = BLOCKS.register("umbrella_fern",
            () -> new AlienDoublePlantBlock(plant(MapColor.COLOR_GREEN)));
    public static final DeferredBlock<AlienPlantBlock> MISTCAP = BLOCKS.register("mistcap",
            () -> new AlienPlantBlock(plant(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 3).sound(SoundType.FUNGUS)));
    public static final DeferredBlock<HangingPlantBlock> GLOWMOSS_CURTAIN = BLOCKS.register("glowmoss_curtain",
            () -> new HangingPlantBlock(plant(MapColor.COLOR_CYAN).lightLevel(s -> 7).sound(SoundType.MOSS_CARPET), true));

    // Saplings (only grow on meteor soil)
    public static final DeferredBlock<AlienSaplingBlock> STILTREED_SAPLING = sapling("stiltreed_sapling", AlienSaplingBlock.Species.STILTREED);
    public static final DeferredBlock<AlienSaplingBlock> COMBWOOD_SAPLING = sapling("combwood_sapling", AlienSaplingBlock.Species.COMBWOOD);
    public static final DeferredBlock<AlienSaplingBlock> RIBWOOD_SAPLING = sapling("ribwood_sapling", AlienSaplingBlock.Species.RIBWOOD);
    public static final DeferredBlock<AlienSaplingBlock> VEILPINE_SAPLING = sapling("veilpine_sapling", AlienSaplingBlock.Species.VEILPINE);

    private static DeferredBlock<AlienSaplingBlock> sapling(String id, AlienSaplingBlock.Species species) {
        return BLOCKS.register(id, () -> new AlienSaplingBlock(species, Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    }

    private ModBlocks() {}
}
