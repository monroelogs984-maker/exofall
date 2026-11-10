package com.bromax.exofall.block;

import com.bromax.exofall.registry.ModTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Two-tall plant (Sapcane, Umbrella Fern) that only grows on meteor soil. */
public class AlienDoublePlantBlock extends DoublePlantBlock {
    public static final MapCodec<AlienDoublePlantBlock> CODEC = simpleCodec(AlienDoublePlantBlock::new);

    public AlienDoublePlantBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.ALIEN_SOIL);
    }
}
