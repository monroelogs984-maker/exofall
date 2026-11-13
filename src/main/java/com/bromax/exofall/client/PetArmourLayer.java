package com.bromax.exofall.client;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.item.BugArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.Set;

/**
 * Bug armour on a tamed pet: every cube of the body bones (body, thorax, head, neck, the first abdomen segments, the
 * Mudgrub's front segments) is drawn again as a slightly bigger shell with the armour's tiling plate texture
 * (textures/entity/bug_armour/&lt;set&gt;.png, 32x32, one texel per model pixel), like vanilla armour over a player. Legs,
 * wings, antennae and tails stay bare, so it works on all eight models without per-model textures.
 */
public class PetArmourLayer<T extends Mob & GeoEntity> extends GeoRenderLayer<T> {
    static final Set<String> BONES = Set.of("body", "thorax", "head", "neck", "seg0", "seg1", "seg2", "seg3", "ab0", "ab1", "ab2");
    private static final float INFLATE = 0.03f, TEX = 32f / 16f;   // texture size in blocks

    public PetArmourLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack pose, T mob, GeoBone bone, RenderType renderType, MultiBufferSource buffers, VertexConsumer buffer,
                              float partialTick, int light, int overlay) {
        if (bone.isHidden() || !BONES.contains(bone.getName()) || !com.bromax.exofall.config.ExofallClientConfig.PET_ARMOUR.get()) return;
        if (!(mob.getItemBySlot(EquipmentSlot.BODY).getItem() instanceof BugArmorItem armour)) return;
        ResourceLocation tex = Exofall.id("textures/entity/bug_armour/" + armour.set().id + ".png");
        VertexConsumer out = buffers.getBuffer(RenderType.armorCutoutNoCull(tex));
        for (GeoCube cube : bone.getCubes()) {
            pose.pushPose();
            RenderUtil.translateToPivotPoint(pose, cube);
            RenderUtil.rotateMatrixAroundCube(pose, cube);
            RenderUtil.translateAwayFromPivotPoint(pose, cube);
            shell(pose.last(), cube, out, light, overlay);
            pose.popPose();
        }
    }

    private static void shell(PoseStack.Pose pose, GeoCube cube, VertexConsumer out, int light, int overlay) {
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, z0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE, z1 = -Float.MAX_VALUE;
        for (GeoQuad q : cube.quads()) {
            if (q == null) continue;
            for (GeoVertex v : q.vertices()) {
                Vector3f p = v.position();
                x0 = Math.min(x0, p.x); y0 = Math.min(y0, p.y); z0 = Math.min(z0, p.z);
                x1 = Math.max(x1, p.x); y1 = Math.max(y1, p.y); z1 = Math.max(z1, p.z);
            }
        }
        if (x0 > x1 || Math.min(x1 - x0, Math.min(y1 - y0, z1 - z0)) < 0.04f) return;   // skip flat cubes (membranes, fins)
        x0 -= INFLATE; y0 -= INFLATE; z0 -= INFLATE; x1 += INFLATE; y1 += INFLATE; z1 += INFLATE;
        float w = (x1 - x0) / TEX, h = (y1 - y0) / TEX, d = (z1 - z0) / TEX;
        Matrix4f m = pose.pose();
        // +y (top), -y, +z, -z, +x, -x
        quad(out, m, pose, light, overlay, 0, 1, 0, w, d, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        quad(out, m, pose, light, overlay, 0, -1, 0, w, d, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(out, m, pose, light, overlay, 0, 0, 1, w, h, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(out, m, pose, light, overlay, 0, 0, -1, w, h, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        quad(out, m, pose, light, overlay, 1, 0, 0, d, h, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        quad(out, m, pose, light, overlay, -1, 0, 0, d, h, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
    }

    /** One face, corners counter-clockwise from the bottom-left; u runs 0..uMax, v uMax..0 (tiles past 1). */
    private static void quad(VertexConsumer out, Matrix4f m, PoseStack.Pose pose, int light, int overlay, float nx, float ny, float nz,
                             float uMax, float vMax, float... c) {
        float[][] uv = {{0, vMax}, {uMax, vMax}, {uMax, 0}, {0, 0}};
        for (int i = 0; i < 4; i++) {
            out.addVertex(m, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(0xFFFFFFFF).setUv(uv[i][0], uv[i][1])
                    .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
