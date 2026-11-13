package com.bromax.exofall.entity.mob.pet;

import com.bromax.exofall.registry.ModEntities;
import com.bromax.exofall.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * The four breeding pairs, one per meteor (Glenn, 2026-11-12). Only these eight bugs can be tamed and bred. Each pair
 * shares one treat: it tames either parent and puts tamed adults in the mood. (Hybrids were scrapped, 2026-11-13.)
 */
public enum PetPair {
    SWAMP(ModEntities.GLASSWING::get, ModEntities.MUDGRUB::get, () -> ModItems.SAPCANE_CANDY.get()),
    HIVE(ModEntities.COMBWASP::get, ModEntities.NURSEMAID::get, () -> ModItems.NECTAR_DROP.get()),
    DESERT(ModEntities.GOLDEN_SCARAB::get, ModEntities.SHIELDBACK::get, () -> ModItems.DUNE_PELLET.get()),
    RAINFOREST(ModEntities.BLOOMMANTIS::get, ModEntities.REEDMANTIS::get, () -> ModItems.MISTCAP_MORSEL.get());

    private final Supplier<EntityType<?>> a, b;
    private final Supplier<Item> treat;

    PetPair(Supplier<EntityType<?>> a, Supplier<EntityType<?>> b, Supplier<Item> treat) {
        this.a = a;
        this.b = b;
        this.treat = treat;
    }

    public Item treat() {
        return treat.get();
    }

    public boolean has(EntityType<?> t) {
        return t == a.get() || t == b.get();
    }

    public static PetPair of(EntityType<?> t) {
        for (PetPair p : values()) if (p.has(t)) return p;
        return null;
    }
}
