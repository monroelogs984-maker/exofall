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
    /** The meteor whose structure start is in or near the source's chunk. */
    static com.bromax.exofall.world.MeteorPiece meteorAt(CommandSourceStack src) {
        var level = src.getLevel();
        var reg = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        BlockPos here = BlockPos.containing(src.getPosition());
        for (var start : level.structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(here),
                s -> reg.getKey(s) != null && reg.getKey(s).getNamespace().equals("exofall"))) {
            for (var piece : start.getPieces()) if (piece instanceof com.bromax.exofall.world.MeteorPiece m) return m;
        }
        return null;
    }

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
                })))
                // /exofall arena wake <socket>: what a sigil does, without one
                .then(Commands.literal("arena").then(Commands.literal("wake").then(Commands.argument("socket", BlockPosArgument.blockPos()).executes(ctx -> {
                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "socket");
                    var level = ctx.getSource().getLevel();
                    var state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof com.bromax.exofall.block.SigilSocketBlock)) {
                        ctx.getSource().sendFailure(Component.literal("No Sigil Socket there"));
                        return 0;
                    }
                    boolean ok = com.bromax.exofall.entity.boss.Arenas.awaken(level, pos, state, null);
                    ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Boss woken" : "A boss is already awake there"), true);
                    return ok ? 1 : 0;
                })))
                // /exofall arena find: where this meteor's arena socket is (nearest meteor start)
                .then(Commands.literal("socket").executes(ctx -> {
                    var src = ctx.getSource();
                    var level = src.getLevel();
                    BlockPos here = BlockPos.containing(src.getPosition());
                    for (var start : level.structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(here),
                            s -> level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(s) != null
                                    && level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(s).getNamespace().equals("exofall"))) {
                        for (var piece : start.getPieces()) {
                            if (piece instanceof com.bromax.exofall.world.MeteorPiece m) {
                                BlockPos c = com.bromax.exofall.world.ArenaGen.centre(m);
                                BlockPos sock = com.bromax.exofall.world.ArenaLayout.socket(c, com.bromax.exofall.world.ArenaGen.door(m));
                                src.sendSuccess(() -> Component.literal("Arena centre " + c.toShortString() + ", socket " + sock.toShortString()), false);
                                return 1;
                            }
                        }
                    }
                    src.sendFailure(Component.literal("Not inside a meteor"));
                    return 0;
                })))
                // /exofall dungeon check | where: walkability self-test / entrance list for the meteor you're in
                .then(Commands.literal("dungeon")
                        .then(Commands.literal("check").executes(ctx -> {
                            var src = ctx.getSource();
                            var m = meteorAt(src);
                            if (m == null) { src.sendFailure(Component.literal("Not inside a meteor")); return 0; }
                            for (String line : com.bromax.exofall.world.dungeon.DungeonCheck.run(src.getLevel(), m))
                                src.sendSuccess(() -> Component.literal(line), false);
                            return 1;
                        }))
                        .then(Commands.literal("where").executes(ctx -> {
                            var src = ctx.getSource();
                            var m = meteorAt(src);
                            if (m == null) { src.sendFailure(Component.literal("Not inside a meteor")); return 0; }
                            m.initNoise();
                            var dg = com.bromax.exofall.world.dungeon.Dungeon.of(m);
                            for (var s : dg.shacks)
                                src.sendSuccess(() -> Component.literal((s.entry ? "Entrance" : "Nest shack") + " at " + s.x + " " + s.floor + " " + s.z), false);
                            return 1;
                        })))
                // /exofall boss fight: skip the nearest boss's dormant and intro steps (testing)
                .then(Commands.literal("boss").then(Commands.literal("fight").executes(ctx -> {
                    var src = ctx.getSource();
                    var list = src.getLevel().getEntitiesOfClass(com.bromax.exofall.entity.boss.ExofallBoss.class,
                            new net.minecraft.world.phys.AABB(BlockPos.containing(src.getPosition())).inflate(48));
                    for (var b : list) b.forceFight();
                    src.sendSuccess(() -> Component.literal(list.size() + " boss(es) fighting"), true);
                    return list.size();
                })))
                // /exofall weapontest: checks every legendary weapon's on-hit effect against husks (testing)
                .then(Commands.literal("weapontest").executes(ctx -> {
                    var src = ctx.getSource();
                    for (String line : com.bromax.exofall.item.WeaponSelfTest.run(src.getLevel(), BlockPos.containing(src.getPosition())))
                        src.sendSuccess(() -> Component.literal(line), false);
                    return 1;
                }))
                // /exofall baubletest: checks the baubles' hooks on a fake player (testing)
                .then(Commands.literal("baubletest").executes(ctx -> {
                    var src = ctx.getSource();
                    for (String line : com.bromax.exofall.item.BaubleSelfTest.run(src.getLevel(), BlockPos.containing(src.getPosition())))
                        src.sendSuccess(() -> Component.literal(line), false);
                    return 1;
                }))
                // /exofall mobtest: runs the dungeon mobs' mechanics on a test pad 40 blocks up (testing)
                .then(Commands.literal("mobtest").executes(ctx -> {
                    var src = ctx.getSource();
                    for (String line : com.bromax.exofall.entity.mob.MobSelfTest.run(src.getLevel(), BlockPos.containing(src.getPosition())))
                        src.sendSuccess(() -> Component.literal(line), false);
                    return 1;
                })));
    }

    private ExofallCommands() {}
}
