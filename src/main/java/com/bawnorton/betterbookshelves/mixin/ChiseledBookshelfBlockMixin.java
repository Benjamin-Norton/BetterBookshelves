package com.bawnorton.betterbookshelves.mixin;

import com.bawnorton.betterbookshelves.BetterBookshelves;
import com.bawnorton.betterbookshelves.util.Book;
import com.bawnorton.betterbookshelves.access.ChiseledBookshelfBlockEntityAccess;
import com.bawnorton.betterbookshelves.util.VectorHelper;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChiseledBookshelfBlock.class)
public abstract class ChiseledBookshelfBlockMixin extends BlockWithEntity {
    @Shadow @Final public static IntProperty BOOKS_STORED;
    @Shadow @Final public static IntProperty LAST_INTERACTION_BOOK_SLOT;

    @Shadow protected abstract void appendProperties(StateManager.Builder<Block, BlockState> builder);

    protected ChiseledBookshelfBlockMixin(Settings settings) {
        super(settings.nonOpaque());
    }

    @Redirect(method = "onUse", at=@At(value = "INVOKE", target = "Lnet/minecraft/block/ChiseledBookshelfBlock;tryRemoveBook(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/block/entity/ChiseledBookshelfBlockEntity;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult tryRemoveLookingAtBook(BlockState state, World world, BlockPos pos, PlayerEntity player, ChiseledBookshelfBlockEntity blockEntity) {
        if (!blockEntity.isEmpty()) {
            int index = getLookingAtIndex(player, blockEntity);
            if(index == -1) return ActionResult.PASS;
            ItemStack itemStack = ((ChiseledBookshelfBlockEntityAccess) blockEntity).getBook(index);
            if(itemStack == ItemStack.EMPTY) return ActionResult.PASS;
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            int i = blockEntity.getBookCount();
            world.setBlockState(pos, state.with(BOOKS_STORED, i).with(LAST_INTERACTION_BOOK_SLOT, i + 1), 3);
            world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            if(!player.getInventory().insertStack(itemStack)) {
                player.dropItem(itemStack, false);
            }
        }
        return ActionResult.CONSUME;
    }

    @Redirect(method = "onUse", at=@At(value = "INVOKE", target = "Lnet/minecraft/block/ChiseledBookshelfBlock;tryAddBook(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/block/entity/ChiseledBookshelfBlockEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult tryAddLookingAtBook(BlockState state, World world, BlockPos pos, PlayerEntity player, ChiseledBookshelfBlockEntity blockEntity, ItemStack stack) {
        if (!blockEntity.isFull()) {
            int index = getLookingAtIndex(player, blockEntity);
            if(index == -1) return ActionResult.PASS;
            boolean sucess = ((ChiseledBookshelfBlockEntityAccess) blockEntity).setBook(index, stack.split(1));
            if(!sucess) {
                stack.increment(1);
                return ActionResult.PASS;
            }
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            if(player.isCreative()) {
                stack.increment(1);
            }
            int i = blockEntity.getBookCount();
            world.setBlockState(pos, state.with(BOOKS_STORED, i).with(LAST_INTERACTION_BOOK_SLOT, i), 3);
            world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return ActionResult.CONSUME;
    }

    @Inject(method = "onStateReplaced", at = @At("HEAD"), cancellable = true)
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved, CallbackInfo ci) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChiseledBookshelfBlockEntity) {
                ChiseledBookshelfBlockEntityAccess chiseledBookshelfBlockEntity = (ChiseledBookshelfBlockEntityAccess)blockEntity;

                for(ItemStack itemStack: chiseledBookshelfBlockEntity.getBooks()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), itemStack);
                }
                world.updateComparators(pos, (ChiseledBookshelfBlock)(Object)this);
                BetterBookshelves.bookshelves.remove(pos);

            }
        } else {
            ((ChiseledBookshelfBlockEntityAccess) world.getBlockEntity(pos)).update();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
        ci.cancel();
    }

    private int getLookingAtIndex(PlayerEntity player, ChiseledBookshelfBlockEntity blockEntity) {
        Vec3d hitPosRel = VectorHelper.getLookOnBlockCoords(player, blockEntity);
        if(hitPosRel == null) return -1;
        boolean isFrontFace = VectorHelper.isOnFrontFace(blockEntity, hitPosRel);
        int x = VectorHelper.getRelativeX(blockEntity, hitPosRel);
        int y = VectorHelper.getRelativeY(hitPosRel);
        if (isFrontFace) {
            Book book = Book.getBook(x, y);
            return book.index();
        }
        return -1;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChiseledBookshelfBlockEntity(pos, state);
    }
}
