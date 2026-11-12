package com.bromax.exofall.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** /exofall armortest: every set's stats and perks, on AI-less zombies on a pad 40 blocks up. */
public final class ArmorSelfTest {
    public static List<String> run(ServerLevel level, BlockPos at) {
        List<String> out = new ArrayList<>();
        BlockPos pad = at.above(40);
        for (int x = -8; x <= 8; x++) for (int z = -3; z <= 3; z++) level.setBlockAndUpdate(pad.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        List<Zombie> made = new ArrayList<>();
        try {
            for (ArmorSet s : ArmorSet.values()) {
                Zombie z = zombie(level, pad, made, 0);
                wear(z, s, 4);
                out.add(String.format("%s: armour %.0f, toughness %.1f, KB res %.2f, speed x%.3f, helmet durability %d", s.id,
                        z.getAttributeValue(Attributes.ARMOR), z.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
                        z.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),
                        z.getAttributeValue(Attributes.MOVEMENT_SPEED) / z.getAttributeBaseValue(Attributes.MOVEMENT_SPEED),
                        s.pieces.get(ArmorItem.Type.HELMET).get().getDefaultInstance().getMaxDamage()));
                z.discard();
            }
            // Honey: full set only
            Zombie h = zombie(level, pad, made, 0);
            wear(h, ArmorSet.CRYSTALLIZED_HONEY, 4);
            ArmorEvents.honey(h);
            double fast = h.getAttributeValue(Attributes.MOVEMENT_SPEED) / h.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            // zombies are undead (no Regeneration), so the regen half runs on a pig
            var pig = EntityType.PIG.create(level);
            pig.setNoAi(true);
            pig.moveTo(pad.getX() + 0.5, pad.getY(), pad.getZ() + 2.5, 0, 0);
            level.addFreshEntity(pig);
            for (ArmorItem.Type t : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS})
                pig.setItemSlot(t.getSlot(), new ItemStack(ArmorSet.CRYSTALLIZED_HONEY.pieces.get(t).get()));
            ArmorEvents.honey(pig);
            boolean regen = pig.hasEffect(MobEffects.REGENERATION);
            pig.discard();
            h.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            tick(h);
            ArmorEvents.honey(h);
            double three = h.getAttributeValue(Attributes.MOVEMENT_SPEED) / h.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            out.add(String.format("honey: full set speed x%.2f regen %s; 3 pieces speed x%.2f", fast, regen, three));

            // Carapace: projectile vs melee, same amount
            float melee = loss(level, pad, made, ArmorSet.REINFORCED_CARAPACE, level.damageSources().source(DamageTypes.MOB_ATTACK));
            float shot = loss(level, pad, made, ArmorSet.REINFORCED_CARAPACE, level.damageSources().source(DamageTypes.MOB_PROJECTILE));
            out.add(String.format("carapace: 10 melee -> %.2f lost, 10 projectile -> %.2f lost (ratio %.2f, want 0.60)", melee, shot, shot / melee));

            // Mireweave: noticed at 12 blocks with a 16 range? and ambush damage
            Zombie seer = zombie(level, pad, made, -6), hidden = zombie(level, pad, made, 6);
            TargetingConditions cond = TargetingConditions.forCombat().range(16).ignoreLineOfSight();
            boolean before = cond.test(seer, hidden);
            wear(hidden, ArmorSet.MIREWEAVE, 4);
            boolean after = cond.test(seer, hidden);
            out.add("mireweave: seen at 12 of 16 blocks: bare " + before + ", full set " + after + " (want true, false)");
            float plain = ambush(level, pad, made, 0), sneaky = ambush(level, pad, made, 4);
            out.add(String.format("mireweave ambush: 10 dmg on an unaware zombie bare -> %.2f, full set -> %.2f (ratio %.2f, want 1.30)", plain, sneaky, sneaky / plain));
        } finally {
            for (Zombie z : made) z.discard();
        }
        return out;
    }

    private static Zombie zombie(ServerLevel level, BlockPos pad, List<Zombie> made, int dx) {
        Zombie z = EntityType.ZOMBIE.create(level);
        z.setNoAi(true);
        z.moveTo(pad.getX() + 0.5 + dx, pad.getY(), pad.getZ() + 0.5, 0, 0);
        level.addFreshEntity(z);
        made.add(z);
        return z;
    }

    private static void wear(Zombie z, ArmorSet s, int n) {
        ArmorItem.Type[] order = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        for (int i = 0; i < n; i++) z.setItemSlot(order[i].getSlot(), new ItemStack(s.pieces.get(order[i]).get()));
        tick(z);
    }

    private static void tick(Zombie z) {
        z.tickCount++;
        z.tick();
    }

    private static float loss(ServerLevel level, BlockPos pad, List<Zombie> made, ArmorSet s, DamageSource src) {
        Zombie z = zombie(level, pad, made, 2);
        wear(z, s, 4);
        z.setHealth(20);
        z.invulnerableTime = 0;
        z.hurt(src, 10);
        float l = 20 - z.getHealth();
        z.discard();
        return l;
    }

    private static float ambush(ServerLevel level, BlockPos pad, List<Zombie> made, int pieces) {
        Zombie attacker = zombie(level, pad, made, -3), victim = zombie(level, pad, made, 3);
        if (pieces > 0) wear(attacker, ArmorSet.MIREWEAVE, pieces);
        victim.setHealth(20);
        victim.invulnerableTime = 0;
        victim.hurt(level.damageSources().mobAttack(attacker), 10);
        float l = 20 - victim.getHealth();
        attacker.discard();
        victim.discard();
        return l;
    }

    private ArmorSelfTest() {}
}
