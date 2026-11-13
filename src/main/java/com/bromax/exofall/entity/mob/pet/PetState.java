package com.bromax.exofall.entity.mob.pet;

import net.minecraft.nbt.CompoundTag;

/** Per-entity pet data. Baby-ness lives in each entity's synced data (it changes the size), the rest is server-only. */
public final class PetState {
    public boolean tamed;
    /** Negative while a baby: counts up to 0, then it's grown. */
    public int age;
    /** Ticks left in the mood to breed. */
    public int love;
    /** Ticks before it can breed again. */
    public int cooldown;

    public void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Tamed", tamed);
        t.putInt("Age", age);
        t.putInt("Cooldown", cooldown);
        tag.put("ExofallPet", t);
    }

    public void load(CompoundTag tag) {
        CompoundTag t = tag.getCompound("ExofallPet");
        tamed = t.getBoolean("Tamed");
        age = t.getInt("Age");
        cooldown = t.getInt("Cooldown");
    }
}
