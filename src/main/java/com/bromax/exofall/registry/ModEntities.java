package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.ExofallBoat;
import com.bromax.exofall.entity.ExofallChestBoat;
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

    private ModEntities() {}
}
