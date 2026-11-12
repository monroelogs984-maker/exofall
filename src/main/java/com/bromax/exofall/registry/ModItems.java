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
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
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
            if (id.equals("bogsilk_web") || id.equals("propolis")) continue;   // mob-made, no items
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

    // ---- Spawn eggs (colours: body, markings)
    public static final DeferredItem<DeferredSpawnEggItem> MUDGRUB_SPAWN_EGG = ITEMS.register("mudgrub_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MUDGRUB, 0xC8B083, 0x4A3424, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GLASSWING_SPAWN_EGG = ITEMS.register("glasswing_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GLASSWING, 0x2E8A80, 0x8FF0D8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BLOODGNAT_SPAWN_EGG = ITEMS.register("bloodgnat_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLOODGNAT, 0x2A2224, 0x8E1F24, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> DIVEBELL_BEETLE_SPAWN_EGG = ITEMS.register("divebell_beetle_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DIVEBELL_BEETLE, 0x1F3A2C, 0xC9B13A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> REEDMANTIS_SPAWN_EGG = ITEMS.register("reedmantis_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.REEDMANTIS, 0x7D8A3A, 0xB7B56A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> COMBWASP_SPAWN_EGG = ITEMS.register("combwasp_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.COMBWASP, 0x2A1E14, 0xD89A2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> STINGER_SPAWN_EGG = ITEMS.register("stinger_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.STINGER, 0x2A1410, 0xB8562A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> NURSEMAID_SPAWN_EGG = ITEMS.register("nursemaid_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.NURSEMAID, 0xF2E6C8, 0xE8C070, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> WAXMITE_SPAWN_EGG = ITEMS.register("waxmite_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WAXMITE, 0xEFE6CF, 0xC89A40, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LARVA_COCOON_SPAWN_EGG = ITEMS.register("larva_cocoon_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LARVA_COCOON, 0xECE2C6, 0xFF9A2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SANDDIVER_SPAWN_EGG = ITEMS.register("sanddiver_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SANDDIVER, 0xD8BF8A, 0x8A6A3E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SHIELDBACK_SPAWN_EGG = ITEMS.register("shieldback_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SHIELDBACK, 0x8A3A22, 0xB08A4A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> EMBER_BEETLE_SPAWN_EGG = ITEMS.register("ember_beetle_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.EMBER_BEETLE, 0x2A2422, 0xFF8A2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ROLLER_SPAWN_EGG = ITEMS.register("roller_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ROLLER, 0x1E2A3E, 0x8A6A42, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOLDEN_SCARAB_SPAWN_EGG = ITEMS.register("golden_scarab_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOLDEN_SCARAB, 0xE0B040, 0x38D0C0, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> TWIGWALKER_SPAWN_EGG = ITEMS.register("twigwalker_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TWIGWALKER, 0x6A5A48, 0x58C8C0, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BLOOMMANTIS_SPAWN_EGG = ITEMS.register("bloommantis_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLOOMMANTIS, 0xF0D8E4, 0xE04A8A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LEAFCUTTER_SPAWN_EGG = ITEMS.register("leafcutter_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LEAFCUTTER, 0x9A3A1E, 0x5AB048, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LANTERN_MOTH_SPAWN_EGG = ITEMS.register("lantern_moth_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LANTERN_MOTH, 0x5A6478, 0x7FF0E8, new Item.Properties()));

    /** Divebell Beetle drop. Placeholder use: 30 s of Water Breathing. */
    public static final DeferredItem<com.bromax.exofall.item.BubbleSacItem> BUBBLE_SAC = ITEMS.register("bubble_sac",
            () -> new com.bromax.exofall.item.BubbleSacItem(new Item.Properties().stacksTo(16)));

    // ---- Boss arenas: one sigil per meteor. The desert's drops whole from the Gold-Encrusted Scarab; the
    // others are crafted from 4 fragments that each meteor's toughest lesser mob drops.
    public static final Map<com.bromax.exofall.world.MeteorStyle, DeferredItem<com.bromax.exofall.item.SigilItem>> SIGILS = new java.util.EnumMap<>(com.bromax.exofall.world.MeteorStyle.class);
    public static final Map<com.bromax.exofall.world.MeteorStyle, DeferredItem<Item>> SIGIL_FRAGMENTS = new java.util.EnumMap<>(com.bromax.exofall.world.MeteorStyle.class);

    static {
        for (com.bromax.exofall.world.MeteorStyle st : com.bromax.exofall.world.MeteorStyle.values()) {
            SIGILS.put(st, ITEMS.register(st.getSerializedName() + "_sigil", () -> new com.bromax.exofall.item.SigilItem(st,
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE).fireResistant())));
            if (st != com.bromax.exofall.world.MeteorStyle.DESERT) {
                SIGIL_FRAGMENTS.put(st, ITEMS.register(st.getSerializedName() + "_sigil_fragment",
                        () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON))));
            }
        }
    }

    public static final DeferredItem<DeferredSpawnEggItem> BOGSPINNER_SPAWN_EGG = ITEMS.register("bogspinner_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BOGSPINNER, 0x4E4630, 0xD8D0A0, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MIRE_LEECH_SPAWN_EGG = ITEMS.register("mire_leech_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MIRE_LEECH, 0x2E3622, 0xC8742A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HONEYGUARD_SPAWN_EGG = ITEMS.register("honeyguard_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HONEYGUARD, 0xC8862A, 0xF0C860, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> PROPOLIS_SPITTER_SPAWN_EGG = ITEMS.register("propolis_spitter_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PROPOLIS_SPITTER, 0x24160C, 0xFFAE2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ANTLION_SPAWN_EGG = ITEMS.register("antlion_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ANTLION, 0x7A6650, 0x2E2014, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> TOMB_SCORPION_SPAWN_EGG = ITEMS.register("tomb_scorpion_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TOMB_SCORPION, 0x2A2420, 0xC8962E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CANOPY_ASSASSIN_SPAWN_EGG = ITEMS.register("canopy_assassin_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CANOPY_ASSASSIN, 0x2A3A1E, 0xC0302A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> POUNCER_SPAWN_EGG = ITEMS.register("pouncer_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.POUNCER, 0x1A2A2A, 0x2EC46C, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> TERROR_OF_THE_BOG_SPAWN_EGG = ITEMS.register("terror_of_the_bog_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TERROR_OF_THE_BOG, 0x4A3A22, 0x8A9A3A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> WAXEN_QUEEN_SPAWN_EGG = ITEMS.register("waxen_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WAXEN_QUEEN, 0xE8C070, 0x3A2414, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DOZEN_LEGGED_WRATH_SPAWN_EGG = ITEMS.register("dozen_legged_wrath_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DOZEN_LEGGED_WRATH, 0xB0281C, 0x2A0E0A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> APEX_MANTIS_SPAWN_EGG = ITEMS.register("apex_mantis_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.APEX_MANTIS, 0x5AA040, 0xD8E87A, new Item.Properties()));


    // ---- Boss drops: materials for each meteor's boss gear (crafted in the Shellrock Crafting Table)
    private static DeferredItem<Item> bossDrop(String id, net.minecraft.world.item.Rarity rarity) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().rarity(rarity).fireResistant()));
    }

    // Terror of the Bog
    public static final DeferredItem<Item> LETHAL_PROBOSCIS = bossDrop("lethal_proboscis", net.minecraft.world.item.Rarity.RARE);
    public static final DeferredItem<Item> BOG_EYE = bossDrop("bog_eye", net.minecraft.world.item.Rarity.RARE);
    // Waxen Queen
    public static final DeferredItem<Item> ROYAL_HONEY_DROP = bossDrop("royal_honey_drop", net.minecraft.world.item.Rarity.UNCOMMON);
    public static final DeferredItem<Item> CRYSTALLIZED_WING = bossDrop("crystallized_wing", net.minecraft.world.item.Rarity.RARE);
    // Dozen-legged Wrath
    public static final DeferredItem<Item> TERROR_LEG = bossDrop("terror_leg", net.minecraft.world.item.Rarity.UNCOMMON);
    public static final DeferredItem<Item> VENOMJAW = bossDrop("venomjaw", net.minecraft.world.item.Rarity.RARE);
    // Apex Mantis
    public static final DeferredItem<Item> MANTIS_EYE = bossDrop("mantis_eye", net.minecraft.world.item.Rarity.RARE);
    /** Deliberately weak (8 damage, 1.3 speed): it's the base for a greater weapon, not an end in itself. */
    public static final DeferredItem<net.minecraft.world.item.SwordItem> MANTIS_GREATCLAW = ITEMS.register("mantis_greatclaw",
            () -> new net.minecraft.world.item.SwordItem(ModTiers.METEORIC, new Item.Properties()
                    .rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()
                    .attributes(weapon(8.0, 1.3))));

    // ---- Legendary boss weapons (3D models; uncraftable for now, Shellrock Crafting Table recipes come later)
    private static DeferredItem<com.bromax.exofall.item.ExofallWeaponItem> legendary(String id,
            com.bromax.exofall.item.ExofallWeaponItem.Kind kind, double damage, double speed) {
        return ITEMS.register(id, () -> new com.bromax.exofall.item.ExofallWeaponItem(kind, new Item.Properties()
                .rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant().attributes(weapon(damage, speed))));
    }

    public static final DeferredItem<com.bromax.exofall.item.ExofallWeaponItem> QUADBLADE_OF_THE_SANDS =
            legendary("quadblade_of_the_sands", com.bromax.exofall.item.ExofallWeaponItem.Kind.QUADBLADE, 15.0, 0.7);
    public static final DeferredItem<com.bromax.exofall.item.ExofallWeaponItem> ROYAL_GRANDHAMMER =
            legendary("royal_grandhammer", com.bromax.exofall.item.ExofallWeaponItem.Kind.GRANDHAMMER, 20.0, 0.5);
    public static final DeferredItem<com.bromax.exofall.item.ExofallWeaponItem> GLASSWING_SCYTHE =
            legendary("glasswing_scythe", com.bromax.exofall.item.ExofallWeaponItem.Kind.SCYTHE, 11.0, 1.2);
    public static final DeferredItem<com.bromax.exofall.item.ExofallWeaponItem> SERRATED_LONGSWORD =
            legendary("serrated_longsword", com.bromax.exofall.item.ExofallWeaponItem.Kind.LONGSWORD, 9.0, 1.7);

    // ---- Baubles (Curios; uncraftable for now)
    private static DeferredItem<com.bromax.exofall.item.BaubleItem> bauble(com.bromax.exofall.item.BaubleItem.Kind kind) {
        return ITEMS.register(kind.name().toLowerCase(java.util.Locale.ROOT), () -> new com.bromax.exofall.item.BaubleItem(kind,
                new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    }

    public static final DeferredItem<com.bromax.exofall.item.BaubleItem> DIVEBELL_LOCKET = bauble(com.bromax.exofall.item.BaubleItem.Kind.DIVEBELL_LOCKET);
    public static final DeferredItem<com.bromax.exofall.item.BaubleItem> EMBER_BEETLE_BROOCH = bauble(com.bromax.exofall.item.BaubleItem.Kind.EMBER_BEETLE_BROOCH);
    public static final DeferredItem<com.bromax.exofall.item.BaubleItem> LEAFCUTTER_CHARM = bauble(com.bromax.exofall.item.BaubleItem.Kind.LEAFCUTTER_CHARM);
    public static final DeferredItem<com.bromax.exofall.item.BaubleItem> STINGER_FANG_RING = bauble(com.bromax.exofall.item.BaubleItem.Kind.STINGER_FANG_RING);
    public static final DeferredItem<com.bromax.exofall.item.BaubleItem> NURSEMAIDS_WAX_CHARM = bauble(com.bromax.exofall.item.BaubleItem.Kind.NURSEMAIDS_WAX_CHARM);
    public static final DeferredItem<com.bromax.exofall.item.BaubleItem> LANTERN_MOTH_PENDANT = bauble(com.bromax.exofall.item.BaubleItem.Kind.LANTERN_MOTH_PENDANT);

    /** Main-hand modifiers for a weapon that shows {@code damage} attack damage and {@code speed} attack speed. */
    private static net.minecraft.world.item.component.ItemAttributeModifiers weapon(double damage, double speed) {
        var hand = net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND;
        var add = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage - 1.0, add), hand)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                        new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed - 4.0, add), hand)
                .build();
    }

    private ModItems() {}
}
