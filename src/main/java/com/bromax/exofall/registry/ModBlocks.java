package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.block.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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

    /** 5x5 crafting table for meteor gear; also crafts everything a normal crafting table does. */
    public static final DeferredBlock<ShellrockTableBlock> SHELLROCK_CRAFTING_TABLE = BLOCKS.register("shellrock_crafting_table",
            () -> new ShellrockTableBlock(Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE).strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_TILES).mapColor(MapColor.TERRACOTTA_WHITE)));

    // ---- mob-made blocks (no items, no drops, they vanish on their own) ----

    /** Bogspinner web: a cobweb that disappears after 30 s. */
    public static final DeferredBlock<com.bromax.exofall.block.BogsilkWebBlock> BOGSILK_WEB = BLOCKS.register("bogsilk_web",
            () -> new com.bromax.exofall.block.BogsilkWebBlock(Properties.ofFullCopy(Blocks.COBWEB).noLootTable()));
    /** Propolis Spitter puddle: slows like honey, gone after 10 s. */
    public static final DeferredBlock<com.bromax.exofall.block.PropolisBlock> PROPOLIS = BLOCKS.register("propolis",
            () -> new com.bromax.exofall.block.PropolisBlock(Properties.of().mapColor(MapColor.COLOR_ORANGE).noCollission().instabreak()
                    .speedFactor(0.35F).jumpFactor(0.5F).sound(SoundType.HONEY_BLOCK).noLootTable().replaceable().noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    // ---- Warrens (dungeons) ----

    /** One bug nest per meteor: mire nest, brood comb, sand burrow, rotwood hollow. */
    public static final java.util.Map<com.bromax.exofall.world.MeteorStyle, DeferredBlock<NestBlock>> NESTS = new java.util.EnumMap<>(com.bromax.exofall.world.MeteorStyle.class);

    static {
        NESTS.put(com.bromax.exofall.world.MeteorStyle.SWAMP, BLOCKS.register("mire_nest", () -> new NestBlock(com.bromax.exofall.world.MeteorStyle.SWAMP,
                Properties.ofFullCopy(Blocks.PACKED_MUD).strength(4.0F, 6.0F).sound(SoundType.MUD).mapColor(MapColor.TERRACOTTA_BROWN))));
        NESTS.put(com.bromax.exofall.world.MeteorStyle.HIVE, BLOCKS.register("brood_comb", () -> new NestBlock(com.bromax.exofall.world.MeteorStyle.HIVE,
                Properties.ofFullCopy(Blocks.HONEYCOMB_BLOCK).strength(4.0F, 6.0F).sound(SoundType.CORAL_BLOCK).mapColor(MapColor.COLOR_ORANGE))));
        NESTS.put(com.bromax.exofall.world.MeteorStyle.DESERT, BLOCKS.register("sand_burrow", () -> new NestBlock(com.bromax.exofall.world.MeteorStyle.DESERT,
                Properties.ofFullCopy(Blocks.SANDSTONE).strength(4.0F, 6.0F).sound(SoundType.SAND).mapColor(MapColor.SAND))));
        NESTS.put(com.bromax.exofall.world.MeteorStyle.RAINFOREST, BLOCKS.register("rotwood_hollow", () -> new NestBlock(com.bromax.exofall.world.MeteorStyle.RAINFOREST,
                Properties.ofFullCopy(Blocks.STRIPPED_MANGROVE_WOOD).strength(4.0F, 6.0F).sound(SoundType.NETHER_WOOD).mapColor(MapColor.TERRACOTTA_GREEN))));
    }

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


    // ---- warded arena blocks ----
    // Unbreakable twins of every block the boss arenas are built from (dome, floor, porch, pillars, trees), so a
    // fight can't be cheesed by digging in, tunnelling out or walling the boss off. They look and behave like the
    // block they copy (honey still sticks, mud still sinks) but can't be mined, blown up, pushed or griefed by
    // withers, and mobs never spawn on them. ArenaGen swaps blocks for their warded twin through WARDED.

    /** A warded block and the block it copies. */
    public record Ward(Supplier<? extends Block> source, DeferredBlock<? extends Block> block) {}

    public static final List<Ward> WARDED = new ArrayList<>();

    private static Properties wardedProps(Block src) {
        return Properties.ofFullCopy(src).strength(-1.0F, 3_600_000.0F).noLootTable()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK).isValidSpawn(Blocks::never);
    }

    private static void ward(String name, Supplier<? extends Block> src, Function<Properties, ? extends Block> ctor) {
        WARDED.add(new Ward(src, BLOCKS.register("warded_" + name, () -> ctor.apply(wardedProps(src.get())))));
    }

    private static void ward(String name, Supplier<? extends Block> src) {
        ward(name, src, Block::new);
    }

    static {
        ward("chitinstone_bricks", CHITINSTONE_BRICKS);
        ward("polished_chitinstone", POLISHED_CHITINSTONE);
        ward("chitinstone_pillar", CHITINSTONE_PILLAR, RotatedPillarBlock::new);
        ward("mud", () -> Blocks.MUD, MudBlock::new);
        ward("packed_mud", () -> Blocks.PACKED_MUD);
        ward("mud_bricks", () -> Blocks.MUD_BRICKS);
        ward("honeycomb_block", () -> Blocks.HONEYCOMB_BLOCK);
        ward("honey_block", () -> Blocks.HONEY_BLOCK, HoneyBlock::new);
        ward("sand", () -> Blocks.SAND);
        ward("red_sand", () -> Blocks.RED_SAND);
        ward("bone_block", () -> Blocks.BONE_BLOCK, RotatedPillarBlock::new);
        ward("moss_block", () -> Blocks.MOSS_BLOCK);
        ward("rooted_dirt", () -> Blocks.ROOTED_DIRT);
        ward("mossy_stone_bricks", () -> Blocks.MOSSY_STONE_BRICKS);
        ward("veilpine_log", VEILPINE.log, RotatedPillarBlock::new);
        ward("veilpine_leaves", VEILPINE.leaves, LeavesBlock::new);
        ward("verdant_froglight", () -> Blocks.VERDANT_FROGLIGHT, RotatedPillarBlock::new);
        ward("ochre_froglight", () -> Blocks.OCHRE_FROGLIGHT, RotatedPillarBlock::new);
        ward("pearlescent_froglight", () -> Blocks.PEARLESCENT_FROGLIGHT, RotatedPillarBlock::new);
        ward("shroomlight", () -> Blocks.SHROOMLIGHT);
    }

    private ModBlocks() {}
}
