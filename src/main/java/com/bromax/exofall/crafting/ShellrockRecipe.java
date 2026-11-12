package com.bromax.exofall.crafting;

import com.bromax.exofall.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A shaped recipe up to 5x5, only craftable in the Shellrock Crafting Table. JSON:
 * <pre>{"type": "exofall:shellrock_crafting", "pattern": ["A A", " B "], "key": {"A": ..., "B": ...},
 *  "result": {"id": "...", "count": 1}}</pre>
 * Like vanilla shaped recipes the pattern may sit anywhere in the grid and may be mirrored left-right.
 * The table also crafts every vanilla crafting recipe.
 */
public record ShellrockRecipe(String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result,
                              List<String> pattern, Map<Character, Ingredient> key) implements Recipe<CraftingInput> {
    public static final int MAX = 5;

    @Override
    public boolean matches(CraftingInput in, Level level) {
        if (in.width() != width || in.height() != height || in.ingredientCount() != countFilled()) return false;
        return matches(in, false) || matches(in, true);
    }

    private int countFilled() {
        int n = 0;
        for (Ingredient i : ingredients) if (!i.isEmpty()) n++;
        return n;
    }

    private boolean matches(CraftingInput in, boolean mirror) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Ingredient want = ingredients.get((mirror ? width - 1 - x : x) + y * width);
                if (!want.test(in.getItem(x, y))) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w >= width && h >= height;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHELLROCK_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SHELLROCK_TYPE.get();
    }

    private static DataResult<ShellrockRecipe> build(String group, List<String> pattern, Map<Character, Ingredient> key, ItemStack result) {
        int h = pattern.size();
        if (h == 0 || h > MAX) return DataResult.error(() -> "pattern needs 1 to " + MAX + " rows");
        int w = pattern.get(0).length();
        if (w == 0 || w > MAX) return DataResult.error(() -> "pattern rows need 1 to " + MAX + " characters");
        NonNullList<Ingredient> ings = NonNullList.withSize(w * h, Ingredient.EMPTY);
        for (int y = 0; y < h; y++) {
            String row = pattern.get(y);
            if (row.length() != w) return DataResult.error(() -> "pattern rows must all be the same length");
            for (int x = 0; x < w; x++) {
                char c = row.charAt(x);
                if (c == ' ') continue;
                Ingredient ing = key.get(c);
                if (ing == null) return DataResult.error(() -> "pattern uses '" + c + "', which is not in the key");
                ings.set(x + y * w, ing);
            }
        }
        return DataResult.success(new ShellrockRecipe(group, w, h, ings, result, pattern, key));
    }

    private static final Codec<Character> SYMBOL = Codec.STRING.comapFlatMap(
            s -> s.length() == 1 && !s.equals(" ") ? DataResult.success(s.charAt(0)) : DataResult.error(() -> "key symbols are one character, not space"),
            String::valueOf);

    private record Raw(String group, List<String> pattern, Map<Character, Ingredient> key, ItemStack result) {}

    public static final MapCodec<ShellrockRecipe> CODEC = RecordCodecBuilder.<Raw>mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(Raw::group),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(Raw::pattern),
            Codec.unboundedMap(SYMBOL, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(Raw::key),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(Raw::result)
    ).apply(i, Raw::new)).flatXmap(
            r -> build(r.group, r.pattern, r.key, r.result),
            r -> DataResult.success(new Raw(r.group, r.pattern, r.key, r.result)));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShellrockRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                buf.writeVarInt(r.width);
                buf.writeVarInt(r.height);
                for (Ingredient ing : r.ingredients) Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                ItemStack.STREAM_CODEC.encode(buf, r.result);
            },
            buf -> {
                String group = ByteBufCodecs.STRING_UTF8.decode(buf);
                int w = buf.readVarInt(), h = buf.readVarInt();
                NonNullList<Ingredient> ings = NonNullList.withSize(w * h, Ingredient.EMPTY);
                for (int k = 0; k < ings.size(); k++) ings.set(k, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                return new ShellrockRecipe(group, w, h, ings, result, new ArrayList<>(), Map.of());
            });

    public static final class Serializer implements RecipeSerializer<ShellrockRecipe> {
        @Override public MapCodec<ShellrockRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ShellrockRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
