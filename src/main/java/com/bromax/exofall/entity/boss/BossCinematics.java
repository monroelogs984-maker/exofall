package com.bromax.exofall.entity.boss;

import com.bromax.exofall.Exofall;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server side of the boss intro cinematic: players watching it can't be hurt. */
@EventBusSubscriber(modid = Exofall.MOD_ID)
public final class BossCinematics {
    private static final Map<UUID, Long> WATCHING = new ConcurrentHashMap<>();

    public static void begin(ServerPlayer player, int ticks) {
        WATCHING.put(player.getUUID(), player.level().getGameTime() + ticks + 5);
    }

    public static boolean watching(Player player) {
        Long until = WATCHING.get(player.getUUID());
        if (until == null) return false;
        if (player.level().getGameTime() > until) {
            WATCHING.remove(player.getUUID());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && watching(p)) event.setCanceled(true);
    }

    private BossCinematics() {}
}
