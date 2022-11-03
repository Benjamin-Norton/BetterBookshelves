package com.bawnorton.betterbookshelves.client.render;

import com.bawnorton.betterbookshelves.BetterBookshelves;
import com.bawnorton.betterbookshelves.config.Config;
import com.bawnorton.betterbookshelves.util.Book;
import com.bawnorton.betterbookshelves.util.Helper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Objects;

public class ChiseledBookshelfBlockEntityRenderer implements BlockEntityRenderer<ChiseledBookshelfBlockEntity> {
    public ChiseledBookshelfBlockEntityRenderer() {}

    @Override
    public void render(ChiseledBookshelfBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if(BetterBookshelves.CONFIG.textPreview != Config.TextPreview.ON) return;
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult target = client.crosshairTarget;
        if(target != null && target.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) target;
            BlockPos pos = blockHitResult.getBlockPos();
            assert client.world != null;
            if(!Objects.equals(client.world.getBlockEntity(pos), entity)) return;
        } else return;

        Book bookToRender = Helper.getLookingAtBook(entity);
        if(bookToRender != Book.NONE) {
            int bookIndex = bookToRender.index();
            ItemStack book = entity.getStack(bookIndex);
            if(book == ItemStack.EMPTY) return;
            assert entity.getWorld() != null;
            BlockState state = entity.getWorld().getBlockState(entity.getPos());
            if(!state.isOf(Blocks.CHISELED_BOOKSHELF)) return;

            Direction rotation = state.get(Properties.HORIZONTAL_FACING);
            TextRenderer textRenderer = client.textRenderer;
            List<Text> displayText = Helper.getBookText(book);
            int textSize = BetterBookshelves.CONFIG.textSize;

            matrices.push();
            matrices.translate(0.5, 0.5, 0.5);
            matrices.multiply(new Quaternionf(new AxisAngle4f((float) Math.toRadians(Helper.getRotation(rotation)), 0, 1f, 0)));
            matrices.translate((32 - (bookToRender.x2 + bookToRender.x1)) / 32.0 - 0.5, (bookToRender.y2 + bookToRender.y1) / 32.0 - 0.5, -0.6);
            matrices.scale(-textSize / 400.0f, -textSize / 400.0f, textSize / 400.0f);

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            float g = client.options.getTextBackgroundOpacity(0f);
            int j = (int)(g * 255.0F) << 24;
            int y = 0;
            for(Text text: displayText) {
                float h = (float)(-textRenderer.getWidth(text) / 2);
                textRenderer.draw(text, h, y, book.getItem() == Items.ENCHANTED_BOOK ? 16777045 : 16777215, false, matrix4f, vertexConsumers, true, j, light);
                y += 10;
            }
            matrices.pop();
        }
    }
}