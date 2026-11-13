package com.bromax.exofall.effect;

import com.bromax.exofall.registry.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Stacking bleed. Each stack deals 1 damage a second; a hit adds a stack (max 3) and tops the timer back up to at
 * least 2 s while keeping its once-a-second rhythm, so a fast combo bleeds every second instead of resetting it.
 */
public class BleedingEffect extends MobEffect {
    public static final int MAX_STACKS = 3;
    public static final int DURATION = 41;

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8E1F24);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 1 && duration < DURATION;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        DamageSource src = new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModEffects.BLEED_DAMAGE));
        entity.hurt(src, amplifier + 1);
        return true;
    }

    /** Adds one bleed stack to {@code target}. */
    public static void addStack(LivingEntity target, LivingEntity source) {
        MobEffectInstance cur = target.getEffect(ModEffects.BLEEDING);
        int amp = 0;
        int dur = DURATION;
        if (cur != null) {
            amp = Math.min(com.bromax.exofall.config.ExofallConfig.LONGSWORD_BLEED_STACKS.get() - 1, cur.getAmplifier() + 1);
            int d = cur.getDuration();
            dur = d >= DURATION ? d : d + 20 * (int) Math.ceil((DURATION - d) / 20.0);
        }
        target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, dur, amp, false, true, true), source);
    }
}
