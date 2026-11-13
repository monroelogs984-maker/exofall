package com.bromax.exofall.entity.mob.pet;

import com.bromax.exofall.item.BaubleSelfTest;
import com.bromax.exofall.registry.ModEntities;
import com.bromax.exofall.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * /exofall pettest: for each of the eight pair species on a pad 40 blocks up: tame two wild ones with treats (count the
 * treats it took), check a tamed one refuses to target a player, breed them, check the baby is tamed and half size,
 * and grow it up. Then a tamed Golden Scarab must drop nothing (no sigil farming).
 */
public final class PetSelfTest {
    public static List<String> run(ServerLevel level, BlockPos at) {
        List<String> out = new ArrayList<>(), bad = new ArrayList<>();
        // the world spawn's chunks are always entity-ticking, so the bugs can find each other there
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pad = new BlockPos(spawn.getX(), Math.max(at.getY(), spawn.getY()) + 40, spawn.getZ());
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            level.setBlockAndUpdate(pad.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(pad.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var fp = new BaubleSelfTest.TestPlayer(level);
        fp.setPos(pad.getX() + 4.5, pad.getY(), pad.getZ() + 0.5);
        level.addNewPlayer(fp);
        List<Mob> made = new ArrayList<>();
        try {
            for (PetPair pair : PetPair.values()) {
                for (EntityType<?> type : types(pair)) {
                    String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
                    Mob a = spawn(level, type, pad, 0, made), b = spawn(level, type, pad, 1, made);
                    int treats = tame(fp, a, pair) + tame(fp, b, pair);
                    boolean tamed = ((Pet) a).isTamedPet() && ((Pet) b).isTamedPet();
                    a.setTarget(fp);
                    boolean passive = a.getTarget() == null;
                    if (a instanceof Animal) {   // Mudgrub: vanilla animal breeding
                        out.add(name + ": tamed " + tamed + " with " + treats + " treats; vanilla breeding");
                        if (!tamed) bad.add(name + " not tamed");
                        continue;
                    }
                    feed(fp, a, pair);
                    feed(fp, b, pair);
                    int la = ((Pet) a).petState().love, lb = ((Pet) b).petState().love;
                    Mob baby = null;
                    for (int t = 0; t < 200 && baby == null; t++) {
                        tick(a);
                        tick(b);
                        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(pad).inflate(8), m -> m.getType() == type && m.isBaby())) baby = m;
                    }
                    String babyInfo = "no baby";
                    if (baby != null) {
                        made.add(baby);
                        float w = baby.getBbWidth() / a.getBbWidth();
                        boolean bt = ((Pet) baby).isTamedPet();
                        ((Pet) baby).petState().age = -1;
                        tick(baby);
                        babyInfo = String.format("baby tamed %s, width x%.2f, grows up %s", bt, w, !baby.isBaby());
                        if (!bt || Math.abs(w - 0.5) > 0.05 || baby.isBaby()) bad.add(name + " baby wrong: " + babyInfo);
                    } else bad.add(name + " didn't breed (love after feeding " + la + "/" + lb + ", after ticks " + ((Pet) a).petState().love
                            + "/" + ((Pet) b).petState().love + ", alive " + a.isAlive() + "/" + b.isAlive() + ", dist " + String.format("%.1f", a.distanceTo(b)) + ")");
                    out.add(name + ": tamed " + tamed + " with " + treats + " treats, passive " + passive + ", " + babyInfo);
                    if (!tamed) bad.add(name + " not tamed");
                    if (!passive) bad.add(name + " still targets players");
                    for (Mob m : made) m.discard();
                    made.clear();
                }
            }
            out.addAll(PetJobTest.run(level, pad, fp, bad));
            // tamed scarab: no drops
            Mob s = spawn(level, ModEntities.GOLDEN_SCARAB.get(), pad, 0, made);
            ((Pet) s).petState().tamed = true;
            s.hurt(level.damageSources().playerAttack(fp), 1000);
            int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pad).inflate(6)).size();
            out.add("tamed golden scarab killed by a player: " + drops + " item drops (want 0) "
                    + level.getEntitiesOfClass(ItemEntity.class, new AABB(pad).inflate(6)).stream().map(i -> i.getItem().toString()).toList());
            if (drops > 0) bad.add("tamed scarab dropped loot");
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pad).inflate(6)).forEach(ItemEntity::discard);
        } finally {
            for (Mob m : made) m.discard();
            fp.discard();
        }
        out.addAll(bad);
        out.add(bad.isEmpty() ? "PASS" : "FAIL (" + bad.size() + ")");
        return out;
    }

    private static List<EntityType<?>> types(PetPair p) {
        List<EntityType<?>> out = new ArrayList<>();
        for (var t : BuiltInRegistries.ENTITY_TYPE) if (p.has(t)) out.add(t);
        return out;
    }

    private static Mob spawn(ServerLevel level, EntityType<?> type, BlockPos pad, int dx, List<Mob> made) {
        Mob m = (Mob) type.create(level);
        m.moveTo(pad.getX() + 0.5 + dx, pad.getY(), pad.getZ() + 0.5, 0, 0);
        level.addFreshEntity(m);
        made.add(m);
        return m;
    }

    private static int tame(BaubleSelfTest.TestPlayer fp, Mob m, PetPair pair) {
        int used = 0;
        while (!((Pet) m).isTamedPet() && used < 50) {
            feed(fp, m, pair);
            used++;
        }
        return used;
    }

    private static void feed(BaubleSelfTest.TestPlayer fp, Mob m, PetPair pair) {
        fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(pair.treat(), 8));
        m.interact(fp, InteractionHand.MAIN_HAND);
    }

    private static void tick(Mob m) {
        m.tickCount++;
        m.tick();
    }

    private PetSelfTest() {}
}
