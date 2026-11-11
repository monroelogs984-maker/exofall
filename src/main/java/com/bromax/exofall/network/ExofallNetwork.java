package com.bromax.exofall.network;

import com.bromax.exofall.Exofall;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class ExofallNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        // The handler body only runs on the client, so the client class is never loaded on a server.
        r.playToClient(CinematicPayload.TYPE, CinematicPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.bromax.exofall.client.BossCinematic.start(payload)));
    }

    private ExofallNetwork() {}
}
