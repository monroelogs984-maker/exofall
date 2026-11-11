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
