package com.bromax.exofall.block;

import com.bromax.exofall.crafting.ShellrockTableMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shellrock Crafting Table: opens the 5x5 grid. */
public class ShellrockTableBlock extends CraftingTableBlock {
    public static final MapCodec<ShellrockTableBlock> CODEC = simpleCodec(ShellrockTableBlock::new);
    private static final Component TITLE = Component.translatable("container.exofall.shellrock_crafting_table");

    public ShellrockTableBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<? extends CraftingTableBlock> codec() {
        return CODEC;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inv, p) -> new ShellrockTableMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
