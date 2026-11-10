package com.bromax.exofall.client;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.BoatWood;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

/** Vanilla's boat rendering, with models and textures picked by Exofall wood instead of Boat.Type. */
public class ExofallBoatRenderer extends EntityRenderer<Boat> {
    private final boolean chest;
    private final ListModel<Boat>[] models;
    private final ResourceLocation[] textures;

    public static ModelLayerLocation layer(BoatWood wood, boolean chest) {
        return new ModelLayerLocation(Exofall.id((chest ? "chest_boat/" : "boat/") + wood.name), "main");
    }

    @SuppressWarnings("unchecked")
    public ExofallBoatRenderer(EntityRendererProvider.Context ctx, boolean chest) {
        super(ctx);
        this.chest = chest;
        this.shadowRadius = 0.8F;
        BoatWood[] woods = BoatWood.values();
        models = new ListModel[woods.length];
        textures = new ResourceLocation[woods.length];
        for (BoatWood w : woods) {
            var part = ctx.bakeLayer(layer(w, chest));
            models[w.ordinal()] = chest ? new ChestBoatModel(part) : new BoatModel(part);
            textures[w.ordinal()] = Exofall.id("textures/entity/" + (chest ? "chest_boat/" : "boat/") + w.name + ".png");
        }
    }

    @Override
    public void render(Boat boat, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.375F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float hurt = boat.getHurtTime() - partial;
        float damage = Math.max(0.0F, boat.getDamage() - partial);
        if (hurt > 0.0F) pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * damage / 10.0F * boat.getHurtDir()));
        float bubble = boat.getBubbleAngle(partial);
        if (!Mth.equal(bubble, 0.0F)) pose.mulPose(new Quaternionf().setAngleAxis(bubble * (float) (Math.PI / 180.0), 1.0F, 0.0F, 1.0F));

        int i = ((BoatWood.Holder) boat).exofallWood().ordinal();
        ListModel<Boat> model = models[i];
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        model.setupAnim(boat, partial, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vc = buffers.getBuffer(model.renderType(textures[i]));
        model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY);
        if (!boat.isUnderWater() && model instanceof WaterPatchModel patch) {
            patch.waterPatch().render(pose, buffers.getBuffer(RenderType.waterMask()), light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        super.render(boat, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Boat boat) {
        return textures[((BoatWood.Holder) boat).exofallWood().ordinal()];
    }
}
