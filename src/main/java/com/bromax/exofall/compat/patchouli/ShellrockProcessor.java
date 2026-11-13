package com.bromax.exofall.compat.patchouli;

import com.bromax.exofall.crafting.ShellrockRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

/**
 * Field Guide page for a Shellrock Crafting Table recipe (template "exofall:shellrock"): reads the recipe named by the
 * page's "recipe" field and fills the 5x5 grid (s0..s24, the recipe centred), the result ("out") and the title.
 * Only loaded by Patchouli, so Exofall runs without it.
 */
public class ShellrockProcessor implements IComponentProcessor {
    private ShellrockRecipe recipe;

    @Override
    public void setup(Level level, IVariableProvider vars) {
        String id = vars.get("recipe", level.registryAccess()).asString();
        recipe = level.getRecipeManager().byKey(ResourceLocation.parse(id))
                .map(h -> h.value() instanceof ShellrockRecipe r ? r : null).orElse(null);
    }

    @Override
    public IVariable process(Level level, String key) {
        var reg = level.registryAccess();
        if (recipe == null) return key.equals("title") ? IVariable.wrap("Missing recipe", reg) : IVariable.from(ItemStack.EMPTY, reg);
        if (key.equals("out")) return IVariable.from(recipe.getResultItem(reg), reg);
        if (key.equals("title")) return IVariable.wrap(recipe.getResultItem(reg).getHoverName().getString(), reg);
        if (key.startsWith("s")) {
            int i = Integer.parseInt(key.substring(1)), gx = i % 5, gy = i / 5;
            int ox = (5 - recipe.width()) / 2, oy = (5 - recipe.height()) / 2;
            int x = gx - ox, y = gy - oy;
            if (x < 0 || y < 0 || x >= recipe.width() || y >= recipe.height()) return IVariable.from(ItemStack.EMPTY, reg);
            Ingredient ing = recipe.ingredients().get(x + y * recipe.width());
            return ing.isEmpty() ? IVariable.from(ItemStack.EMPTY, reg) : IVariable.from(ing, reg);
        }
        return null;
    }
}
