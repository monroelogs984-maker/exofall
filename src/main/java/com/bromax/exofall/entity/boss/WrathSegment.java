package com.bromax.exofall.entity.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/** A hittable body segment of the Dozen-legged Wrath; damage goes to the head (the boss itself). */
public class WrathSegment extends PartEntity<DozenLeggedWrath> {
    private final EntityDimensions size;

    public WrathSegment(DozenLeggedWrath parent, float width, float height) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurt(source, amount * 0.8f);
    }

    @Override
    public boolean isPickable() {
        return !getParent().isHidden();
    }

    @Override
    public boolean is(Entity e) {
        return this == e || getParent() == e;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
