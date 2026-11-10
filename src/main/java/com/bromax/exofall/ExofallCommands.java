package com.bromax.exofall;

import com.bromax.exofall.block.AlienSaplingBlock;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /exofall grow <pos>: grow the alien sapling at pos right away (ops only; for testing). */
public final class ExofallCommands {
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("exofall").requires(s -> s.hasPermission(2))
                .then(Commands.literal("grow").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                    var level = ctx.getSource().getLevel();
                    if (!(level.getBlockState(pos).getBlock() instanceof AlienSaplingBlock sapling)) {
                        ctx.getSource().sendFailure(Component.literal("No Exofall sapling there"));
                        return 0;
                    }
                    boolean ok = sapling.grow(level, pos, level.random);
                    if (ok) ctx.getSource().sendSuccess(() -> Component.literal("Grew the tree"), true);
                    else ctx.getSource().sendFailure(Component.literal("No room for the tree"));
                    return ok ? 1 : 0;
                }))));
    }

    private ExofallCommands() {}
}
