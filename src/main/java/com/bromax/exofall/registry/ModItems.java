package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.entity.BoatWood;
import com.bromax.exofall.item.ExofallBoatItem;
import com.bromax.exofall.item.ModTiers;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items. Every block gets an item automatically; all of them show in the Exofall creative tab. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Exofall.MOD_ID);

    /** Crust-breaker pickaxe: the only tool that drops meteor crust. Name and recipe are placeholders. */
    public static final DeferredItem<PickaxeItem> PLACEHOLDER_CRUSTBREAKER = ITEMS.register("placeholder_crustbreaker",
            () -> new PickaxeItem(ModTiers.METEORIC, new Item.Properties()
                    .fireResistant()
                    .attributes(PickaxeItem.createAttributes(ModTiers.METEORIC, 1.0F, -2.8F))));

    static {
        // Block items, in registration order (stones, woods, plants).
        for (DeferredHolder<Block, ? extends Block> holder : ModBlocks.BLOCKS.getEntries()) {
            String id = holder.getId().getPath();
            ITEMS.register(id, () -> {
                Block b = holder.get();
                if (b instanceof DoorBlock || b instanceof DoublePlantBlock) return new DoubleHighBlockItem(b, new Item.Properties());
                if (b instanceof WaterlilyBlock) return new PlaceOnWaterBlockItem(b, new Item.Properties());
                return new net.minecraft.world.item.BlockItem(b, new Item.Properties());
            });
        }
    }

    /** Boats and chest boats per alien wood, keyed by item id. */
    public static final Map<String, DeferredItem<ExofallBoatItem>> BOATS = new LinkedHashMap<>();

    static {
        for (BoatWood w : BoatWood.values()) {
            for (boolean chest : new boolean[] {false, true}) {
                String id = w.name + (chest ? "_chest_boat" : "_boat");
                BOATS.put(id, ITEMS.register(id, () -> new ExofallBoatItem(w, chest, new Item.Properties().stacksTo(1))));
            }
        }
    }

    private ModItems() {}
}
