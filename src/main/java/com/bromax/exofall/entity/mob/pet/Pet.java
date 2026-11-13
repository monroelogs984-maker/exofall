package com.bromax.exofall.entity.mob.pet;

/** A bug that can be tamed and bred (see {@link Pets}). */
public interface Pet {
    PetState petState();

    default boolean isTamedPet() {
        return petState().tamed;
    }
}
