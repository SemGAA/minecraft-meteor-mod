package net.kaupenjoe.tutorialmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kaupenjoe.tutorialmod.block.ModBlocks;
import net.kaupenjoe.tutorialmod.entity.custom.MeteorEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.InventoryMenu;

public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.8F;
    }

    @Override
    public void render(MeteorEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.55D, 0.0D);

        float spin = (entity.tickCount + partialTick) * 5.5F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.4F));

        poseStack.pushPose();
        poseStack.scale(2.5F, 2.0F, 2.8F);
        blockRenderer.renderSingleBlock(Blocks.BLACKSTONE.defaultBlockState(),
                poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.4D, 0.1D, -0.25D);
        poseStack.scale(1.5F, 1.4F, 1.5F);
        blockRenderer.renderSingleBlock(Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.45D, -0.2D, 0.3D);
        poseStack.scale(1.2F, 1.2F, 1.2F);
        blockRenderer.renderSingleBlock(Blocks.MAGMA_BLOCK.defaultBlockState(),
                poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 0.0D);
        poseStack.scale(0.9F, 0.9F, 0.9F);
        blockRenderer.renderSingleBlock(ModBlocks.ALEXANDRITE_BLOCK.get().defaultBlockState(),
                poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
