package com.bromax.exofall.client;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.boss.ExofallBoss;
import com.bromax.exofall.network.CinematicPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Boss intro cinematic (client). The view switches to a free camera that starts at the player's eyes,
 * swings round and pushes in on the boss's face while it roars, with letterbox bars and the boss's name and
 * subtitle. The player can't move or attack meanwhile (the server also makes them invulnerable), and the
 * HUD is hidden. Ends early if the boss disappears or the player dies.
 */
@EventBusSubscriber(modid = Exofall.MOD_ID, value = Dist.CLIENT)
public final class BossCinematic {
    private static int bossId = -1, age, total;
    @Nullable private static Marker cam;
    private static Vec3 startEye = Vec3.ZERO;
    private static float startYaw, startPitch;
    @Nullable private static CameraType savedCamera;

    public static void start(CinematicPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !com.bromax.exofall.config.ExofallClientConfig.BOSS_CINEMATIC.get()) return;
        Entity boss = mc.level.getEntity(p.entityId());
        if (!(boss instanceof ExofallBoss)) return;
        stop();
        bossId = p.entityId();
        total = p.ticks();
        age = 0;
        startEye = mc.player.getEyePosition();
        startYaw = mc.player.getYRot();
        startPitch = mc.player.getXRot();
        cam = new Marker(EntityType.MARKER, mc.level);
        cam.moveTo(startEye.x, startEye.y, startEye.z, startYaw, startPitch);
        cam.setOldPosAndRot();
        savedCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(cam);
    }

    public static boolean active() {
        return cam != null;
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (cam != null && mc.player != null) mc.setCameraEntity(mc.player);
        if (savedCamera != null) mc.options.setCameraType(savedCamera);
        savedCamera = null;
        cam = null;
        bossId = -1;
    }

    @Nullable
    private static ExofallBoss boss() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getEntity(bossId) instanceof ExofallBoss b ? b : null;
    }

    private static double ease(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (cam == null) return;
        Minecraft mc = Minecraft.getInstance();
        ExofallBoss b = boss();
        if (++age >= total || b == null || mc.player == null || !mc.player.isAlive() || cam.level() != mc.level) {
            stop();
            return;
        }
        cam.setOldPosAndRot();
        double p = age / (double) total;
        // Boss face and front
        Vec3 face = b.position().add(0, b.getBbHeight() * 0.6, 0);
        float bossYaw = b.getYRot();
        double dist = Math.max(4.5, b.getBbWidth() * 1.8 + 3.0);
        double height = b.getBbHeight() * 0.45 + 0.8;
        // Orbit from 50 degrees off the boss's right side to 15 degrees off its left while pushing in
        double orbit = Math.toRadians(bossYaw + Mth.lerp(ease((p - 0.1) / 0.75), 50, -15));
        double push = Mth.lerp(ease((p - 0.45) / 0.45), 1.0, 0.8);
        Vec3 shot = b.position().add(-Math.sin(orbit) * dist * push, height, Math.cos(orbit) * dist * push);
        Vec3 pos = startEye.lerp(shot, ease(p / 0.35));
        // Don't put the camera inside a wall
        var hit = mc.level.clip(new ClipContext(face, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cam));
        if (hit.getType() == HitResult.Type.BLOCK) pos = hit.getLocation().add(face.subtract(hit.getLocation()).normalize().scale(0.4));
        // Roar shake
        if (p > 0.45 && p < 0.75) {
            double amp = 0.06 * Math.sin((p - 0.45) / 0.3 * Math.PI);
            pos = pos.add((mc.level.random.nextDouble() - 0.5) * amp, (mc.level.random.nextDouble() - 0.5) * amp, 0);
        }
        Vec3 look = face.subtract(pos);
        float yaw = (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
        float turn = (float) ease(p / 0.2);
        yaw = startYaw + Mth.wrapDegrees(yaw - startYaw) * turn;
        yaw = cam.yRotO + Mth.wrapDegrees(yaw - cam.yRotO);   // keep it continuous so it never spins the long way
        pitch = Mth.lerp(turn, startPitch, pitch);
        cam.setPos(pos.x, pos.y, pos.z);
        cam.setYRot(yaw);
        cam.setXRot(pitch);
    }

    @SubscribeEvent
    public static void hideHud(RenderGuiLayerEvent.Pre event) {
        if (cam != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void drawOverlay(RenderGuiEvent.Post event) {
        if (cam == null) return;
        ExofallBoss b = boss();
        if (b == null) return;
        GuiGraphics g = event.getGuiGraphics();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        double p = (age + pt) / total;
        int w = g.guiWidth(), h = g.guiHeight();
        double bars = Math.min(ease(p / 0.12), ease((1 - p) / 0.1));
        int barH = (int) Math.round(h * 0.12 * bars);
        g.fill(0, 0, w, barH, 0xFF000000);
        g.fill(0, h - barH, w, h, 0xFF000000);
        double a = Math.min(ease((p - 0.4) / 0.12), ease((0.97 - p) / 0.1));
        int alpha = (int) (a * 255);
        if (alpha < 8) return;
        Font font = Minecraft.getInstance().font;
        Component name = b.getDisplayName();
        Component sub = b.subtitle();
        g.pose().pushPose();
        g.pose().translate(w / 2f, h * 0.68f, 0);
        g.pose().scale(3f, 3f, 1f);
        g.drawString(font, name, -font.width(name) / 2, 0, (alpha << 24) | 0xF2E2B0, true);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(w / 2f, h * 0.68f + 32, 0);
        g.pose().scale(1.25f, 1.25f, 1f);
        g.drawString(font, sub, -font.width(sub) / 2, 0, (alpha << 24) | 0xB0A890, true);
        g.pose().popPose();
    }

    @SubscribeEvent
    public static void freezeMovement(MovementInputUpdateEvent event) {
        if (cam == null) return;
        var in = event.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void blockInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (cam != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private BossCinematic() {}
}
