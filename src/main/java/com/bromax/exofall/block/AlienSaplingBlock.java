package com.bromax.exofall.block;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.WoodSet;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Sapling for an alien wood. Only grows on meteor soil. Grows like a vanilla sapling (two stages, random ticks
 * or bone meal) into the same tree shape the meteor generator builds; needs the space to be clear.
 */
public class AlienSaplingBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public enum Species { STILTREED, COMBWOOD, RIBWOOD, VEILPINE }

    public static final MapCodec<AlienSaplingBlock> CODEC = simpleCodec(p -> new AlienSaplingBlock(Species.STILTREED, p));
    private final Species species;

    public AlienSaplingBlock(Species species, Properties props) {
        super(props);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(com.bromax.exofall.registry.ModTags.ALIEN_SOIL);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0) advance(level, pos, state, random);
    }

    private void advance(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            grow(level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        advance(level, pos, state, random);
    }

    // ---------------------------------------------------------------- tree shapes

    /** Grows the tree now if there's room; returns whether it grew. */
    public boolean grow(ServerLevel level, BlockPos base, RandomSource random) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        switch (species) {
            case STILTREED -> stiltreed(blocks, base, random);
            case COMBWOOD -> combwood(blocks, base, random);
            case RIBWOOD -> ribwood(blocks, base, random);
            case VEILPINE -> veilpine(blocks, base, random);
        }
        for (BlockPos p : blocks.keySet()) {
            if (p.equals(base)) continue;
            BlockState there = level.getBlockState(p);
            if (!there.canBeReplaced() && !there.is(net.minecraft.tags.BlockTags.LEAVES)) return false; // no room
            if (level.isOutsideBuildHeight(p)) return false;
        }
        level.setBlock(base, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 4);
        blocks.forEach((p, s) -> level.setBlock(p, s, 3));
        return true;
    }

    private static BlockState log(WoodSet w) {
        return w.log.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Y);
    }

    private static BlockState leaves(WoodSet w) {
        return w.leaves.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1);
    }

    /** Trunk on 4 diagonal stilt roots, segmented, with a drooping frond cap. Mirrors SwampInterior. */
    private static void stiltreed(Map<BlockPos, BlockState> out, BlockPos b, RandomSource r) {
        WoodSet w = ModBlocks.STILTREED;
        int rootTop = 3, top = rootTop + 7 + r.nextInt(5);
        for (int k = 1; k <= 3; k++) {
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
                if (k < 3) out.put(b.offset(sx * k, rootTop - k, sz * k), w.wood.get().defaultBlockState());
                else for (int y = 0; y <= rootTop - 3; y++) out.put(b.offset(sx * 3, y, sz * 3), w.wood.get().defaultBlockState());
            }
        }
        for (int y = rootTop; y <= top; y++) {
            out.put(b.above(y), (y - rootTop) % 4 == 3 ? w.strippedLog.get().defaultBlockState() : log(w));
        }
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int r2 = dx * dx + dz * dz;
            if (r2 <= 6) out.putIfAbsent(b.offset(dx, top + 1, dz), leaves(w));
            if (r2 > 2 && r2 <= 13) out.putIfAbsent(b.offset(dx, top, dz), leaves(w));
            if (r2 > 8 && r2 <= 17 && r.nextFloat() < 0.65F) out.putIfAbsent(b.offset(dx, top - 1, dz), leaves(w));
            if (r2 > 10 && r2 <= 17 && r.nextFloat() < 0.35F) out.putIfAbsent(b.offset(dx, top - 2, dz), leaves(w));
        }
    }

    /** 3x3 trunk with a hollow core broken by plank nodes, root flare at the base. Mirrors HiveInterior. */
    private static void combwood(Map<BlockPos, BlockState> out, BlockPos b, RandomSource r) {
        WoodSet w = ModBlocks.COMBWOOD;
        int h = 9 + r.nextInt(6);
        for (int y = 0; y < h; y++) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                boolean core = dx == 0 && dz == 0;
                if (!core) out.put(b.offset(dx, y, dz), log(w));
                else if (y % 7 == 0 || y == h - 1) out.put(b.offset(dx, y, dz), w.planks.get().defaultBlockState());
                else out.put(b.offset(dx, y, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
        for (int y = 0; y < 2; y++) {
            out.put(b.offset(2, y, 0), w.wood.get().defaultBlockState());
            out.put(b.offset(-2, y, 0), w.wood.get().defaultBlockState());
            out.put(b.offset(0, y, 2), w.wood.get().defaultBlockState());
            out.put(b.offset(0, y, -2), w.wood.get().defaultBlockState());
        }
    }

    /** Two log strands twisting round each other with a crown of 4 ribs. Mirrors DesertInterior. */
    private static void ribwood(Map<BlockPos, BlockState> out, BlockPos b, RandomSource r) {
        WoodSet w = ModBlocks.RIBWOOD;
        int top = 8 + r.nextInt(7);
        double phase = r.nextDouble() * Math.PI * 2;
        for (int y = 0; y < 3; y++) out.put(b.above(y), log(w));
        for (int y = 0; y <= top; y++) {
            double a = y * 0.45 + phase;
            for (int s = 0; s < 2; s++) {
                double c = a + s * Math.PI;
                out.put(b.offset((int) Math.round(Math.cos(c) * 1.6), y, (int) Math.round(Math.sin(c) * 1.6)), log(w));
            }
        }
        for (int j = 0; j < 4; j++) {
            double c = phase + j * Math.PI / 2;
            for (int k = 2; k <= 5; k++) {
                int ry = top + (k <= 3 ? 1 : k == 4 ? 0 : -1);
                out.put(b.offset((int) Math.round(Math.cos(c) * k), ry, (int) Math.round(Math.sin(c) * k)), log(w));
            }
        }
    }

    /** Tall tiered cone. Mirrors RainforestInterior. */
    private static void veilpine(Map<BlockPos, BlockState> out, BlockPos b, RandomSource r) {
        WoodSet w = ModBlocks.VEILPINE;
        int top = 10 + r.nextInt(8);
        for (int y = 0; y <= top; y++) out.put(b.above(y), log(w));
        for (int y = 3; y <= top + 1; y++) {
            int depth = top + 1 - y;
            double rad = Math.min(3.6, 0.5 + depth * 0.33);
            if (depth % 3 == 2) rad *= 0.55;
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) { if (y == top + 1) out.put(b.above(y), leaves(w)); continue; }
                if (dx * dx + dz * dz <= rad * rad && r.nextFloat() < 0.92F) out.putIfAbsent(b.offset(dx, y, dz), leaves(w));
            }
        }
    }
}
