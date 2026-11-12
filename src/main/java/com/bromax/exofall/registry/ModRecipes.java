package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.crafting.ShellrockRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Shellrock Crafting Table's recipe type, serializer and menu. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Exofall.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Exofall.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Exofall.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ShellrockRecipe>> SHELLROCK_TYPE =
            TYPES.register("shellrock_crafting", () -> RecipeType.simple(Exofall.id("shellrock_crafting")));
    public static final DeferredHolder<RecipeSerializer<?>, ShellrockRecipe.Serializer> SHELLROCK_SERIALIZER =
            SERIALIZERS.register("shellrock_crafting", ShellrockRecipe.Serializer::new);
    public static final DeferredHolder<MenuType<?>, MenuType<com.bromax.exofall.crafting.ShellrockTableMenu>> SHELLROCK_TABLE_MENU =
            MENUS.register("shellrock_crafting_table", () -> new MenuType<>(com.bromax.exofall.crafting.ShellrockTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModRecipes() {}
}
