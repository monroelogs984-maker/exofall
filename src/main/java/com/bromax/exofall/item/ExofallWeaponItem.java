package com.bromax.exofall.item;

import com.bromax.exofall.effect.BleedingEffect;
import com.bromax.exofall.entity.boss.ExofallBoss;
import com.bromax.exofall.registry.ModEffects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.Tags;

/** Exofall's legendary boss weapons. Each kind has one on-hit effect; lifesteal lives in {@link WeaponEvents}. */
public class ExofallWeaponItem extends SwordItem {
    public enum Kind { QUADBLADE, GRANDHAMMER, SCYTHE, LONGSWORD }

    /** Glasswing Scythe launch: upward speed for a ~2 block rise, and its cooldown. */
    public static final double LAUNCH_SPEED = 0.54;
    public static final int LAUNCH_COOLDOWN = 80;
    /** Royal Grandhammer: share of damage dealt that heals the wielder. */
    public static final float LIFESTEAL = 0.10F;

    public final Kind kind;

    public ExofallWeaponItem(Kind kind, Properties props) {
        super(ModTiers.METEORIC, props);
        this.kind = kind;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            switch (kind) {
                case QUADBLADE -> target.addEffect(new MobEffectInstance(ModEffects.SANDBOUND, 60, 0), attacker);
                case LONGSWORD -> BleedingEffect.addStack(target, attacker);
                case SCYTHE -> launch(target, attacker);
                case GRANDHAMMER -> { }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private void launch(LivingEntity target, LivingEntity attacker) {
        if (target instanceof ExofallBoss || target.getType().is(Tags.EntityTypes.BOSSES)) return;
        if (attacker instanceof Player p) {
            if (p.getCooldowns().isOnCooldown(this)) return;
            p.getCooldowns().addCooldown(this, LAUNCH_COOLDOWN);
        }
        Vec3 v = target.getDeltaMovement();
        target.setDeltaMovement(v.x, Math.max(v.y, LAUNCH_SPEED), v.z);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (kind == Kind.GRANDHAMMER && ability == ItemAbilities.SWORD_SWEEP) return false;
        return super.canPerformAction(stack, ability);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.exofall." + kindId() + ".desc").withStyle(ChatFormatting.GRAY));
    }

    private String kindId() {
        return switch (kind) {
            case QUADBLADE -> "quadblade_of_the_sands";
            case GRANDHAMMER -> "royal_grandhammer";
            case SCYTHE -> "glasswing_scythe";
            case LONGSWORD -> "serrated_longsword";
        };
    }
}
