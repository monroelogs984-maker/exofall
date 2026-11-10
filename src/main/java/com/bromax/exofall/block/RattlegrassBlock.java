package com.bromax.exofall.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Dry grass that rattles when something moves through it. Sneaking is silent. Emits a vibration
 * game event, which the desert beetles will listen for once they exist.
 */
public class RattlegrassBlock extends AlienPlantBlock {
    public static final MapCodec<RattlegrassBlock> CODEC = simpleCodec(RattlegrassBlock::new);

    public RattlegrassBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || entity.isSteppingCarefully()) return;
        if (entity.xOld == entity.getX() && entity.zOld == entity.getZ()) return;
        if (level.getGameTime() % 10 != (pos.asLong() & 7)) return;
        level.playSound(null, pos, SoundEvents.BAMBOO_SAPLING_HIT, SoundSource.BLOCKS, 0.7F, 1.6F + level.random.nextFloat() * 0.3F);
        level.gameEvent(entity, GameEvent.ENTITY_ACTION, pos);
        if (entity instanceof Player) level.gameEvent(entity, GameEvent.STEP, pos);
    }
}
