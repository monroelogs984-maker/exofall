package com.bromax.exofall.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Floats on water like a lily pad. Driftmoss is just a carpet; a Bladderpad pops when stepped on,
 * puffing a slowing gas around it, then is gone.
 */
public class SurfacePadBlock extends WaterlilyBlock {
    public static final MapCodec<SurfacePadBlock> CODEC = simpleCodec(p -> new SurfacePadBlock(p, false));

    private final boolean bursts;

    public SurfacePadBlock(Properties props, boolean bursts) {
        super(props);
        this.bursts = bursts;
    }

    @Override
    public MapCodec<WaterlilyBlock> codec() {
        return (MapCodec<WaterlilyBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!bursts || level.isClientSide || !(entity instanceof LivingEntity)) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.BLOCKS, 1.0F, 0.8F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2.5))) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 40, 1.2, 0.5, 1.2, 0.01);
        }
    }
}
