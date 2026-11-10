package com.bromax.exofall.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Snaretongue and Dunespine: slows whatever walks into it and nips it while it keeps moving,
 * like a sweet berry bush. Easy to see and easy to walk around.
 */
public class ThornPlantBlock extends AlienPlantBlock {
    public static final MapCodec<ThornPlantBlock> CODEC = simpleCodec(ThornPlantBlock::new);

    public ThornPlantBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        entity.makeStuckInBlock(state, new Vec3(0.8, 0.75, 0.8));
        if (level.isClientSide) return;
        boolean moving = entity.xOld != entity.getX() || entity.zOld != entity.getZ();
        if (moving && Math.abs(entity.getX() - entity.xOld) + Math.abs(entity.getZ() - entity.zOld) > 0.003) {
            entity.hurt(level.damageSources().sweetBerryBush(), 1.0F);
        }
    }
}
