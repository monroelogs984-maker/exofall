package com.bromax.exofall.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common config. Defaults are tuned for Sovereign 202's late game; other packs can scale them. */
public final class ExofallConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue GEAR_DAMAGE_MULTIPLIER = BUILDER
            .comment("Multiplier on Exofall weapon damage. 1.0 = Sovereign 202 balance.")
            .defineInRange("gear.damageMultiplier", 1.0, 0.1, 10.0);

    public static final ModConfigSpec.DoubleValue GEAR_ARMOR_MULTIPLIER = BUILDER
            .comment("Multiplier on Exofall armour and toughness values. 1.0 = Sovereign 202 balance.")
            .defineInRange("gear.armorMultiplier", 1.0, 0.1, 10.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ExofallConfig() {}
}
