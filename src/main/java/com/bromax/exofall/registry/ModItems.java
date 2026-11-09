package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Materials, the crust-breaker pickaxe, legendaries and armour get registered here. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Exofall.MOD_ID);

    public static final DeferredItem<?> PLACEHOLDER_CRUST = ITEMS.registerSimpleBlockItem(ModBlocks.PLACEHOLDER_CRUST);
    public static final DeferredItem<?> PLACEHOLDER_BOG_PLANT = ITEMS.registerSimpleBlockItem(ModBlocks.PLACEHOLDER_BOG_PLANT);

    private ModItems() {}
}
