package com.bromax.exofall.crafting;

import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.Optional;

/** Shellrock Crafting Table: a 5x5 grid that crafts {@link ShellrockRecipe}s and every vanilla crafting recipe. */
public class ShellrockTableMenu extends AbstractContainerMenu {
    public static final int GRID = 5;
    public static final int RESULT = 0, GRID_START = 1, GRID_END = 1 + GRID * GRID, INV_START = GRID_END, INV_END = INV_START + 36;
    /** Slot positions in the screen texture. */
    public static final int GRID_X = 12, GRID_Y = 18, RESULT_X = 142, RESULT_Y = 54, INV_Y = 124;

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, GRID, GRID);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public ShellrockTableMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public ShellrockTableMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModRecipes.SHELLROCK_TABLE_MENU.get(), id);
        this.access = access;
        this.player = inv.player;
        addSlot(new TableResultSlot(inv.player, craftSlots, resultSlots, 0, RESULT_X, RESULT_Y));
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) addSlot(new Slot(craftSlots, x + y * GRID, GRID_X + x * 18, GRID_Y + y * 18));
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x + y * 9 + 9, 8 + x * 18, INV_Y + y * 18));
        }
        for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x, 8 + x * 18, INV_Y + 58));
    }

    /** The recipe the grid matches: a table recipe first, then vanilla crafting. */
    static Optional<RecipeHolder<? extends Recipe<CraftingInput>>> find(Level level, CraftingInput in) {
        var rm = level.getRecipeManager();
        Optional<RecipeHolder<ShellrockRecipe>> own = rm.getRecipeFor(ModRecipes.SHELLROCK_TYPE.get(), in, level);
        if (own.isPresent()) return Optional.of(own.get());
        return rm.getRecipeFor(RecipeType.CRAFTING, in, level).map(h -> h);
    }

    @Override
    public void slotsChanged(Container container) {
        access.execute((level, pos) -> {
            if (!(player instanceof ServerPlayer sp)) return;
            CraftingInput in = craftSlots.asCraftInput();
            ItemStack out = ItemStack.EMPTY;
            Optional<RecipeHolder<? extends Recipe<CraftingInput>>> found = find(level, in);
            if (found.isPresent() && resultSlots.setRecipeUsed(level, sp, found.get())) {
                ItemStack made = found.get().value().assemble(in, level.registryAccess());
                if (made.isItemEnabled(level.enabledFeatures())) out = made;
            }
            resultSlots.setItem(0, out);
            setRemoteSlot(0, out);
            sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, out));
        });
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, craftSlots));
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.SHELLROCK_CRAFTING_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index == RESULT) {
            access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, p));
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index >= INV_START) {
            if (!moveItemStackTo(stack, GRID_START, GRID_END, false)) {
                if (index < INV_END - 9) {
                    if (!moveItemStackTo(stack, INV_END - 9, INV_END, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, INV_START, INV_END - 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        if (index == RESULT) p.drop(stack, false);
        return copy;
    }

    /** Vanilla's result slot only looks up remainders among crafting recipes; this one uses the table's lookup. */
    static class TableResultSlot extends ResultSlot {
        private final CraftingContainer grid;
        private final Player player;

        TableResultSlot(Player player, CraftingContainer grid, Container result, int index, int x, int y) {
            super(player, grid, result, index, x, y);
            this.grid = grid;
            this.player = player;
        }

        @Override
        public void onTake(Player p, ItemStack stack) {
            checkTakeAchievements(stack);
            CraftingInput.Positioned pos = grid.asPositionedCraftInput();
            CraftingInput in = pos.input();
            Level level = p.level();
            CommonHooks.setCraftingPlayer(p);
            NonNullList<ItemStack> rest = find(level, in).map(h -> h.value().getRemainingItems(in))
                    .orElseGet(() -> NonNullList.withSize(in.size(), ItemStack.EMPTY));
            CommonHooks.setCraftingPlayer(null);
            for (int y = 0; y < in.height(); y++) {
                for (int x = 0; x < in.width(); x++) {
                    int slot = x + pos.left() + (y + pos.top()) * grid.getWidth();
                    ItemStack have = grid.getItem(slot);
                    ItemStack left = rest.get(x + y * in.width());
                    if (!have.isEmpty()) {
                        grid.removeItem(slot, 1);
                        have = grid.getItem(slot);
                    }
                    if (!left.isEmpty()) {
                        if (have.isEmpty()) grid.setItem(slot, left);
                        else if (ItemStack.isSameItemSameComponents(have, left)) {
                            left.grow(have.getCount());
                            grid.setItem(slot, left);
                        } else if (!player.getInventory().add(left)) {
                            player.drop(left, false);
                        }
                    }
                }
            }
        }
    }
}
