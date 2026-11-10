package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.item.ModTiers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Materials, the crust-breaker pickaxe, legendaries and armour get registered here. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Exofall.MOD_ID);

    public static final DeferredItem<?> PLACEHOLDER_CRUST = ITEMS.registerSimpleBlockItem(ModBlocks.PLACEHOLDER_CRUST);
    public static final DeferredItem<?> PLACEHOLDER_BOG_PLANT = ITEMS.registerSimpleBlockItem(ModBlocks.PLACEHOLDER_BOG_PLANT);

    /** Crust-breaker pickaxe: the only tool that drops meteor crust. Name and recipe are placeholders. */
    public static final DeferredItem<PickaxeItem> PLACEHOLDER_CRUSTBREAKER = ITEMS.register("placeholder_crustbreaker",
            () -> new PickaxeItem(ModTiers.METEORIC, new Item.Properties()
                    .fireResistant()
                    .attributes(PickaxeItem.createAttributes(ModTiers.METEORIC, 1.0F, -2.8F))));

    private ModItems() {}
}
