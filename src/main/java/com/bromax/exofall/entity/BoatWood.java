package com.bromax.exofall.entity;

import com.bromax.exofall.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/** The alien woods that make boats. Stored by name on the entity so new woods never shift old saves. */
public enum BoatWood {
    STILTREED("stiltreed", () -> ModItems.BOATS.get("stiltreed_boat").get(), () -> ModItems.BOATS.get("stiltreed_chest_boat").get()),
    COMBWOOD("combwood", () -> ModItems.BOATS.get("combwood_boat").get(), () -> ModItems.BOATS.get("combwood_chest_boat").get()),
    RIBWOOD("ribwood", () -> ModItems.BOATS.get("ribwood_boat").get(), () -> ModItems.BOATS.get("ribwood_chest_boat").get()),
    VEILPINE("veilpine", () -> ModItems.BOATS.get("veilpine_boat").get(), () -> ModItems.BOATS.get("veilpine_chest_boat").get());

    public final String name;
    private final Supplier<Item> boat, chestBoat;

    BoatWood(String name, Supplier<Item> boat, Supplier<Item> chestBoat) {
        this.name = name;
        this.boat = boat;
        this.chestBoat = chestBoat;
    }

    public Item item(boolean chest) {
        return chest ? chestBoat.get() : boat.get();
    }

    public static BoatWood byName(String name) {
        for (BoatWood w : values()) if (w.name.equals(name)) return w;
        return STILTREED;
    }

    public static BoatWood byId(int id) {
        return values()[Math.floorMod(id, values().length)];
    }

    /** Implemented by both Exofall boat entities. */
    public interface Holder {
        BoatWood exofallWood();
        void setExofallWood(BoatWood wood);
    }
}
