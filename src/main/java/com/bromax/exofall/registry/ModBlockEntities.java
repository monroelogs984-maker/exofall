package com.bromax.exofall.registry;

import com.bromax.exofall.Exofall;
import com.bromax.exofall.block.NestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Exofall.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NestBlockEntity>> NEST = TYPES.register("nest",
            () -> BlockEntityType.Builder.of(NestBlockEntity::new,
                    ModBlocks.NESTS.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new)).build(null));

    private ModBlockEntities() {}
}
