package com.bromax.exofall.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config (config/exofall-client.toml): only how things look on this computer. */
public final class ExofallClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BOSS_CINEMATIC = BUILDER
            .comment("Play the camera intro when a boss wakes. Off = you keep your own view during its 5 second wake-up.")
            .define("bossCinematic", true);
    public static final ModConfigSpec.BooleanValue PET_ARMOUR = BUILDER
            .comment("Draw bug armour on tamed pets.")
            .define("showPetArmour", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ExofallClientConfig() {}
}
