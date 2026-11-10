package com.bromax.exofall.block;

import com.bromax.exofall.registry.ModTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A small plant that only grows on meteor soil ({@code #exofall:alien_soil}). */
public class AlienPlantBlock extends BushBlock {
    public static final MapCodec<AlienPlantBlock> CODEC = simpleCodec(AlienPlantBlock::new);

    public AlienPlantBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.ALIEN_SOIL);
    }
}
