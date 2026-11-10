package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One alien wood: logs, wood, planks and the usual planks family. Leaves only for woods that have them. */
public final class WoodSet {
    public final String name;
    public final DeferredBlock<RotatedPillarBlock> log, wood, strippedLog, strippedWood;
    public final DeferredBlock<Block> planks;
    public final DeferredBlock<StairBlock> stairs;
    public final DeferredBlock<SlabBlock> slab;
    public final DeferredBlock<FenceBlock> fence;
    public final DeferredBlock<FenceGateBlock> fenceGate;
    public final DeferredBlock<DoorBlock> door;
    public final DeferredBlock<TrapDoorBlock> trapdoor;
    public final DeferredBlock<ButtonBlock> button;
    public final DeferredBlock<PressurePlateBlock> pressurePlate;
    public final DeferredBlock<LeavesBlock> leaves; // null if leafless

    WoodSet(DeferredRegister.Blocks reg, String name, Block base, boolean hasLeaves) {
        this.name = name;
        BlockSetType set = BlockSetType.register(new BlockSetType(Exofall.MOD_ID + ":" + name));
        WoodType type = WoodType.register(new WoodType(Exofall.MOD_ID + ":" + name, set));
        BlockBehaviour.Properties p = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(base.defaultMapColor());

        log = reg.register(name + "_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
        wood = reg.register(name + "_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));
        strippedLog = reg.register("stripped_" + name + "_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));
        strippedWood = reg.register("stripped_" + name + "_wood", () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));
        planks = reg.register(name + "_planks", () -> new Block(p));
        stairs = reg.register(name + "_stairs", () -> new StairBlock(planks.get().defaultBlockState(), p));
        slab = reg.register(name + "_slab", () -> new SlabBlock(p));
        fence = reg.register(name + "_fence", () -> new FenceBlock(p));
        fenceGate = reg.register(name + "_fence_gate", () -> new FenceGateBlock(type, p));
        door = reg.register(name + "_door", () -> new DoorBlock(set, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)));
        trapdoor = reg.register(name + "_trapdoor", () -> new TrapDoorBlock(set, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));
        button = reg.register(name + "_button", () -> new ButtonBlock(set, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));
        pressurePlate = reg.register(name + "_pressure_plate", () -> new PressurePlateBlock(set, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));
        leaves = hasLeaves ? reg.register(name + "_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES))) : null;
    }
}
