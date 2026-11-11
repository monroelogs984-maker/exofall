package com.bromax.exofall.client;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.BoatWood;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Exofall.MOD_ID, value = Dist.CLIENT)
public final class ExofallClient {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BOAT.get(), ctx -> new ExofallBoatRenderer(ctx, false));
        event.registerEntityRenderer(ModEntities.CHEST_BOAT.get(), ctx -> new ExofallBoatRenderer(ctx, true));
        event.registerEntityRenderer(ModEntities.MUDGRUB.get(), MobRenderers.Mudgrub::new);
        event.registerEntityRenderer(ModEntities.GLASSWING.get(), ctx -> new MobRenderers.SeeThrough<>(ctx, "glasswing", 0.9f));
        event.registerEntityRenderer(ModEntities.BLOODGNAT.get(), ctx -> new MobRenderers.SeeThrough<>(ctx, "bloodgnat", 0.4f));
        event.registerEntityRenderer(ModEntities.DIVEBELL_BEETLE.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "divebell_beetle", 0.6f, true, false));
        event.registerEntityRenderer(ModEntities.REEDMANTIS.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "reedmantis", 0.5f, true, false));
        event.registerEntityRenderer(ModEntities.COMBWASP.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "combwasp", 0.4f, true, false));
        event.registerEntityRenderer(ModEntities.STINGER.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "stinger", 0.7f, true, false));
        event.registerEntityRenderer(ModEntities.NURSEMAID.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "nursemaid", 0.5f, true, true));
        event.registerEntityRenderer(ModEntities.WAXMITE.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "waxmite", 0.5f, false, false));
        event.registerEntityRenderer(ModEntities.LARVA_COCOON.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "larva_cocoon", 0.45f, false, true));
        event.registerEntityRenderer(ModEntities.SANDDIVER.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "sanddiver", 0.6f, false, false));
        event.registerEntityRenderer(ModEntities.SHIELDBACK.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "shieldback", 0.9f, false, false));
        event.registerEntityRenderer(ModEntities.EMBER_BEETLE.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "ember_beetle", 0.35f, false, true));
        event.registerEntityRenderer(ModEntities.ROLLER.get(), MobRenderers.RollerRenderer::new);
        event.registerEntityRenderer(ModEntities.GOLDEN_SCARAB.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "golden_scarab", 0.45f, false, true));
        event.registerEntityRenderer(ModEntities.TWIGWALKER.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "twigwalker", 0.6f, false, false));
        event.registerEntityRenderer(ModEntities.BLOOMMANTIS.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "bloommantis", 0.5f, true, false));
        event.registerEntityRenderer(ModEntities.LEAFCUTTER.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "leafcutter", 0.4f, false, false));
        event.registerEntityRenderer(ModEntities.LANTERN_MOTH.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "lantern_moth", 0.3f, true, true));
        event.registerEntityRenderer(ModEntities.ROLLED_BOULDER.get(), MobRenderers.BoulderRenderer::new);
        event.registerEntityRenderer(ModEntities.TERROR_OF_THE_BOG.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "terror_of_the_bog", 1.6f, false, true));
        event.registerEntityRenderer(ModEntities.WAXEN_QUEEN.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "waxen_queen", 1.4f, true, true));
        event.registerEntityRenderer(ModEntities.DOZEN_LEGGED_WRATH.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, new MobRenderers.WrathModel(), 1.2f, false, true));
        event.registerEntityRenderer(ModEntities.APEX_MANTIS.get(), ctx -> new MobRenderers.BugRenderer<>(ctx, "apex_mantis", 1.3f, true, true));
        event.registerEntityRenderer(ModEntities.BOSS_GLOB.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.6f, false));
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (BoatWood w : BoatWood.values()) {
            event.registerLayerDefinition(ExofallBoatRenderer.layer(w, false), BoatModel::createBodyModel);
            event.registerLayerDefinition(ExofallBoatRenderer.layer(w, true), ChestBoatModel::createBodyModel);
        }
    }

    private ExofallClient() {}
}
