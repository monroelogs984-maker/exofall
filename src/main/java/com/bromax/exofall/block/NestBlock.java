package com.bromax.exofall.block;

import com.bromax.exofall.registry.ModBlockEntities;
import com.bromax.exofall.registry.ModBlocks;
import com.bromax.exofall.world.MeteorStyle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A meteor's bug nest: a lump riddled with holes that keeps letting out that meteor's bugs while a player is near
 * (see {@link NestBlockEntity}). Found in the Warrens and in the shacks above them. Breaking it drops experience.
 */
public class NestBlock extends BaseEntityBlock {
    public static final MapCodec<NestBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            MeteorStyle.CODEC.fieldOf("style").forGetter(b -> b.style), propertiesCodec()).apply(i, NestBlock::new));

    private final MeteorStyle style;

    public NestBlock(MeteorStyle style, Properties props) {
        super(props);
        this.style = style;
    }

    public MeteorStyle style() {
        return style;
    }

    public static BlockState forStyle(MeteorStyle style) {
        return ModBlocks.NESTS.get(style).get().defaultBlockState();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.NEST.get(), NestBlockEntity::serverTick);
    }

    @Override
    public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity be, @Nullable Entity breaker, ItemStack tool) {
        return 15 + level.getRandom().nextInt(15) + level.getRandom().nextInt(15);
    }

    /** Something always stirring in the holes. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) != 0) return;
        ParticleOptions p = switch (style) {
            case SWAMP -> ParticleTypes.MYCELIUM;
            case HIVE -> ParticleTypes.DRIPPING_HONEY;
            case DESERT -> ParticleTypes.WHITE_ASH;
            case RAINFOREST -> ParticleTypes.SPORE_BLOSSOM_AIR;
        };
        double x = pos.getX() + r.nextDouble(), y = pos.getY() + (style == MeteorStyle.HIVE ? 0.05 : 0.6 + r.nextDouble() * 0.5),
                z = pos.getZ() + r.nextDouble();
        level.addParticle(p, x, y, z, 0, 0.02, 0);
        if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.03, 0);
    }
}
