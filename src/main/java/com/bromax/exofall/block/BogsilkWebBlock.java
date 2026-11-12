package com.bromax.exofall.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A Bogspinner's web: works like a cobweb, then sags away after 30 s (so spiders don't fill the world with webs). */
public class BogsilkWebBlock extends WebBlock {
    public static final MapCodec<WebBlock> CODEC = simpleCodec(BogsilkWebBlock::new);
    public static final int LIFETIME = 600;

    public BogsilkWebBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<WebBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        level.scheduleTick(pos, this, LIFETIME);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }
}
