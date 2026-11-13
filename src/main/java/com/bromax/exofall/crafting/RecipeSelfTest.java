package com.bromax.exofall.crafting;

import com.bromax.exofall.registry.ModItems;
import com.bromax.exofall.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * /exofall recipetest: every Shellrock table recipe is laid out in a 5x5 grid and looked up through the recipe manager
 * (must find itself, nothing else, and craft its result); every meteor gear item must have a recipe; and the eight
 * dungeon-mob drops are rolled 1000 times each from their loot tables for a player kill.
 */
public final class RecipeSelfTest {
    public static List<String> run(ServerLevel level, BlockPos at) {
        List<String> out = new ArrayList<>(), bad = new ArrayList<>();
        var rm = level.getRecipeManager();
        var all = rm.getAllRecipesFor(ModRecipes.SHELLROCK_TYPE.get());
        Set<String> crafted = new HashSet<>();
        for (var holder : all) {
            ShellrockRecipe r = holder.value();
            NonNullList<ItemStack> grid = NonNullList.withSize(25, ItemStack.EMPTY);
            for (int y = 0; y < r.height(); y++)
                for (int x = 0; x < r.width(); x++) {
                    var ing = r.ingredients().get(x + y * r.width());
                    if (!ing.isEmpty()) grid.set(x + y * 5, ing.getItems()[0].copy());
                }
            CraftingInput in = CraftingInput.ofPositioned(5, 5, grid).input();
            var found = rm.getRecipeFor(ModRecipes.SHELLROCK_TYPE.get(), in, level);
            String id = holder.id().toString();
            if (found.isEmpty()) bad.add(id + ": grid matches nothing");
            else if (!found.get().id().equals(holder.id())) bad.add(id + ": grid crafts " + found.get().id() + " instead");
            else {
                ItemStack res = found.get().value().assemble(in, level.registryAccess());
                crafted.add(BuiltInRegistries.ITEM.getKey(res.getItem()).getPath());
            }
            // the vanilla 3x3 table must not also craft it
            if (r.width() <= 3 && r.height() <= 3) {
                var vanilla = rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, level);
                if (vanilla.isPresent()) bad.add(id + ": also a vanilla recipe " + vanilla.get().id());
            }
        }
        List<String> want = new ArrayList<>();
        for (var s : com.bromax.exofall.item.ArmorSet.values()) for (var p : s.pieces.values()) want.add(BuiltInRegistries.ITEM.getKey(p.get()).getPath());
        for (var item : List.of(ModItems.QUADBLADE_OF_THE_SANDS, ModItems.ROYAL_GRANDHAMMER, ModItems.GLASSWING_SCYTHE, ModItems.SERRATED_LONGSWORD,
                ModItems.DIVEBELL_LOCKET, ModItems.EMBER_BEETLE_BROOCH, ModItems.LEAFCUTTER_CHARM, ModItems.STINGER_FANG_RING,
                ModItems.NURSEMAIDS_WAX_CHARM, ModItems.LANTERN_MOTH_PENDANT)) want.add(BuiltInRegistries.ITEM.getKey(item.get()).getPath());
        for (String w : want) if (!crafted.contains(w)) bad.add(w + " has no working recipe");
        out.add(all.size() + " shellrock recipes, " + crafted.size() + " craft correctly; " + want.size() + " gear items checked");
        var crust = rm.byKey(com.bromax.exofall.Exofall.id("crustbreaker"));
        out.add("crustbreaker recipe " + (crust.isPresent() ? "loaded" : "MISSING"));
        if (crust.isEmpty()) bad.add("crustbreaker recipe missing");

        // drop rates
        var fp = new com.bromax.exofall.item.BaubleSelfTest.TestPlayer(level);
        var mobs = List.<EntityType<?>>of(com.bromax.exofall.registry.ModEntities.BOGSPINNER.get(), com.bromax.exofall.registry.ModEntities.MIRE_LEECH.get(),
                com.bromax.exofall.registry.ModEntities.HONEYGUARD.get(), com.bromax.exofall.registry.ModEntities.PROPOLIS_SPITTER.get(),
                com.bromax.exofall.registry.ModEntities.ANTLION.get(), com.bromax.exofall.registry.ModEntities.TOMB_SCORPION.get(),
                com.bromax.exofall.registry.ModEntities.CANOPY_ASSASSIN.get(), com.bromax.exofall.registry.ModEntities.POUNCER.get());
        var drops = List.of(ModItems.BOGSILK_SPOOL, ModItems.LEECH_FANG, ModItems.HONEYGUARD_PLATE, ModItems.PROPOLIS_GLAND,
                ModItems.ANTLION_MANDIBLE, ModItems.TOMB_STINGER, ModItems.ASSASSIN_HOOK, ModItems.POUNCER_SHELL);
        StringBuilder rates = new StringBuilder("drop rate per player kill:");
        for (int i = 0; i < mobs.size(); i++) {
            var e = mobs.get(i).create(level);
            if (!(e instanceof LivingEntity mob)) continue;
            mob.setPos(at.getX(), at.getY(), at.getZ());
            var table = level.getServer().reloadableRegistries().getLootTable(mob.getLootTable());
            int hits = 0;
            for (int k = 0; k < 1000; k++) {
                var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, mob)
                        .withParameter(LootContextParams.ORIGIN, mob.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(fp))
                        .withParameter(LootContextParams.ATTACKING_ENTITY, fp).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, fp)
                        .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fp).create(LootContextParamSets.ENTITY);
                for (ItemStack s : table.getRandomItems(params)) if (s.is(drops.get(i).get())) hits++;
            }
            rates.append(' ').append(BuiltInRegistries.ITEM.getKey(drops.get(i).get()).getPath()).append(' ').append(hits / 10).append('%');
            if (hits < 250 || hits > 420) bad.add(drops.get(i).getId().getPath() + " drop rate off: " + hits / 10.0 + "%");
        }
        out.add(rates.toString());
        out.addAll(bad);
        out.add(bad.isEmpty() ? "PASS" : "FAIL (" + bad.size() + ")");
        return out;
    }

    private RecipeSelfTest() {}
}
