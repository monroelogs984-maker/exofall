package com.bromax.exofall.entity.mob.rainforest;

import com.bromax.exofall.block.AlienDoublePlantBlock;
import com.bromax.exofall.block.AlienPlantBlock;
import com.bromax.exofall.entity.mob.AmbushMantis;
import com.bromax.exofall.entity.mob.BugSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Flower-mimic mantis: sits among the rainforest plants; petals shiver loose (and chime) before it strikes. Eats Lantern Moths. */
public class Bloommantis extends AmbushMantis {
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH,
            SoundEvents.SPIDER_STEP, 1.0f);

    public Bloommantis(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(28, 8, 3, 0.3);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected boolean isCover(BlockState state) {
        return state.getBlock() instanceof AlienPlantBlock || state.getBlock() instanceof AlienDoublePlantBlock;
    }

    @Override
    protected void tell(ServerLevel level, BlockPos at) {
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.0, getZ(), 14, 0.6, 0.5, 0.6, 0.02);
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2f, 1.4f);
        level.playSound(null, at, SoundEvents.FLOWERING_AZALEA_BREAK, SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return e instanceof LanternMoth;
    }
}
