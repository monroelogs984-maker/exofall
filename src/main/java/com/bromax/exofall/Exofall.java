package com.bromax.exofall;

import com.bromax.exofall.block.Stripping;
import com.bromax.exofall.config.ExofallConfig;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModCreativeTabs;
import com.bromax.exofall.registry.ModEntities;
import com.bromax.exofall.registry.ModItems;
import com.bromax.exofall.registry.ModRecipes;
import com.bromax.exofall.registry.ModWorldgen;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Exofall.MOD_ID)
public final class Exofall {
    public static final String MOD_ID = "exofall";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Exofall(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        com.bromax.exofall.item.ArmorSet.register(modBus);
        ModItems.ITEMS.register(modBus);
        com.bromax.exofall.registry.ModBlockEntities.TYPES.register(modBus);
        com.bromax.exofall.registry.ModEffects.EFFECTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.MENUS.register(modBus);
        ModWorldgen.STRUCTURE_TYPES.register(modBus);
        ModWorldgen.STRUCTURE_PIECES.register(modBus);
        NeoForge.EVENT_BUS.addListener(Stripping::onToolModify);
        NeoForge.EVENT_BUS.addListener(ExofallCommands::register);
        container.registerConfig(ModConfig.Type.COMMON, ExofallConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, com.bromax.exofall.config.ExofallClientConfig.SPEC);
        LOGGER.info("BROMAX's Exofall loading");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
