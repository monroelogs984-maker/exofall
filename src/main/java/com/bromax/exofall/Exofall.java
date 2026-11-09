package com.bromax.exofall;

import com.bromax.exofall.config.ExofallConfig;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModCreativeTabs;
import com.bromax.exofall.registry.ModItems;
import com.bromax.exofall.registry.ModWorldgen;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Exofall.MOD_ID)
public final class Exofall {
    public static final String MOD_ID = "exofall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Exofall(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModWorldgen.STRUCTURE_TYPES.register(modBus);
        ModWorldgen.STRUCTURE_PIECES.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, ExofallConfig.SPEC);
        LOGGER.info("BROMAX's Exofall loading");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
