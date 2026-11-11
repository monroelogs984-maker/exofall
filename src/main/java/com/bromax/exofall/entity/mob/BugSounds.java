package com.bromax.exofall.entity.mob;

import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

/** Vanilla stand-in sounds for a bug. */
public record BugSounds(@Nullable SoundEvent ambient, @Nullable SoundEvent hurt, @Nullable SoundEvent death,
                        @Nullable SoundEvent step, float pitch, float volume) {
    public static BugSounds of(SoundEvent ambient, SoundEvent hurt, SoundEvent death, SoundEvent step, float pitch) {
        return new BugSounds(ambient, hurt, death, step, pitch, 1.0f);
    }
}
