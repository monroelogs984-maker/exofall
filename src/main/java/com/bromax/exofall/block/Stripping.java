package com.bromax.exofall.block;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.WoodSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;

/** Axe-stripping for the alien logs and wood. */
public final class Stripping {
    private static Map<Block, Block> map;

    public static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) return;
        if (map == null) {
            Map<Block, Block> m = new HashMap<>();
            for (WoodSet w : ModBlocks.WOODS) {
                m.put(w.log.get(), w.strippedLog.get());
                m.put(w.wood.get(), w.strippedWood.get());
            }
            map = m;
        }
        Block to = map.get(event.getState().getBlock());
        if (to != null) {
            event.setFinalState(to.defaultBlockState().setValue(RotatedPillarBlock.AXIS, event.getState().getValue(RotatedPillarBlock.AXIS)));
        }
    }

    private Stripping() {}
}
