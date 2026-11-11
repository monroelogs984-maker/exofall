package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.ExofallBoat;
import com.bromax.exofall.entity.ExofallChestBoat;
import com.bromax.exofall.entity.mob.Bloodgnat;
import com.bromax.exofall.entity.mob.Glasswing;
import com.bromax.exofall.entity.mob.Mudgrub;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Exofall.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ExofallBoat>> BOAT = ENTITIES.register("boat",
            () -> EntityType.Builder.<ExofallBoat>of(ExofallBoat::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10).build("exofall:boat"));
    public static final DeferredHolder<EntityType<?>, EntityType<ExofallChestBoat>> CHEST_BOAT = ENTITIES.register("chest_boat",
            () -> EntityType.Builder.<ExofallChestBoat>of(ExofallChestBoat::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10).build("exofall:chest_boat"));

    // ---- Swamp meteor mobs
    public static final DeferredHolder<EntityType<?>, EntityType<Mudgrub>> MUDGRUB = ENTITIES.register("mudgrub",
            () -> EntityType.Builder.of(Mudgrub::new, MobCategory.CREATURE)
                    .sized(1.0F, 0.9F).eyeHeight(0.6F).clientTrackingRange(8).build("exofall:mudgrub"));
    public static final DeferredHolder<EntityType<?>, EntityType<Glasswing>> GLASSWING = ENTITIES.register("glasswing",
            () -> EntityType.Builder.of(Glasswing::new, MobCategory.MONSTER)
                    .sized(1.6F, 1.2F).eyeHeight(1.0F).clientTrackingRange(10).build("exofall:glasswing"));
    public static final DeferredHolder<EntityType<?>, EntityType<Bloodgnat>> BLOODGNAT = ENTITIES.register("bloodgnat",
            () -> EntityType.Builder.of(Bloodgnat::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.1F).eyeHeight(0.8F).clientTrackingRange(8).build("exofall:bloodgnat"));

    // ---- Meteor lesser mobs (generated list, see tools/mobs)
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.swamp.DivebellBeetle>> DIVEBELL_BEETLE = ENTITIES.register("divebell_beetle",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.swamp.DivebellBeetle::new, MobCategory.WATER_CREATURE)
                    .sized(1.2F, 0.7F).eyeHeight(0.5F).clientTrackingRange(10).build("exofall:divebell_beetle"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.swamp.Reedmantis>> REEDMANTIS = ENTITIES.register("reedmantis",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.swamp.Reedmantis::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.6F).eyeHeight(1.4F).clientTrackingRange(10).build("exofall:reedmantis"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.hive.Combwasp>> COMBWASP = ENTITIES.register("combwasp",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.hive.Combwasp::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.9F).eyeHeight(0.6F).clientTrackingRange(10).build("exofall:combwasp"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.hive.Stinger>> STINGER = ENTITIES.register("stinger",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.hive.Stinger::new, MobCategory.MONSTER)
                    .sized(1.4F, 1.3F).eyeHeight(0.9F).clientTrackingRange(10).build("exofall:stinger"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.hive.Nursemaid>> NURSEMAID = ENTITIES.register("nursemaid",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.hive.Nursemaid::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F).eyeHeight(0.7F).clientTrackingRange(10).build("exofall:nursemaid"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.hive.Waxmite>> WAXMITE = ENTITIES.register("waxmite",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.hive.Waxmite::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.6F).eyeHeight(0.35F).clientTrackingRange(10).build("exofall:waxmite"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.hive.LarvaCocoon>> LARVA_COCOON = ENTITIES.register("larva_cocoon",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.hive.LarvaCocoon::new, MobCategory.MONSTER)
                    .sized(0.75F, 1.2F).eyeHeight(0.8F).clientTrackingRange(10).build("exofall:larva_cocoon"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.desert.Sanddiver>> SANDDIVER = ENTITIES.register("sanddiver",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.desert.Sanddiver::new, MobCategory.MONSTER)
                    .sized(1.0F, 0.7F).eyeHeight(0.5F).clientTrackingRange(10).build("exofall:sanddiver"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.desert.Shieldback>> SHIELDBACK = ENTITIES.register("shieldback",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.desert.Shieldback::new, MobCategory.MONSTER)
                    .sized(1.5F, 1.3F).eyeHeight(0.6F).clientTrackingRange(10).build("exofall:shieldback"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.desert.EmberBeetle>> EMBER_BEETLE = ENTITIES.register("ember_beetle",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.desert.EmberBeetle::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.5F).eyeHeight(0.35F).clientTrackingRange(10).build("exofall:ember_beetle"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.desert.Roller>> ROLLER = ENTITIES.register("roller",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.desert.Roller::new, MobCategory.MONSTER)
                    .sized(1.1F, 0.9F).eyeHeight(0.6F).clientTrackingRange(10).build("exofall:roller"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.desert.GoldenScarab>> GOLDEN_SCARAB = ENTITIES.register("golden_scarab",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.desert.GoldenScarab::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.7F).eyeHeight(0.5F).clientTrackingRange(10).build("exofall:golden_scarab"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.rainforest.Twigwalker>> TWIGWALKER = ENTITIES.register("twigwalker",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.rainforest.Twigwalker::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.0F).eyeHeight(0.8F).clientTrackingRange(10).build("exofall:twigwalker"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.rainforest.Bloommantis>> BLOOMMANTIS = ENTITIES.register("bloommantis",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.rainforest.Bloommantis::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.6F).eyeHeight(1.4F).clientTrackingRange(10).build("exofall:bloommantis"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.rainforest.Leafcutter>> LEAFCUTTER = ENTITIES.register("leafcutter",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.rainforest.Leafcutter::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.8F).eyeHeight(0.5F).clientTrackingRange(10).build("exofall:leafcutter"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.rainforest.LanternMoth>> LANTERN_MOTH = ENTITIES.register("lantern_moth",
            () -> EntityType.Builder.of(com.bromax.exofall.entity.mob.rainforest.LanternMoth::new, MobCategory.AMBIENT)
                    .sized(0.8F, 0.6F).eyeHeight(0.4F).clientTrackingRange(10).build("exofall:lantern_moth"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.bromax.exofall.entity.mob.desert.RolledBoulder>> ROLLED_BOULDER = ENTITIES.register("rolled_boulder",
            () -> EntityType.Builder.<com.bromax.exofall.entity.mob.desert.RolledBoulder>of(com.bromax.exofall.entity.mob.desert.RolledBoulder::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F).clientTrackingRange(8).updateInterval(1).noSummon().build("exofall:rolled_boulder"));

    private ModEntities() {}
}
