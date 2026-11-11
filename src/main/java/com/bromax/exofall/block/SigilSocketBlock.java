package com.bromax.exofall.block;

import com.bromax.exofall.entity.boss.Arenas;
import com.bromax.exofall.item.SigilItem;
import com.bromax.exofall.world.MeteorStyle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sits in the arena's door post, facing out. Using the matching meteor's sigil on it opens the Sigil Seal
 * and wakes the boss, which lies dormant in the arena until someone walks in. Unbreakable.
 */
public class SigilSocketBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SigilSocketBlock> CODEC = simpleCodec(SigilSocketBlock::new);
    public static final EnumProperty<MeteorStyle> KIND = EnumProperty.create("kind", MeteorStyle.class);

    public SigilSocketBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(KIND, MeteorStyle.SWAMP));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KIND);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof SigilItem sigil)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        MeteorStyle kind = state.getValue(KIND);
        if (sigil.style() != kind) {
            player.displayClientMessage(Component.translatable("block.exofall.sigil_socket.wrong", SigilItem.nameFor(kind)), true);
            return ItemInteractionResult.FAIL;
        }
        if (Arenas.awaken((ServerLevel) level, pos, state, player)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("block.exofall.sigil_socket.hint", SigilItem.nameFor(state.getValue(KIND))), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
