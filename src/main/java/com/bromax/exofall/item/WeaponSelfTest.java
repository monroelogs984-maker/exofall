package com.bromax.exofall.item;

import com.bromax.exofall.registry.ModEffects;
import com.bromax.exofall.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Dev check for the weapons' on-hit effects (calls the hit hooks directly; vanilla attack maths is not under test). */
public final class WeaponSelfTest {
    public static List<String> run(ServerLevel level, BlockPos at) {
        List<String> out = new ArrayList<>();
        FakePlayer fp = FakePlayerFactory.getMinecraft(level);
        fp.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);

        for (var w : List.of(ModItems.QUADBLADE_OF_THE_SANDS, ModItems.ROYAL_GRANDHAMMER, ModItems.GLASSWING_SCYTHE, ModItems.SERRATED_LONGSWORD)) {
            ItemStack stack = new ItemStack(w.get());
            fp.setItemSlot(EquipmentSlot.MAINHAND, stack);
            double dmg = 1 + stack.getAttributeModifiers().modifiers().stream()
                    .filter(e -> e.attribute().equals(Attributes.ATTACK_DAMAGE)).mapToDouble(e -> e.modifier().amount()).sum();
            double spd = 4 + stack.getAttributeModifiers().modifiers().stream()
                    .filter(e -> e.attribute().equals(Attributes.ATTACK_SPEED)).mapToDouble(e -> e.modifier().amount()).sum();
            Husk t = EntityType.HUSK.create(level);
            t.setNoAi(true);
            t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            t.setHealth(200);
            t.setPos(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
            level.addFreshEntity(t);
            String name = w.getId().getPath();
            switch (w.get().kind) {
                case QUADBLADE -> {
                    w.get().hurtEnemy(stack, t, fp);
                    var e = t.getEffect(ModEffects.SANDBOUND);
                    out.add(name + " dmg " + dmg + " spd " + spd + ": sandbound " + (e == null ? "MISSING" : e.getDuration() + "t")
                            + ", speed " + String.format("%.4f -> %.4f", t.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(), t.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                }
                case GRANDHAMMER -> {
                    fp.setHealth(5);
                    t.hurt(level.damageSources().playerAttack(fp), 20);
                    out.add(name + " dmg " + dmg + " spd " + spd + ": husk hp " + t.getHealth() + ", wielder 5 -> " + fp.getHealth());
                    fp.setHealth(fp.getMaxHealth());
                }
                case SCYTHE -> {
                    w.get().hurtEnemy(stack, t, fp);
                    double vy1 = t.getDeltaMovement().y;
                    t.setDeltaMovement(0, 0, 0);
                    w.get().hurtEnemy(stack, t, fp);
                    double vy2 = t.getDeltaMovement().y;
                    out.add(name + " dmg " + dmg + " spd " + spd + ": launch vy " + vy1 + ", second hit (cooldown) vy " + vy2
                            + ", cooldown " + fp.getCooldowns().isOnCooldown(w.get()));
                }
                case LONGSWORD -> {
                    StringBuilder sb = new StringBuilder(name + " dmg " + dmg + " spd " + spd + ":");
                    float hp0 = t.getHealth();
                    for (int hit = 0; hit < 4; hit++) {
                        w.get().hurtEnemy(stack, t, fp);
                        for (int i = 0; i < 12; i++) t.tick();
                        var e = t.getEffect(ModEffects.BLEEDING);
                        sb.append(" hit").append(hit + 1).append(" stacks ").append(e == null ? 0 : e.getAmplifier() + 1)
                                .append(" dur ").append(e == null ? 0 : e.getDuration());
                    }
                    for (int i = 0; i < 80; i++) t.tick();
                    sb.append("; bled ").append(hp0 - t.getHealth()).append(" hp total, effect left: ").append(t.hasEffect(ModEffects.BLEEDING));
                    out.add(sb.toString());
                }
            }
            t.discard();
        }
        return out;
    }

    private WeaponSelfTest() {}
}
