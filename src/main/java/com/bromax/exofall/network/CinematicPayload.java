package com.bromax.exofall.network;

import com.bromax.exofall.Exofall;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: play the intro cinematic of boss {@code entityId} for {@code ticks}. */
public record CinematicPayload(int entityId, int ticks) implements CustomPacketPayload {
    public static final Type<CinematicPayload> TYPE = new Type<>(Exofall.id("boss_cinematic"));
    public static final StreamCodec<ByteBuf, CinematicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CinematicPayload::entityId, ByteBufCodecs.VAR_INT, CinematicPayload::ticks, CinematicPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
