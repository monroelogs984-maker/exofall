package com.bromax.exofall.entity.mob.rainforest;

import com.bromax.exofall.entity.mob.BugSounds;
import com.bromax.exofall.entity.mob.FlyingBug;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Harmless glowing moth. Flies to any player holding a light (torch, lantern, glowing block) and circles it, shedding glow dust. */
public class LanternMoth extends FlyingBug {
    private static final BugSounds SOUNDS = new BugSounds(null, SoundEvents.BAT_HURT, SoundEvents.BAT_DEATH, null, 1.4f, 0.5f);

    public LanternMoth(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return flyingAttrs(8, 0, 0, 0.5);
    }

    static boolean isLight(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().getLightEmission() > 0;
    }

    static boolean holdsLight(Player p) {
        return isLight(p.getMainHandItem()) || isLight(p.getOffhandItem());
    }

    @Override
    protected BugSounds sounds() {
        return SOUNDS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SeekLightGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 15 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.4, getZ(), 1, 0.3, 0.2, 0.3, 0);
        }
    }

    class SeekLightGoal extends Goal {
        @Nullable private Player player;
        private float angle;
        private int searchDelay;

        SeekLightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (--searchDelay > 0) return false;
            searchDelay = 5;
            player = level().getNearestPlayer(getX(), getY(), getZ(), 24,
                    e -> EntitySelector.NO_SPECTATORS.test(e) && e instanceof Player p && holdsLight(p));
            return player != null;
        }

        @Override
        public boolean canContinueToUse() {
            return player != null && player.isAlive() && holdsLight(player) && distanceToSqr(player) < 32 * 32;
        }

        @Override
        public void start() {
            angle = random.nextFloat() * 6.28f;
        }

        @Override
        public void tick() {
            if (player == null) return;
            angle += 0.24f;   // ticked every other tick
            Vec3 c = player.getEyePosition();
            getMoveControl().setWantedPosition(c.x + Math.cos(angle) * 2.5, c.y + Math.sin(angle * 0.7) * 0.6, c.z + Math.sin(angle) * 2.5, 1.0);
        }
    }
}
