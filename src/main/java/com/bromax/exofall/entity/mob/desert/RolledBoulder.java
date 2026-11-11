package com.bromax.exofall.entity.mob.desert;

import com.bromax.exofall.registry.ModEntities;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A packed sand-dung ball shoved by a Roller. Rolls straight, falls off edges, shatters on walls or on a hit. */
public class RolledBoulder extends Entity implements GeoEntity {
    private static final RawAnimation ROLL = RawAnimation.begin().thenLoop("misc.idle");
    static final double SPEED = 0.55;
    static final float DAMAGE = 10;
    static final int LIFETIME = 100;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 dir = Vec3.ZERO;
    @Nullable private UUID ownerId;
    private int age;

    public RolledBoulder(EntityType<? extends RolledBoulder> type, Level level) {
        super(type, level);
    }

    public RolledBoulder(Level level, LivingEntity owner, Vec3 dir) {
        this(ModEntities.ROLLED_BOULDER.get(), level);
        this.ownerId = owner.getUUID();
        this.dir = dir;
        setYRot((float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Nullable
    private LivingEntity owner() {
        if (ownerId != null && level() instanceof ServerLevel sl && sl.getEntity(ownerId) instanceof LivingEntity le) return le;
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Vec3 dm = getDeltaMovement();
        dm = new Vec3(dir.x * SPEED, onGround() ? -0.04 : dm.y - 0.08, dir.z * SPEED);
        setDeltaMovement(dm);
        move(MoverType.SELF, dm);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.1),
                e -> e.isAlive() && !(e instanceof Roller) && !e.isSpectator())) {
            LivingEntity owner = owner();
            e.hurt(damageSources().mobProjectile(this, owner), DAMAGE);
            e.knockback(0.9, -dir.x, -dir.z);
            shatter();
            return;
        }
        if (horizontalCollision || ++age > LIFETIME) shatter();
    }

    /** Mobs don't block it like a boat (it would shatter on its own Roller); hits are checked above. */
    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    private void shatter() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_MUD.defaultBlockState()),
                    getX(), getY() + 0.4, getZ(), 30, 0.35, 0.35, 0.35, 0.1);
            playSound(SoundEvents.MUD_BREAK, 1.2f, 0.7f);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved()) shatter();
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        dir = new Vec3(tag.getDouble("DirX"), 0, tag.getDouble("DirZ"));
        age = tag.getInt("Age");
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("DirX", dir.x);
        tag.putDouble("DirZ", dir.z);
        tag.putInt("Age", age);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(ROLL)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
