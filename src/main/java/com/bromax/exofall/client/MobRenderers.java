package com.bromax.exofall.client;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.mob.Hideable;
import com.bromax.exofall.entity.mob.desert.Roller;
import com.bromax.exofall.entity.mob.desert.RolledBoulder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
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

    /**
     * Generic bug renderer. translucent: drawn see-through (glass wings, wing covers, bubbles).
     * glow: adds the emissive layer from textures/entity/<model>_glowmask.png. Hideable mobs
     * (burrowed Sanddivers) are not drawn at all, shadow included.
     */
    public static class BugRenderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T> {
        private final boolean translucent;
        private final float shadow;

        public BugRenderer(EntityRendererProvider.Context ctx, String model, float shadow, boolean translucent, boolean glow) {
            this(ctx, new DefaultedEntityGeoModel<>(Exofall.id(model), false), shadow, translucent, glow);
        }

        public BugRenderer(EntityRendererProvider.Context ctx, software.bernie.geckolib.model.GeoModel<T> model, float shadow, boolean translucent, boolean glow) {
            super(ctx, model);
            this.shadowRadius = shadow;
            this.shadow = shadow;
            this.translucent = translucent;
            if (glow) addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return translucent ? RenderType.entityTranslucent(texture) : super.getRenderType(animatable, texture, buffers, partialTick);
        }

        @Override
        public void render(T mob, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (mob instanceof Hideable h && h.isHidden()) {
                shadowRadius = 0;
                return;
            }
            shadowRadius = shadow;
            super.render(mob, yaw, partialTick, pose, buffers, light);
        }
    }

    /** Honeyguard: its wax shield is only drawn while it still has one. */
    public static class HoneyguardRenderer extends BugRenderer<com.bromax.exofall.entity.mob.hive.Honeyguard> {
        public HoneyguardRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, "honeyguard", 1.0f, true, false);
        }

        @Override
        public void preRender(PoseStack pose, com.bromax.exofall.entity.mob.hive.Honeyguard g, BakedGeoModel model, @Nullable MultiBufferSource buffers,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay, int colour) {
            model.getBone("shield").ifPresent(b -> b.setHidden(!g.hasShield()));
            super.preRender(pose, g, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    /** Canopy Assassin: drawn upside-down, feet on the ceiling, while it clings or climbs. */
    public static class AssassinRenderer extends BugRenderer<com.bromax.exofall.entity.mob.rainforest.CanopyAssassin> {
        public AssassinRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, "canopy_assassin", 0.6f, true, false);
        }

        private static boolean flipped(com.bromax.exofall.entity.mob.rainforest.CanopyAssassin a) {
            return a.state() == com.bromax.exofall.entity.mob.rainforest.CanopyAssassin.CLINGING
                    || a.state() == com.bromax.exofall.entity.mob.rainforest.CanopyAssassin.CLIMBING;
        }

        @Override
        protected void applyRotations(com.bromax.exofall.entity.mob.rainforest.CanopyAssassin a, PoseStack pose, float age, float yaw, float partialTick, float nativeScale) {
            if (flipped(a)) {
                // flip about Z; under the flip a Y turn runs backwards, so the yaw is negated to keep the heading
                pose.translate(0, a.getBbHeight(), 0);
                pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
                super.applyRotations(a, pose, age, -yaw, partialTick, nativeScale);
            } else {
                super.applyRotations(a, pose, age, yaw, partialTick, nativeScale);
            }
        }
    }

    /** Roller: the boulder it pushes is only drawn while it has one. */
    public static class RollerRenderer extends BugRenderer<Roller> {
        public RollerRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, "roller", 0.7f, false, false);
        }

        @Override
        public void preRender(PoseStack pose, Roller roller, BakedGeoModel model, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            model.getBone("boulder").ifPresent(b -> b.setHidden(!roller.hasBoulder()));
            super.preRender(pose, roller, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    public static class BoulderRenderer extends GeoEntityRenderer<RolledBoulder> {
        public BoulderRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(Exofall.id("rolled_boulder"), false));
            this.shadowRadius = 0.4f;
        }
    }

    /**
     * Dozen-legged Wrath: each trailing body segment turns by how much the heading changed between it and the
     * segment ahead, so the body follows the head's path (see DozenLeggedWrath#segmentYaw).
     */
    public static class WrathModel extends DefaultedEntityGeoModel<com.bromax.exofall.entity.boss.DozenLeggedWrath> {
        public WrathModel() {
            super(Exofall.id("dozen_legged_wrath"), false);
        }

        @Override
        public void setCustomAnimations(com.bromax.exofall.entity.boss.DozenLeggedWrath w, long id,
                                        software.bernie.geckolib.animation.AnimationState<com.bromax.exofall.entity.boss.DozenLeggedWrath> state) {
            super.setCustomAnimations(w, id, state);
            float pt = state.getPartialTick();
            for (int i = 1; i <= com.bromax.exofall.entity.boss.DozenLeggedWrath.SEGMENTS; i++) {
                int k = i;
                float turn = net.minecraft.util.Mth.wrapDegrees(w.segmentYaw(k - 1, pt) - w.segmentYaw(k, pt));
                var bone = getAnimationProcessor().getBone("seg" + k);
                if (bone != null) bone.setRotY(bone.getRotY() + turn * net.minecraft.util.Mth.DEG_TO_RAD);
            }
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
