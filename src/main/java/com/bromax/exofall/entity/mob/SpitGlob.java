package com.bromax.exofall.entity.mob;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A spat glob, drawn as its item. Silk (Bogspinner, cobweb look): 2 damage and a Bogsilk Web where
 * it lands. Propolis (Propolis Spitter, honey-block look): 4 damage, Slowness II 3 s, and a splash
 * of sticky resin puddles on the floor around the hit.
 */
public class SpitGlob extends ThrowableItemProjectile {
    public SpitGlob(EntityType<? extends SpitGlob> type, Level level) {
        super(type, level);
    }

    public static SpitGlob silk(LivingEntity owner) {
        return make(owner, Items.COBWEB);
    }

    public static SpitGlob propolis(LivingEntity owner) {
        return make(owner, Items.HONEY_BLOCK);
    }

    private static SpitGlob make(LivingEntity owner, Item look) {
        SpitGlob g = new SpitGlob(ModEntities.SPIT_GLOB.get(), owner.level());
        g.setOwner(owner);
        g.setItem(new ItemStack(look));
        g.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        return g;
    }

    public boolean isPropolis() {
        return getItem().is(Items.HONEY_BLOCK);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBWEB;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
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
            le.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity o ? o : null), isPropolis() ? 12 : 6);
            if (isPropolis()) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel sl)) return;
        BlockPos at = result instanceof BlockHitResult b ? b.getBlockPos().relative(b.getDirection())
                : result instanceof EntityHitResult e ? e.getEntity().blockPosition() : BlockPos.containing(result.getLocation());
        if (isPropolis()) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if ((dx == 0 && dz == 0) || random.nextFloat() < 0.55f) puddle(sl, at.offset(dx, 0, dz));
            playSound(SoundEvents.HONEY_BLOCK_BREAK, 1.0f, 0.8f);
        } else {
            BlockState s = sl.getBlockState(at);
            if (s.canBeReplaced() && s.getFluidState().isEmpty()) sl.setBlockAndUpdate(at, ModBlocks.BOGSILK_WEB.get().defaultBlockState());
            playSound(SoundEvents.WOOL_PLACE, 1.0f, 1.3f);
        }
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.08);
        discard();
    }

    /** Drop the puddle onto the first floor within 2 blocks below. */
    private static void puddle(ServerLevel sl, BlockPos p) {
        for (int i = 0; i < 3; i++, p = p.below()) {
            BlockState s = sl.getBlockState(p);
            if (!s.canBeReplaced() || !s.getFluidState().isEmpty()) {
                if (i == 0) return;
                continue;
            }
            BlockState pud = ModBlocks.PROPOLIS.get().defaultBlockState();
            if (pud.canSurvive(sl, p)) {
                sl.setBlockAndUpdate(p, pud);
                return;
            }
        }
    }
}
