package com.bromax.exofall.compat.jei;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.crafting.ShellrockRecipe;
import com.bromax.exofall.crafting.ShellrockTableMenu;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI page for the Shellrock Crafting Table's 5x5 recipes, with the "+" button filling the table from your inventory.
 * The table is also listed as a place to do normal crafting. Only loaded when JEI is installed.
 */
@JeiPlugin
public class ExofallJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<ShellrockRecipe>> SHELLROCK =
            RecipeType.createRecipeHolderType(Exofall.id("shellrock_crafting"));

    @Override
    public ResourceLocation getPluginUid() {
        return Exofall.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new Category(reg.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        reg.addRecipes(SHELLROCK, level.getRecipeManager().getAllRecipesFor(ModRecipes.SHELLROCK_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        ItemStack table = new ItemStack(ModBlocks.SHELLROCK_CRAFTING_TABLE.get());
        reg.addRecipeCatalyst(table, SHELLROCK);
        reg.addRecipeCatalyst(table, RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(ShellrockTableMenu.class, ModRecipes.SHELLROCK_TABLE_MENU.get(), SHELLROCK,
                ShellrockTableMenu.GRID_START, ShellrockTableMenu.GRID * ShellrockTableMenu.GRID,
                ShellrockTableMenu.INV_START, 36);
    }

    /** 5x5 grid, an arrow, the result. Every grid cell gets a slot (empty ones too) so "+" maps cell for cell. */
    static final class Category implements IRecipeCategory<RecipeHolder<ShellrockRecipe>> {
        private static final int W = 150, H = 92, CELL = 18, OUT_X = 128, OUT_Y = 37;
        private final IDrawable icon;
        private final IDrawableStatic arrow;

        Category(IGuiHelper gui) {
            this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.SHELLROCK_CRAFTING_TABLE.get()));
            this.arrow = gui.getRecipeArrow();
        }

        @Override public RecipeType<RecipeHolder<ShellrockRecipe>> getRecipeType() { return SHELLROCK; }
        @Override public Component getTitle() { return Component.translatable("block.exofall.shellrock_crafting_table"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return W; }
        @Override public int getHeight() { return H; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<ShellrockRecipe> holder, IFocusGroup focuses) {
            ShellrockRecipe r = holder.value();
            int g = ShellrockTableMenu.GRID;
            for (int y = 0; y < g; y++) {
                for (int x = 0; x < g; x++) {
                    var slot = b.addSlot(RecipeIngredientRole.INPUT, 1 + x * CELL, 1 + y * CELL).setStandardSlotBackground();
                    if (x < r.width() && y < r.height()) {
                        var ing = r.ingredients().get(x + y * r.width());
                        if (!ing.isEmpty()) slot.addIngredients(ing);
                    }
                }
            }
            b.addSlot(RecipeIngredientRole.OUTPUT, OUT_X, OUT_Y).setOutputSlotBackground().addItemStack(r.getResultItem(null));
        }

        @Override
        public void draw(RecipeHolder<ShellrockRecipe> r, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
            arrow.draw(g, 96, 37);
        }
    }
}
