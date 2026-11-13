package com.bromax.exofall.entity.mob.swamp;

import com.bromax.exofall.entity.mob.AmbushMantis;
import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.Mudgrub;
import com.bromax.exofall.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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

/** Swamp meteor mantis: hides in Sapcane; the reeds rustle and shed bits before it lunges. Eats Mudgrubs. */
public class Reedmantis extends AmbushMantis {
    private static final BugSounds SOUNDS = BugSounds.of(null, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH,
            SoundEvents.SPIDER_STEP, 0.8f);

    public Reedmantis(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attrs(60, 18, 4, 0.3);
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected boolean isCover(BlockState state) {
        return state.is(ModBlocks.SAPCANE.get());
    }

    @Override
    protected void tell(ServerLevel level, BlockPos at) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.SAPCANE.get().defaultBlockState()),
                getX(), getY() + 1.2, getZ(), 18, 0.5, 0.7, 0.5, 0.05);
        level.playSound(null, at, SoundEvents.BIG_DRIPLEAF_TILT_DOWN, SoundSource.HOSTILE, 1.2f, 0.7f);
        level.playSound(null, at, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.0f, 0.8f);
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return e instanceof Mudgrub;
    }
}
