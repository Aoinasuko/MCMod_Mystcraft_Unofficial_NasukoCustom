package com.xcompwiz.mystcraft.block;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.blockentity.BookReceptacleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockBookReceptacle extends BaseEntityBlock {
    public static final EnumProperty<Direction> ROTATION = BlockStateProperties.FACING;

    public BlockBookReceptacle(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ROTATION, context.getClickedFace());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(ROTATION)));
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BookReceptacleBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BookReceptacleBlockEntity receptacle)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!BookReceptacleBlockEntity.isPortalBook(stack) || receptacle.hasBook()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            // CP273: Linking Books are position-bound lazily.  Descriptive Books
            // produced by the Binder already carry LegacyItemData, which hid this
            // bug for a long time; a freshly linked Linking Book could reach the
            // receptacle before inventoryTick/use had initialized its destination.
            // Normalize the actual held stack first, then copy the fully linked
            // payload into the receptacle.
            if (stack.getItem() instanceof com.xcompwiz.mystcraft.item.ItemLinking linking) {
                linking.validateLinkData(level, stack, player);
            }
            receptacle.setBook(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (level instanceof ServerLevel serverLevel) {
                String key = receptacle.getPortalBlocks().isEmpty()
                        ? "message.mystcraft.portal_frame_missing"
                        : "message.mystcraft.portal_active";
                com.xcompwiz.mystcraft.util.MystPlayerMessages.send(player, net.minecraft.network.chat.Component.translatable(key), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BookReceptacleBlockEntity receptacle) || !receptacle.hasBook()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack book = receptacle.removeBook();
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, book);
            } else {
                player.getInventory().placeItemBackInInventory(book, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   net.minecraft.world.level.block.Block neighborBlock,
                                   net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof BookReceptacleBlockEntity receptacle) {
            receptacle.refreshPortal(serverLevel);
        }
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
    }

}
