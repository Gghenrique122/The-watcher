package com.stalkerhorror.client;

import com.stalkerhorror.StalkerHorror;
import com.stalkerhorror.entity.StalkerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Olhos que brilham no escuro (full-bright), visiveis na neblina. */
public class StalkerEyesLayer extends EyesLayer<StalkerEntity, HumanoidModel<StalkerEntity>> {
    private static final RenderType EYES = RenderType.eyes(
            new ResourceLocation(StalkerHorror.MODID, "textures/entity/stalker_eyes.png"));

    public StalkerEyesLayer(RenderLayerParent<StalkerEntity, HumanoidModel<StalkerEntity>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
