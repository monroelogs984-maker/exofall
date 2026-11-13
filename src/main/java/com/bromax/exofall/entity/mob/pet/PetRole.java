package com.bromax.exofall.entity.mob.pet;

import com.bromax.exofall.registry.ModEntities;
import net.minecraft.world.entity.EntityType;

/** What a tamed bug does (Glenn, 2026-11-13): five fighters with their own style, three helpers that never fight. */
public enum PetRole {
    /** Telegraphed dive (its wild attack). */
    GLASSWING(true),
    /** Poison sting. */
    COMBWASP(true),
    /** Tank: front shield, pulls monsters off its owner. */
    SHIELDBACK(true),
    /** Ambush: big first hit on every new target. */
    MANTIS(true),
    /** Heals the owner and the owner's pets. */
    NURSEMAID(false),
    /** Digs up swamp loot. */
    MUDGRUB(false),
    /** Senses ores, digs up gold. */
    GOLDEN_SCARAB(false);

    public final boolean fighter;

    PetRole(boolean fighter) {
        this.fighter = fighter;
    }

    public static PetRole of(EntityType<?> t) {
        if (t == ModEntities.GLASSWING.get()) return GLASSWING;
        if (t == ModEntities.COMBWASP.get()) return COMBWASP;
        if (t == ModEntities.SHIELDBACK.get()) return SHIELDBACK;
        if (t == ModEntities.REEDMANTIS.get() || t == ModEntities.BLOOMMANTIS.get()) return MANTIS;
        if (t == ModEntities.NURSEMAID.get()) return NURSEMAID;
        if (t == ModEntities.MUDGRUB.get()) return MUDGRUB;
        if (t == ModEntities.GOLDEN_SCARAB.get()) return GOLDEN_SCARAB;
        return null;
    }
}
