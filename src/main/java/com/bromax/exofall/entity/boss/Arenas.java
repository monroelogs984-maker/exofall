package com.bromax.exofall.entity.boss;

import com.bromax.exofall.block.SigilSocketBlock;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModEntities;
import com.bromax.exofall.world.ArenaLayout;
import com.bromax.exofall.world.MeteorStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Sigil socket logic: opening the seal and waking the boss. */
public final class Arenas {
    public static EntityType<? extends ExofallBoss> bossFor(MeteorStyle style) {
        return switch (style) {
            case SWAMP -> ModEntities.TERROR_OF_THE_BOG.get();
            case HIVE -> ModEntities.WAXEN_QUEEN.get();
            case DESERT -> ModEntities.DOZEN_LEGGED_WRATH.get();
            case RAINFOREST -> ModEntities.APEX_MANTIS.get();
        };
    }

    /**
     * Wakes the boss of the arena whose socket is at {@code socket}: opens the seal and puts the boss, dormant,
     * at the arena centre. Fails (with a message) if a boss is already there.
     */
    public static boolean awaken(ServerLevel level, BlockPos socket, BlockState state, @Nullable Player player) {
        Direction door = state.getValue(SigilSocketBlock.FACING);
        MeteorStyle kind = state.getValue(SigilSocketBlock.KIND);
        BlockPos centre = ArenaLayout.centreFromSocket(socket, door);
        if (!level.getEntitiesOfClass(ExofallBoss.class, ArenaLayout.bounds(centre).inflate(8)).isEmpty()) {
            if (player != null) player.displayClientMessage(Component.translatable("block.exofall.sigil_socket.awake"), true);
            return false;
        }
        ExofallBoss boss = bossFor(kind).create(level);
        if (boss == null) return false;
        float yaw = door.toYRot();
        boss.moveTo(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.TRIGGERED, null);
        boss.setHome(centre, door, true);
        level.addFreshEntity(boss);
        openSeal(level, centre, door);
        level.playSound(null, socket, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.5f, 0.7f);
        level.playSound(null, socket, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.5f);
        level.sendParticles(ParticleTypes.END_ROD, socket.getX() + 0.5, socket.getY() + 0.5, socket.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
        if (player != null) player.displayClientMessage(Component.translatable("block.exofall.sigil_socket.opened"), true);
        return true;
    }

    public static void openSeal(ServerLevel level, BlockPos centre, Direction door) {
        boolean any = false;
        for (BlockPos p : ArenaLayout.seal(centre, door)) {
            if (!level.getBlockState(p).is(ModBlocks.SIGIL_SEAL.get())) continue;
            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            level.sendParticles(ParticleTypes.PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.3);
            any = true;
        }
        if (any) {
            BlockPos mid = centre.relative(door, ArenaLayout.SEAL_A).above(1);
            level.playSound(null, mid, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 2f, 0.5f);
            level.playSound(null, mid, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2f, 0.5f);
        }
    }

    private Arenas() {}
}
