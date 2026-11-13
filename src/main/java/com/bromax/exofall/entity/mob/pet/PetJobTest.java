package com.bromax.exofall.entity.mob.pet;

import com.bromax.exofall.config.ExofallConfig;
import com.bromax.exofall.item.ArmorSet;
import com.bromax.exofall.item.BaubleSelfTest;
import com.bromax.exofall.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The pet jobs half of /exofall pettest: owner + modes, tamed stats, bug armour on/off + perks, follow-teleport,
 * friendly fire, fighters joining the owner's fights, Shieldback taunt, mantis first strike, Nursemaid heal, Mudgrub
 * and Golden Scarab digs, scarab ore sense, golems leaving pets alone.
 */
final class PetJobTest {
    static List<String> run(ServerLevel level, BlockPos pad, BaubleSelfTest.TestPlayer fp, List<String> bad) {
        List<String> out = new ArrayList<>();
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        try {
            // ---- owner, modes, stats, armour, on every species
            for (PetPair pair : PetPair.values()) for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if (!pair.has(type)) continue;
                String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
                Mob m = mob(level, type, pad, 0, made);
                double wildHp = m.getMaxHealth();
                Pets.tame(m, (Pet) m, fp);
                PetState st = ((Pet) m).petState();
                boolean owner = fp.getUUID().equals(st.owner);
                double hpX = m.getMaxHealth() / wildHp;
                fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                StringBuilder modes = new StringBuilder(st.mode.name());
                for (int i = 0; i < 3; i++) {
                    m.interact(fp, InteractionHand.MAIN_HAND);
                    modes.append('>').append(st.mode.name());
                }
                double bare = m.getAttributeValue(Attributes.ARMOR);
                fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ArmorSet.ANCIENTSTONE.bugArmour.get()));
                m.interact(fp, InteractionHand.MAIN_HAND);
                tick(m, 2);   // equipment changes apply on the next tick
                double worn = m.getAttributeValue(Attributes.ARMOR), kb = m.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                fp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
                m.interact(fp, InteractionHand.MAIN_HAND);
                tick(m, 2);
                boolean off = m.getItemBySlot(EquipmentSlot.BODY).isEmpty() && fp.getInventory().countItem(ArmorSet.ANCIENTSTONE.bugArmour.get()) > 0;
                fp.getInventory().clearContent();
                double after = m.getAttributeValue(Attributes.ARMOR);
                out.add(String.format("%s: owner %s, hp x%.1f, modes %s, armour %.0f>%.0f>%.0f (kb %.1f), shears return it %s",
                        name, owner, hpX, modes, bare, worn, after, kb, off));
                if (!owner) bad.add(name + " has no owner after taming");
                // wild health includes the meteor-mob multiplier, which taming swaps for the pet one
                double wantX = ExofallConfig.PET_HEALTH_MULTIPLIER.get() / ExofallConfig.MOB_HEALTH_MULTIPLIER.get();
                if (Math.abs(hpX - wantX) > 0.01) bad.add(name + " tamed health x" + hpX + " (want x" + wantX + ")");
                if (!modes.toString().equals("FOLLOW>STAY>WANDER>FOLLOW")) bad.add(name + " modes " + modes);
                if (worn < bare + 8 || Math.abs(after - bare) > 0.01 || !off) bad.add(name + " armour on/off wrong");
                if (kb < 0.49) bad.add(name + " ancientstone knockback " + kb);
                m.discard();
            }

            // ---- follow teleport (Glasswing flies, Shieldback walks)
            for (EntityType<?> type : List.of(ModEntities.GLASSWING.get(), ModEntities.SHIELDBACK.get())) {
                Mob m = tamed(level, type, pad, 0, fp, made);
                fp.setPos(pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5);
                m.moveTo(pad.getX() + 0.5, pad.getY() + 40, pad.getZ() + 0.5);
                tick(m, 25);
                double d = m.distanceTo(fp);
                out.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + " 40 blocks away teleports to its owner: " + String.format("%.1f", d) + " blocks after");
                if (d > 5) bad.add(type + " didn't teleport (" + d + ")");
                m.discard();
            }
            fp.setPos(pad.getX() + 4.5, pad.getY(), pad.getZ() + 0.5);

            // ---- friendly fire
            Mob wasp = tamed(level, ModEntities.COMBWASP.get(), pad, 0, fp, made);
            float hp = wasp.getHealth();
            wasp.hurt(level.damageSources().playerAttack(fp), 6);
            out.add("owner hits own pet: " + (hp - wasp.getHealth()) + " damage (want 0)");
            if (wasp.getHealth() < hp) bad.add("owner can hurt own pet");

            // ---- fighters join the owner's fights
            Zombie z = zombie(level, pad, 4, made);
            fp.tickCount += 5;
            fp.setLastHurtByMob(z);
            tick(wasp, 4);
            out.add("zombie hits the owner, the Combwasp targets it: " + (wasp.getTarget() == z));
            if (wasp.getTarget() != z) bad.add("fighter didn't defend its owner");
            wasp.setTarget(null);
            Zombie z2 = zombie(level, pad, -4, made);
            fp.tickCount += 5;
            fp.setLastHurtMob(z2);
            tick(wasp, 4);
            out.add("owner hits a zombie, the Combwasp targets it: " + (wasp.getTarget() == z2));
            if (wasp.getTarget() != z2) bad.add("fighter didn't join its owner's attack");
            Pets.setMode(wasp, wasp instanceof Pet p ? p.petState() : null, PetState.Mode.STAY);
            fp.tickCount += 5;
            fp.setLastHurtMob(z);
            tick(wasp, 4);
            out.add("staying Combwasp ignores the owner's fight: " + (wasp.getTarget() == null));
            if (wasp.getTarget() != null) bad.add("staying pet still fights");
            wasp.discard();

            // ---- Shieldback taunt
            Mob shield = tamed(level, ModEntities.SHIELDBACK.get(), pad, 0, fp, made);
            z.setTarget(fp);
            tick(shield, ExofallConfig.PET_TAUNT_INTERVAL.get() + 2);
            out.add("zombie after the owner switches to the Shieldback: " + (z.getTarget() == shield));
            if (z.getTarget() != shield) bad.add("Shieldback taunt didn't pull the zombie");
            shield.discard();

            // ---- mantis first strike
            Mob mantis = tamed(level, ModEntities.REEDMANTIS.get(), pad, 0, fp, made);
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            z.setHealth(z.getMaxHealth());
            float h0 = z.getHealth();
            mantis.doHurtTarget(z);
            float first = h0 - z.getHealth();
            z.invulnerableTime = 0;
            z.setHealth(z.getMaxHealth());
            mantis.doHurtTarget(z);
            float second = z.getMaxHealth() - z.getHealth();
            out.add(String.format("tamed Reedmantis: first hit %.1f, second %.1f (first should be x%.1f)", first, second, ExofallConfig.PET_AMBUSH_MULTIPLIER.get()));
            if (second <= 0 || Math.abs(first / second - ExofallConfig.PET_AMBUSH_MULTIPLIER.get()) > 0.2) bad.add("mantis first strike wrong");
            mantis.discard();

            // ---- Reinforced Carapace: projectile resistance
            Mob carapace = tamed(level, ModEntities.SHIELDBACK.get(), pad, 0, fp, made);
            Arrow arrow = new Arrow(EntityType.ARROW, level);
            made.add(arrow);
            float bareHit = hit(carapace, level.damageSources().arrow(arrow, null), 4);
            carapace.setItemSlot(EquipmentSlot.BODY, new ItemStack(ArmorSet.REINFORCED_CARAPACE.bugArmour.get()));
            tick(carapace, 2);
            float armHit = hit(carapace, level.damageSources().arrow(arrow, null), 4);
            carapace.setItemSlot(EquipmentSlot.BODY, new ItemStack(ArmorSet.ANCIENTSTONE.bugArmour.get()));
            tick(carapace, 2);
            float ancHit = hit(carapace, level.damageSources().arrow(arrow, null), 4);
            out.add(String.format("arrow on a Shieldback: bare %.2f, Ancientstone %.2f, Carapace %.2f (Carapace should be lowest)", bareHit, ancHit, armHit));
            if (!(armHit < ancHit)) bad.add("carapace projectile perk not working");
            carapace.discard();

            // ---- Nursemaid heals the owner
            Mob nurse = tamed(level, ModEntities.NURSEMAID.get(), pad, 0, fp, made);
            nurse.moveTo(fp.getX(), fp.getY() + 2.5, fp.getZ());
            fp.setHealth(10);
            tick(nurse, ExofallConfig.PET_HEAL_INTERVAL.get() * 3 + 30);
            out.add("Nursemaid next to a hurt owner: owner health 10 -> " + fp.getHealth());
            if (fp.getHealth() <= 10) bad.add("Nursemaid didn't heal her owner");
            fp.setHealth(fp.getMaxHealth());
            nurse.discard();

            // ---- digs
            for (EntityType<?> type : List.of(ModEntities.MUDGRUB.get(), ModEntities.GOLDEN_SCARAB.get())) {
                BlockPos under = pad.offset(-3, -1, -3);
                level.setBlockAndUpdate(under, Blocks.MUD.defaultBlockState());
                Mob m = tamed(level, type, pad, 0, fp, made);
                m.moveTo(under.getX() + 0.5, under.getY() + 1, under.getZ() + 0.5);
                PetState st = ((Pet) m).petState();
                Pets.setMode(m, st, PetState.Mode.STAY);
                st.jobTimer = 10;
                st.nuggetTimer = 10;
                tick(m, 20);
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(under).inflate(3));
                String got = items.stream().map(i -> i.getItem().getCount() + " " + BuiltInRegistries.ITEM.getKey(i.getItem().getItem()).getPath()).toList().toString();
                out.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + " dig: " + got + " (on ground " + m.onGround()
                        + ", timers " + st.jobTimer + "/" + st.nuggetTimer + ", running " + m.goalSelector.getAvailableGoals().stream()
                        .filter(w -> w.isRunning()).map(w -> w.getGoal().getClass().getSimpleName()).toList() + ")");
                if (items.isEmpty()) bad.add(type + " dug nothing");
                items.forEach(ItemEntity::discard);
                level.setBlockAndUpdate(under, Blocks.STONE.defaultBlockState());
                m.discard();
            }

            // ---- scarab senses ore (gold beats a nearer iron)
            Mob scarab = tamed(level, ModEntities.GOLDEN_SCARAB.get(), pad, 0, fp, made);
            BlockPos iron = pad.offset(0, -3, 2), gold = pad.offset(5, -4, -4);
            level.setBlockAndUpdate(iron, Blocks.IRON_ORE.defaultBlockState());
            level.setBlockAndUpdate(gold, Blocks.GOLD_ORE.defaultBlockState());
            BlockPos found = new PetGoals.Upkeep((net.minecraft.world.entity.PathfinderMob) scarab, (Pet) scarab, PetRole.GOLDEN_SCARAB).sense(level, fp);
            out.add("Golden Scarab senses: " + found + " (gold at " + gold + ", " + PetGoals.Upkeep.direction(5, -4, -4).getString() + ")");
            if (!gold.equals(found)) bad.add("scarab didn't find the gold ore");
            level.setBlockAndUpdate(iron, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(gold, Blocks.STONE.defaultBlockState());

            // ---- golems leave pets alone
            var golem = EntityType.IRON_GOLEM.create(level);
            golem.moveTo(pad.getX() + 2.5, pad.getY(), pad.getZ() + 0.5);
            level.addFreshEntity(golem);
            made.add(golem);
            Mob tamedWasp = tamed(level, ModEntities.COMBWASP.get(), pad, 0, fp, made);
            golem.setTarget(tamedWasp);
            out.add("iron golem told to target a tamed Combwasp: " + (golem.getTarget() == null ? "refuses" : "targets it"));
            if (golem.getTarget() != null) bad.add("golem targets pets");
        } finally {
            made.forEach(net.minecraft.world.entity.Entity::discard);
        }
        return out;
    }

    private static float hit(Mob m, DamageSource src, float amount) {
        m.setHealth(m.getMaxHealth());
        m.invulnerableTime = 0;
        m.hurt(src, amount);
        float d = m.getMaxHealth() - m.getHealth();
        m.setHealth(m.getMaxHealth());
        return d;
    }

    private static Zombie zombie(ServerLevel level, BlockPos pad, int dx, List<net.minecraft.world.entity.Entity> made) {
        Zombie z = EntityType.ZOMBIE.create(level);
        z.moveTo(pad.getX() + 0.5 + dx, pad.getY(), pad.getZ() + 2.5);
        z.setNoAi(true);
        level.addFreshEntity(z);
        made.add(z);
        return z;
    }

    private static Mob mob(ServerLevel level, EntityType<?> type, BlockPos pad, int dx, List<net.minecraft.world.entity.Entity> made) {
        Mob m = (Mob) type.create(level);
        m.moveTo(pad.getX() + 0.5 + dx, pad.getY(), pad.getZ() + 0.5, 0, 0);
        level.addFreshEntity(m);
        made.add(m);
        return m;
    }

    private static Mob tamed(ServerLevel level, EntityType<?> type, BlockPos pad, int dx, BaubleSelfTest.TestPlayer fp, List<net.minecraft.world.entity.Entity> made) {
        Mob m = mob(level, type, pad, dx, made);
        Pets.tame(m, (Pet) m, fp);
        return m;
    }

    private static void tick(Mob m, int n) {
        for (int i = 0; i < n; i++) {
            m.tickCount++;
            m.tick();
        }
    }

    private PetJobTest() {}
}
