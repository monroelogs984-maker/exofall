package com.bromax.exofall.entity.mob;

import com.bromax.exofall.entity.mob.desert.Antlion;
import com.bromax.exofall.entity.mob.desert.TombScorpion;
import com.bromax.exofall.entity.mob.hive.Honeyguard;
import com.bromax.exofall.entity.mob.rainforest.CanopyAssassin;
import com.bromax.exofall.entity.mob.rainforest.Pouncer;
import com.bromax.exofall.entity.mob.swamp.Bogspinner;
import com.bromax.exofall.entity.mob.swamp.MireLeech;
import com.bromax.exofall.item.BaubleSelfTest;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModEffects;
import com.bromax.exofall.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dev check for the 8 dungeon-era mobs: builds a small test pad 40 blocks above the caller, runs each
 * mechanic by ticking the mobs by hand against a damageable fake player, then clears the pad.
 */
public final class MobSelfTest {
    public static List<String> run(ServerLevel level, BlockPos at) {
        List<String> out = new ArrayList<>();
        BlockPos o = at.above(40);
        clear(level, o);
        Player fp = new BaubleSelfTest.TestPlayer(level);
        fp.setPos(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5);
        level.addNewPlayer((net.minecraft.server.level.ServerPlayer) fp);   // mobs look players up in the level
        try {
            // silk + propolis globs
            floor(level, o, Blocks.STONE.defaultBlockState(), 0);
            SpitGlob silk = SpitGlob.silk(fp);
            silk.setPos(o.getX() + 0.5, o.getY() + 3, o.getZ() + 0.5);
            silk.setDeltaMovement(0, -0.6, 0);
            level.addFreshEntity(silk);
            tick(silk, fp, 30);
            out.add("silk glob: web placed " + level.getBlockState(o.above()).is(ModBlocks.BOGSILK_WEB.get()) + ", glob gone " + silk.isRemoved());
            clearAbove(level, o);
            SpitGlob prop = SpitGlob.propolis(fp);
            prop.setPos(o.getX() + 0.5, o.getY() + 3, o.getZ() + 0.5);
            prop.setDeltaMovement(0, -0.6, 0);
            level.addFreshEntity(prop);
            tick(prop, fp, 30);
            int puddles = 0;
            for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, 1, -1), o.offset(1, 1, 1))) if (level.getBlockState(p).is(ModBlocks.PROPOLIS.get())) puddles++;
            out.add("propolis glob: puddles " + puddles + "/9 (centre always, the rest ~55%)");
            clearAbove(level, o);

            // Bogspinner: +50% on webbed targets
            Bogspinner sp = spawn(level, ModEntities.BOGSPINNER.get(), o.offset(0, 1, 0));
            fp.setPos(o.getX() + 1.5, o.getY() + 1, o.getZ() + 0.5);
            float h0 = hit(fp, () -> sp.doHurtTarget(fp));
            level.setBlockAndUpdate(fp.blockPosition(), ModBlocks.BOGSILK_WEB.get().defaultBlockState());
            float h1 = hit(fp, () -> sp.doHurtTarget(fp));
            out.add("bogspinner bite: open " + h0 + ", webbed " + h1 + " (want x1.5)");
            sp.discard();
            clearAbove(level, o);

            // Mire Leech: latch in water, drain, drop off when the host leaves the water
            for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, 1, -2), o.offset(2, 2, 2))) level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
            fp.setPos(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5);
            MireLeech le = spawn(level, ModEntities.MIRE_LEECH.get(), o.offset(1, 1, 0));
            fp.setHealth(20);
            ((BaubleSelfTest.TestPlayer) fp).refreshFluids();
            le.doHurtTarget(fp);
            boolean latched = le.isLatched();
            tick(le, fp, 81);
            float drained = 20 - fp.getHealth();
            clearAbove(level, o);
            ((BaubleSelfTest.TestPlayer) fp).refreshFluids();
            tick(le, fp, 12);
            out.add("mire leech: latched " + latched + ", drained " + drained + " in 4 s (drain 6 every 2 s, from 20 hp), let go on dry land " + !le.isLatched());
            le.discard();

            // Honeyguard: front hits do 15% and wear the shield; a 20-damage blow breaks it
            Honeyguard hg = spawn(level, ModEntities.HONEYGUARD.get(), o.offset(0, 1, 0));
            hg.setYRot(0);
            hg.yBodyRot = 0;
            fp.setPos(o.getX() + 0.5, o.getY() + 1, o.getZ() + 2.5);
            float g0 = hg.getHealth();
            hg.invulnerableTime = 0;
            hg.hurt(level.damageSources().playerAttack(fp), 8);
            float g1 = hg.getHealth();
            boolean shieldAfter8 = hg.hasShield();
            hg.invulnerableTime = 0;
            hg.hurt(level.damageSources().playerAttack(fp), 30);
            out.add("honeyguard: front 8 -> lost " + (g0 - g1) + " (15% of 8, then its armour), shield kept " + shieldAfter8 + "; 30 (heavy) blow -> shield broken " + !hg.hasShield());
            hg.discard();

            // Antlion: digs a funnel on sand and drags nearby mobs in
            for (int y = 1; y <= 3; y++) floor(level, o, Blocks.SAND.defaultBlockState(), y);
            Antlion al = spawn(level, ModEntities.ANTLION.get(), o.offset(0, 4, 0));
            tick(al, fp, 20);
            boolean pit = al.inPit();
            boolean hole = level.getBlockState(o.offset(0, 3, 0)).isAir() && level.getBlockState(o.offset(0, 2, 0)).isAir();
            Mob pig = spawn(level, EntityType.PIG, o.offset(3, 4, 0));
            double d0 = pig.distanceTo(al);
            for (int i = 0; i < 20; i++) { al.tickCount++; al.tick(); pig.tickCount++; pig.tick(); }
            out.add("antlion: in pit " + pit + ", funnel dug " + hole + ", pig dragged " + String.format("%.2f -> %.2f", d0, pig.distanceTo(al)));
            al.discard();
            pig.discard();
            clearAbove(level, o);

            // Tomb Scorpion: tail sting from 3 blocks applies Sandbound
            TombScorpion ts = spawn(level, ModEntities.TOMB_SCORPION.get(), o.offset(0, 1, 0));
            fp.setPos(o.getX() + 0.5, o.getY() + 1, o.getZ() + 3.5);
            fp.setHealth(20);
            ts.setTarget(fp);
            tick(ts, fp, 60);
            out.add("tomb scorpion: target hp " + fp.getHealth() + ", sandbound " + fp.hasEffect(ModEffects.SANDBOUND));
            ts.discard();
            fp.removeAllEffects();

            // Canopy Assassin: clings under a ceiling, drops on a player below
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) level.setBlockAndUpdate(o.offset(x, 5, z), Blocks.STONE.defaultBlockState());
            CanopyAssassin ca = spawn(level, ModEntities.CANOPY_ASSASSIN.get(), o.offset(0, 1, 0));
            boolean clung = ca.isClinging();
            double hangY = ca.getY() - o.getY();
            fp.setPos(o.getX() + 0.5, o.getY() + 1, o.getZ() + 0.5);
            fp.setHealth(20);
            tick(ca, fp, 40);
            out.add("canopy assassin: clinging " + clung + String.format(" at +%.1f", hangY) + ", target hp " + fp.getHealth()
                    + ", bleeding " + fp.hasEffect(ModEffects.BLEEDING) + ", on ground " + !ca.isClinging());
            ca.discard();
            fp.removeAllEffects();
            clearAbove(level, o);

            // Pouncer: crouch, leap, land on the target
            Pouncer pc = spawn(level, ModEntities.POUNCER.get(), o.offset(0, 1, 0));
            fp.setPos(o.getX() + 0.5, o.getY() + 1, o.getZ() + 5.5);
            fp.setHealth(20);
            pc.setTarget(fp);
            tick(pc, fp, 60);
            out.add("pouncer: target hp " + fp.getHealth() + String.format(", ended %.1f blocks from target", pc.distanceTo(fp)));
            pc.discard();
        } finally {
            level.removePlayerImmediately((net.minecraft.server.level.ServerPlayer) fp, Entity.RemovalReason.DISCARDED);
            clear(level, o);
        }
        return out;
    }

    private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, BlockPos p) {
        T m = type.create(level);
        m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.COMMAND, null);
        level.addFreshEntity(m);
        return m;
    }

    private static void tick(Entity e, Player fp, int n) {
        for (int i = 0; i < n && !e.isRemoved(); i++) {
            fp.invulnerableTime = 0;
            e.tickCount++;   // the level does this before each tick; goals run on alternate ticks
            e.tick();
        }
    }

    private static float hit(Player fp, Runnable r) {
        fp.setHealth(20);
        fp.invulnerableTime = 0;
        r.run();
        return 20 - fp.getHealth();
    }

    private static void floor(ServerLevel level, BlockPos o, BlockState s, int y) {
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) level.setBlockAndUpdate(o.offset(x, y, z), s);
    }

    private static void clearAbove(ServerLevel level, BlockPos o) {
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-5, 1, -5), o.offset(5, 8, 5))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
    }

    private static void clear(ServerLevel level, BlockPos o) {
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-5, 0, -5), o.offset(5, 8, 5))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
    }

    private MobSelfTest() {}
}
