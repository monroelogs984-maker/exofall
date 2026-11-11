package com.bromax.exofall.entity.mob;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
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
        event.put(ModEntities.DIVEBELL_BEETLE.get(), com.bromax.exofall.entity.mob.swamp.DivebellBeetle.createAttributes().build());
        event.put(ModEntities.REEDMANTIS.get(), com.bromax.exofall.entity.mob.swamp.Reedmantis.createAttributes().build());
        event.put(ModEntities.COMBWASP.get(), com.bromax.exofall.entity.mob.hive.Combwasp.createAttributes().build());
        event.put(ModEntities.STINGER.get(), com.bromax.exofall.entity.mob.hive.Stinger.createAttributes().build());
        event.put(ModEntities.NURSEMAID.get(), com.bromax.exofall.entity.mob.hive.Nursemaid.createAttributes().build());
        event.put(ModEntities.WAXMITE.get(), com.bromax.exofall.entity.mob.hive.Waxmite.createAttributes().build());
        event.put(ModEntities.LARVA_COCOON.get(), com.bromax.exofall.entity.mob.hive.LarvaCocoon.createAttributes().build());
        event.put(ModEntities.SANDDIVER.get(), com.bromax.exofall.entity.mob.desert.Sanddiver.createAttributes().build());
        event.put(ModEntities.SHIELDBACK.get(), com.bromax.exofall.entity.mob.desert.Shieldback.createAttributes().build());
        event.put(ModEntities.EMBER_BEETLE.get(), com.bromax.exofall.entity.mob.desert.EmberBeetle.createAttributes().build());
        event.put(ModEntities.ROLLER.get(), com.bromax.exofall.entity.mob.desert.Roller.createAttributes().build());
        event.put(ModEntities.GOLDEN_SCARAB.get(), com.bromax.exofall.entity.mob.desert.GoldenScarab.createAttributes().build());
        event.put(ModEntities.TWIGWALKER.get(), com.bromax.exofall.entity.mob.rainforest.Twigwalker.createAttributes().build());
        event.put(ModEntities.BLOOMMANTIS.get(), com.bromax.exofall.entity.mob.rainforest.Bloommantis.createAttributes().build());
        event.put(ModEntities.LEAFCUTTER.get(), com.bromax.exofall.entity.mob.rainforest.Leafcutter.createAttributes().build());
        event.put(ModEntities.LANTERN_MOTH.get(), com.bromax.exofall.entity.mob.rainforest.LanternMoth.createAttributes().build());
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
        event.register(ModEntities.DIVEBELL_BEETLE.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.bromax.exofall.entity.mob.swamp.DivebellBeetle::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.REEDMANTIS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.COMBWASP.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.STINGER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.NURSEMAID.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WAXMITE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.LARVA_COCOON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SANDDIVER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SHIELDBACK.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.EMBER_BEETLE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anySolidGround, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ROLLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GOLDEN_SCARAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anySolidGround, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.TWIGWALKER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anySolidGround, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BLOOMMANTIS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.LEAFCUTTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anySolidGround, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.LANTERN_MOTH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MobSetup::anyLightHostile, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Hostile bugs: any light level (meteor interiors are plant-lit), not in Peaceful. */
    static <T extends net.minecraft.world.entity.Mob> boolean anyLightHostile(EntityType<T> type, net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    /** Creatures: any solid floor at any light level. */
    static <T extends net.minecraft.world.entity.Mob> boolean anySolidGround(EntityType<T> type, net.minecraft.world.level.LevelAccessor level,
            net.minecraft.world.entity.MobSpawnType reason, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    private MobSetup() {}
}
