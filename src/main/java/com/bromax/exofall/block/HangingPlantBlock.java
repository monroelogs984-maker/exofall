package com.bromax.exofall.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hangs from a ceiling. Chains (Weepvine, Glowmoss Curtain) can also hang from themselves and are
 * climbable through {@code #minecraft:climbable}; Lanternpods hang alone.
 */
public class HangingPlantBlock extends Block {
    public static final MapCodec<HangingPlantBlock> CODEC = simpleCodec(p -> new HangingPlantBlock(p, true));
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape POD_SHAPE = Block.box(4, 4, 4, 12, 16, 12);

    private final boolean chain;

    public HangingPlantBlock(Properties props, boolean chain) {
        super(props);
        this.chain = chain;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return chain ? SHAPE : POD_SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState up = level.getBlockState(above);
        return (chain && up.is(this)) || up.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.UP && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }
}
