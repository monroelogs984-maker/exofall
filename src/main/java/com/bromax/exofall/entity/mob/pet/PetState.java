package com.bromax.exofall.entity.mob.pet;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Per-entity pet data. Baby-ness lives in each entity's synced data (it changes the size), the rest is server-only. */
public final class PetState {
    /** What a tamed pet does when it isn't fighting or working: cycled by right-clicking it with an empty hand. */
    public enum Mode { FOLLOW, STAY, WANDER;
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public boolean tamed;
    /** The player who tamed it (or bred it). Null for pets tamed before 1.1: the next player to right-click it claims it. */
    @Nullable public UUID owner;
    public Mode mode = Mode.FOLLOW;
    /** Negative while a baby: counts up to 0, then it's grown. */
    public int age;
    /** Ticks left in the mood to breed. */
    public int love;
    /** Ticks before it can breed again. */
    public int cooldown;
    /** Ticks until a helper's next find (Mudgrub dig, Golden Scarab ore sense / nugget). */
    public int jobTimer = -1, nuggetTimer = -1;
    /** Mantis pets: the last target they hit, so only the first strike on each new target gets the ambush bonus. Not saved. */
    public int lastStruck = -1;

    public void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Tamed", tamed);
        if (owner != null) t.putUUID("Owner", owner);
        t.putString("Mode", mode.name());
        t.putInt("Age", age);
        t.putInt("Cooldown", cooldown);
        t.putInt("Job", jobTimer);
        t.putInt("Nugget", nuggetTimer);
        tag.put("ExofallPet", t);
    }

    public void load(CompoundTag tag) {
        CompoundTag t = tag.getCompound("ExofallPet");
        tamed = t.getBoolean("Tamed");
        owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
        try {
            mode = t.contains("Mode") ? Mode.valueOf(t.getString("Mode")) : Mode.FOLLOW;
        } catch (IllegalArgumentException e) {
            mode = Mode.FOLLOW;
        }
        age = t.getInt("Age");
        cooldown = t.getInt("Cooldown");
        jobTimer = t.contains("Job") ? t.getInt("Job") : -1;
        nuggetTimer = t.contains("Nugget") ? t.getInt("Nugget") : -1;
    }
}
