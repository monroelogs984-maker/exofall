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

    private ModEntities() {}
}
