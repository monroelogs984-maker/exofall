package com.bromax.exofall.item;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The four meteor armour sets (DESIGN.md "Armour sets"). They differ in role, not in unlock order. Defence is per piece
 * (boots, leggings, chestplate, helmet); durability is a multiple of the per-slot base, Netherite = 37. Repaired with the
 * set's main dungeon-mob drop.
 */
public enum ArmorSet {
    /** Swamp stealth: mobs notice you at up to half range, ambush bonus. 20 armour / 2.5 toughness. */
    MIREWEAVE("mireweave", new int[]{3, 6, 8, 3}, 2.5F, 0.0F, 37, 15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> ModItems.BOGSILK_SPOOL.get()),
    /** Hive: full set gives +15% speed and constant Regeneration I. Diamond protection. */
    CRYSTALLIZED_HONEY("crystallized_honey", new int[]{3, 6, 8, 3}, 2.0F, 0.0F, 37, 18, SoundEvents.ARMOR_EQUIP_GOLD, () -> ModItems.PROPOLIS_GLAND.get()),
    /** Rainforest: projectile protection. 22 armour / 14 toughness. */
    REINFORCED_CARAPACE("reinforced_carapace", new int[]{4, 7, 8, 3}, 3.5F, 0.1F, 43, 15, SoundEvents.ARMOR_EQUIP_TURTLE, () -> ModItems.POUNCER_SHELL.get()),
    /** Desert tank: 22 armour / 16 toughness (the most), 0.8 knockback resistance, -2.5% speed per piece. */
    ANCIENTSTONE("ancientstone", new int[]{4, 7, 8, 3}, 4.0F, 0.2F, 74, 10, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> ModItems.TOMB_STINGER.get());

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Exofall.MOD_ID);

    public final String id;
    private final int[] defense;
    private final float toughness, knockback;
    private final int durability, enchant;
    private final Holder<SoundEvent> sound;
    private final Supplier<Item> repair;
    private DeferredHolder<ArmorMaterial, ArmorMaterial> material;
    public final Map<ArmorItem.Type, DeferredItem<ExofallArmorItem>> pieces = new EnumMap<>(ArmorItem.Type.class);
    /** Armour for a tamed bug (see {@link BugArmorItem}). */
    public DeferredItem<BugArmorItem> bugArmour;

    ArmorSet(String id, int[] defense, float toughness, float knockback, int durability, int enchant, Holder<SoundEvent> sound, Supplier<Item> repair) {
        this.id = id;
        this.defense = defense;
        this.toughness = toughness;
        this.knockback = knockback;
        this.durability = durability;
        this.enchant = enchant;
        this.sound = sound;
        this.repair = repair;
    }

    public Holder<ArmorMaterial> material() {
        return material;
    }

    /** Registers the materials and the 16 pieces; call from the mod constructor. */
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        for (ArmorSet s : values()) {
            s.material = MATERIALS.register(s.id, () -> new ArmorMaterial(
                    Map.of(ArmorItem.Type.BOOTS, s.defense[0], ArmorItem.Type.LEGGINGS, s.defense[1],
                            ArmorItem.Type.CHESTPLATE, s.defense[2], ArmorItem.Type.HELMET, s.defense[3], ArmorItem.Type.BODY, s.defense[2]),
                    s.enchant, s.sound, () -> Ingredient.of(s.repair.get()),
                    List.of(new ArmorMaterial.Layer(Exofall.id(s.id))), s.toughness, s.knockback));
            for (ArmorItem.Type t : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
                s.pieces.put(t, ModItems.ITEMS.register(s.id + "_" + t.getName(), () -> new ExofallArmorItem(s, t,
                        new Item.Properties().durability(t.getDurability(s.durability)).fireResistant()
                                .rarity(net.minecraft.world.item.Rarity.EPIC))));
            }
        }
        for (ArmorSet s : values()) {
            s.bugArmour = ModItems.ITEMS.register(s.id + "_bug_armour", () -> new BugArmorItem(s,
                    new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.RARE)));
        }
        MATERIALS.register(bus);
    }
}
