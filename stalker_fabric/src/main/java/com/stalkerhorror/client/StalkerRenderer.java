package com.stalkerhorror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stalkerhorror.StalkerHorror;
import com.stalkerhorror.entity.StalkerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StalkerRenderer extends MobRenderer<StalkerEntity, HumanoidModel<StalkerEntity>> {
    private static final ResourceLocation TEX =
            new ResourceLocation(StalkerHorror.MODID, "textures/entity/stalker.png");

    public StalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.0F);
        this.addLayer(new StalkerEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(StalkerEntity entity) {
        return TEX;
    }

    /** Alto e magro, proporcoes erradas. */
    @Override
    protected void scale(StalkerEntity entity, PoseStack stack, float partialTick) {
        stack.scale(0.92F, 1.38F, 0.92F);
    }
}
