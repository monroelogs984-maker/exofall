package com.bromax.exofall.item;

import com.bromax.exofall.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Dev check for the baubles (equips each on a fake player and pokes the hook it relies on). */
public final class BaubleSelfTest {
    public static List<String> run(ServerLevel level, BlockPos at) {
        List<String> out = new ArrayList<>();
        FakePlayer fp = new TestPlayer(level);
        fp.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        var inv = CuriosApi.getCuriosInventory(fp);
        if (inv.isEmpty()) {
            out.add("no curios inventory on the fake player");
            return out;
        }
        out.add("slots: " + inv.get().getCurios().keySet());

        equip(fp, "necklace", ModItems.DIVEBELL_LOCKET.get());
        var breathe = new LivingBreatheEvent(fp, false, 1, 0);
        NeoForge.EVENT_BUS.post(breathe);
        out.add("divebell: wearing " + BaubleEvents.wearing(fp, ModItems.DIVEBELL_LOCKET.get()) + ", can breathe " + breathe.canBreathe()
                + ", submerged mining " + fp.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SUBMERGED_MINING_SPEED));

        equip(fp, "body", ModItems.EMBER_BEETLE_BROOCH.get());
        fp.setHealth(20);
        fp.invulnerableTime = 0;
        fp.hurt(level.damageSources().inFire(), 4);
        out.add("ember: 4 fire damage -> health " + fp.getHealth());

        equip(fp, "charm", ModItems.LEAFCUTTER_CHARM.get());
        fp.setHealth(20);
        fp.invulnerableTime = 0;
        fp.hurt(level.damageSources().sweetBerryBush(), 1);
        var web = net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState();
        var leaves = net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState();
        var cut = new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(fp, leaves, 1F, fp.blockPosition());
        NeoForge.EVENT_BUS.post(cut);
        fp.fallDistance = 5;
        fp.makeStuckInBlock(web, new net.minecraft.world.phys.Vec3(0.25, 0.05, 0.25));
        out.add("leafcutter: berry bush 1 -> health " + fp.getHealth() + ", web passes " + BaubleEvents.leafcutterPasses(fp, web)
                + ", leaf break speed " + cut.getNewSpeed() + ", web ignored (fall distance kept) " + (fp.fallDistance == 5));

        equip(fp, "charm", ModItems.NURSEMAIDS_WAX_CHARM.get());
        fp.getFoodData().setFoodLevel(0);
        fp.getFoodData().setSaturation(0);
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(fp, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY));
        out.add("wax charm: bonus from bread (5 food) = food " + fp.getFoodData().getFoodLevel() + ", sat " + fp.getFoodData().getSaturationLevel());
        return out;
    }

    private static void equip(FakePlayer fp, String slot, Item item) {
        CuriosApi.getCuriosInventory(fp).flatMap(h -> h.getStacksHandler(slot))
                .ifPresent(s -> s.getStacks().setStackInSlot(0, new ItemStack(item)));
        // FakePlayer.tick() does nothing; Curios applies equip changes from the entity tick events
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(fp));
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(fp));
    }

    /** A fake player that can be hurt (NeoForge's ignores all damage). */
    private static final class TestPlayer extends FakePlayer {
        TestPlayer(ServerLevel level) {
            super(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("e0f0a11a-0000-4000-8000-0000000000b5"), "[ExofallTest]"));
            try {   // skip the 3 s post-spawn damage immunity (mojmap name; this is a dev-only test)
                var f = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
                f.setAccessible(true);
                f.setInt(this, 0);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
            return false;
        }
    }

    private BaubleSelfTest() {}
}
