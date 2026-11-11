package com.bromax.exofall.entity.boss;

import com.bromax.exofall.entity.mob.Bug;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/** A lobbed glob (bog mud, queen's wax): damage plus a lingering effect. Drawn as its item. */
public class BossGlob extends ThrowableItemProjectile {
    private float damage = 6;
    @Nullable private Holder<MobEffect> effect;
    private int effectTicks, effectAmp;

    public BossGlob(EntityType<? extends BossGlob> type, Level level) {
        super(type, level);
    }

    public static BossGlob shoot(ExofallBoss owner, Item look, float damage, @Nullable Holder<MobEffect> effect, int ticks, int amp) {
        BossGlob g = new BossGlob(com.bromax.exofall.registry.ModEntities.BOSS_GLOB.get(), owner.level());
        g.setOwner(owner);
        g.setItem(new ItemStack(look));
        g.damage = damage;
        g.effect = effect;
        g.effectTicks = ticks;
        g.effectAmp = amp;
        return g;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.MUD;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !(e instanceof Bug);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity owner = getOwner();
        if (hit.getEntity() instanceof LivingEntity le) {
            le.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity o ? o : null), damage);
            if (effect != null) le.addEffect(new MobEffectInstance(effect, effectTicks, effectAmp));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 10; i++) {
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.25, (random.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100 && !level().isClientSide) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        if (effect != null) {
            tag.putString("Effect", effect.unwrapKey().map(k -> k.location().toString()).orElse(""));
            tag.putInt("EffectTicks", effectTicks);
            tag.putInt("EffectAmp", effectAmp);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (tag.contains("Effect")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Effect"));
            effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id).map(h -> (Holder<MobEffect>) h).orElse(null);
            effectTicks = tag.getInt("EffectTicks");
            effectAmp = tag.getInt("EffectAmp");
        }
    }
}
