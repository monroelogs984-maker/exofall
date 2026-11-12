package com.bromax.exofall.mixin;

import com.bromax.exofall.item.BaubleEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leafcutter Charm: skip the cobweb / berry bush slowdown. Runs on both sides, since the client moves the player. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void exofall$leafcutter(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity e && BaubleEvents.leafcutterPasses(e, state)) ci.cancel();
    }
}
