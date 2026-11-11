package com.bromax.exofall.client;

import com.bromax.exofall.Exofall;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** GeckoLib renderers for Exofall mobs. Models/animations/textures come from tools/mobs/gen_mobs.py. */
public final class MobRenderers {
    private MobRenderers() {}

    /** Drawn see-through so the glassy wings show what is behind them. */
    public static class SeeThrough<T extends Mob & GeoEntity> extends GeoEntityRenderer<T> {
        public SeeThrough(EntityRendererProvider.Context ctx, String model, float shadow) {
            super(ctx, new DefaultedEntityGeoModel<>(Exofall.id(model), false));
            this.shadowRadius = shadow;
        }

        @Override
        public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    /** Babies are drawn at half size. */
    public static class Mudgrub extends GeoEntityRenderer<com.bromax.exofall.entity.mob.Mudgrub> {
        public Mudgrub(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(Exofall.id("mudgrub"), false));
            this.shadowRadius = 0.6f;
        }

        @Override
        public void render(com.bromax.exofall.entity.mob.Mudgrub grub, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            withScale(grub.isBaby() ? 0.5f : 1f);
            super.render(grub, yaw, partialTick, pose, buffers, light);
        }
    }
}
