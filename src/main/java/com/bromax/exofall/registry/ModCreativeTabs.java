package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Exofall.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.exofall"))
                    .icon(() -> new ItemStack(ModBlocks.SHELLROCK.get()))
                    .displayItems((params, output) -> ModItems.ITEMS.getEntries().stream()
                            .sorted(java.util.Comparator.comparingInt(ModCreativeTabs::group))   // stable: keeps registration order inside a group
                            .forEach(item -> output.accept(item.get())))
                    .build());

    /** Tab order: gear, crafting materials, treats and sigils, building blocks, plants, nests, spawn eggs, warded blocks. */
    private static int group(net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, ? extends net.minecraft.world.item.Item> h) {
        String id = h.getId().getPath();
        var item = h.get();
        if (id.startsWith("warded_")) return 9;
        if (id.endsWith("_spawn_egg")) return 8;
        if (id.equals("crustbreaker") || item instanceof net.minecraft.world.item.SwordItem) return 0;
        if (item instanceof net.minecraft.world.item.ArmorItem) return 1;
        if (item instanceof com.bromax.exofall.item.BaubleItem) return 2;
        if (item instanceof com.bromax.exofall.item.TreatItem || id.contains("sigil")) return 4;
        if (item instanceof net.minecraft.world.item.BlockItem bi) {
            var b = bi.getBlock();
            if (b instanceof com.bromax.exofall.block.NestBlock) return 7;
            if (b instanceof net.minecraft.world.level.block.BushBlock || b instanceof net.minecraft.world.level.block.LeavesBlock
                    || b instanceof net.minecraft.world.level.block.AmethystClusterBlock || id.contains("sapling")) return 6;
            return 5;
        }
        return 3; // drops, boats, bubble sac and other materials
    }

    private ModCreativeTabs() {}
}
