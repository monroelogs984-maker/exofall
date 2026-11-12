package com.bromax.exofall.block;

import com.bromax.exofall.entity.mob.Bug;
import com.bromax.exofall.registry.ModBlockEntities;
import com.bromax.exofall.registry.ModEntities;
import com.bromax.exofall.world.MeteorStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Supplier;

/**
 * Spawning for {@link NestBlock}: every 10-25 s while a player is within 16 blocks, 1-2 of the meteor's bugs crawl
 * out onto free floor within 4 blocks. Stops while 6 bugs are already round it. Not in Peaceful.
 */
public class NestBlockEntity extends BlockEntity {
    private static final int RANGE = 16, CAP = 6, SPREAD = 4;
    private int delay = 40;

    private record Entry(Supplier<? extends EntityType<? extends Mob>> type, int weight) {}

    private static List<Entry> pool(MeteorStyle st) {
        return switch (st) {
            case SWAMP -> List.of(new Entry(ModEntities.BOGSPINNER, 30), new Entry(ModEntities.GLASSWING, 20),
                    new Entry(ModEntities.BLOODGNAT, 15), new Entry(ModEntities.REEDMANTIS, 15));
            case HIVE -> List.of(new Entry(ModEntities.COMBWASP, 30), new Entry(ModEntities.STINGER, 20),
                    new Entry(ModEntities.PROPOLIS_SPITTER, 20), new Entry(ModEntities.HONEYGUARD, 15), new Entry(ModEntities.WAXMITE, 15));
            case DESERT -> List.of(new Entry(ModEntities.TOMB_SCORPION, 25), new Entry(ModEntities.SANDDIVER, 20),
                    new Entry(ModEntities.SHIELDBACK, 20), new Entry(ModEntities.ANTLION, 15));
            case RAINFOREST -> List.of(new Entry(ModEntities.POUNCER, 25), new Entry(ModEntities.CANOPY_ASSASSIN, 20),
                    new Entry(ModEntities.TWIGWALKER, 20), new Entry(ModEntities.BLOOMMANTIS, 15), new Entry(ModEntities.LANTERN_MOTH, 10));
        };
    }

    public NestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEST.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NestBlockEntity be) {
        if (--be.delay > 0 || !(level instanceof ServerLevel server) || !(state.getBlock() instanceof NestBlock nest)) return;
        RandomSource r = level.random;
        be.delay = 40;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RANGE, EntitySelector.NO_SPECTATORS) == null) return;
        if (level.getEntitiesOfClass(Bug.class, new AABB(pos).inflate(10)).size() >= CAP) return;
        be.delay = 200 + r.nextInt(300);
        int count = 1 + r.nextInt(2), spawned = 0;
        for (int i = 0; i < count; i++) if (spawnOne(server, pos, pool(nest.style()), r)) spawned++;
        if (spawned > 0) {
            level.levelEvent(2004, pos, 0);
            level.playSound(null, pos, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.6F);
            be.setChanged();
        } else {
            be.delay = 60; // no room yet; look again soon
        }
    }

    /** One weighted-random bug on a free cell near the nest. Public for the dungeon self-test. */
    public static boolean spawnOne(ServerLevel level, BlockPos nest, List<Entry> pool, RandomSource r) {
        int total = 0;
        for (Entry e : pool) total += e.weight();
        int roll = r.nextInt(total);
        EntityType<? extends Mob> type = pool.get(0).type().get();
        for (Entry e : pool) {
            if ((roll -= e.weight()) < 0) { type = e.type().get(); break; }
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos p = nest.offset(r.nextInt(2 * SPREAD + 1) - SPREAD, r.nextInt(3) - 1, r.nextInt(2 * SPREAD + 1) - SPREAD);
            if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) continue;
            double x = p.getX() + 0.5, y = p.getY(), z = p.getZ() + 0.5;
            if (!level.noCollision(type.getSpawnAABB(x, y, z))) continue;
            Mob mob = type.create(level);
            if (mob == null) return false;
            mob.moveTo(x, y, z, r.nextFloat() * 360F, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.SPAWNER, null);
            level.addFreshEntityWithPassengers(mob);
            mob.spawnAnim();
            return true;
        }
        return false;
    }

    public static boolean spawnFor(ServerLevel level, BlockPos nest, MeteorStyle style) {
        return spawnOne(level, nest, pool(style), level.random);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        delay = tag.getInt("Delay");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putInt("Delay", delay);
    }
}
