package com.bromax.exofall.entity.mob;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Attributes and natural-spawn rules. Which mobs spawn where is set in each meteor biome's spawners. */
@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class MobSetup {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MUDGRUB.get(), Mudgrub.createAttributes().build());
        event.put(ModEntities.GLASSWING.get(), Glasswing.createAttributes().build());
        event.put(ModEntities.BLOODGNAT.get(), Bloodgnat.createAttributes().build());
    }

    /** Meteor interiors are lit by plants, so monsters ignore light; they only spawn in meteor biomes anyway. */
    @SubscribeEvent
    public static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.MUDGRUB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mudgrub::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GLASSWING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BLOODGNAT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private MobSetup() {}
}
