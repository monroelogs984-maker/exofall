package com.bromax.exofall.world;

import com.bromax.exofall.world.interior.DesertInterior;
import com.bromax.exofall.world.interior.HiveInterior;
import com.bromax.exofall.world.interior.RainforestInterior;
import com.bromax.exofall.world.interior.SwampInterior;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

/** The four meteor kinds. Each owns an interior generator; shell and biome handling are shared. */
public enum MeteorStyle implements StringRepresentable {
    SWAMP("swamp", SwampInterior::new),
    HIVE("hive", HiveInterior::new),
    DESERT("desert", DesertInterior::new),
    RAINFOREST("rainforest", RainforestInterior::new);

    public static final Codec<MeteorStyle> CODEC = StringRepresentable.fromEnum(MeteorStyle::values);

    private final String name;
    private final MeteorInterior interior;

    MeteorStyle(String name, Supplier<MeteorInterior> interior) {
        this.name = name;
        this.interior = interior.get();
    }

    public MeteorInterior interior() {
        return interior;
    }

    public static MeteorStyle byName(String name) {
        for (MeteorStyle s : values()) if (s.name.equals(name)) return s;
        return SWAMP;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
