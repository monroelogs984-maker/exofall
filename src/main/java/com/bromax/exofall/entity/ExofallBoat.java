package com.bromax.exofall.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A boat made of an alien wood. Plays exactly like a vanilla boat. */
public class ExofallBoat extends Boat implements BoatWood.Holder {
    private static final EntityDataAccessor<Integer> WOOD = SynchedEntityData.defineId(ExofallBoat.class, EntityDataSerializers.INT);

    public ExofallBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WOOD, 0);
    }

    @Override public BoatWood exofallWood() { return BoatWood.byId(entityData.get(WOOD)); }
    @Override public void setExofallWood(BoatWood wood) { entityData.set(WOOD, wood.ordinal()); }

    @Override
    public Item getDropItem() {
        return exofallWood().item(false);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ExofallWood", exofallWood().name);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setExofallWood(BoatWood.byName(tag.getString("ExofallWood")));
    }
}
