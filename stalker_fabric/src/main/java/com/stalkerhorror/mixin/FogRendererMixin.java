package com.stalkerhorror.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.stalkerhorror.client.ClientEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    /** Neblina densa: sobrescreve inicio/fim da neblina do terreno. */
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void stalker_horror$fog(Camera camera, FogRenderer.FogMode mode, float farPlane,
                                           boolean thick, float partialTick, CallbackInfo ci) {
        ClientEvents.adjustFog(camera, mode, farPlane);
    }

    /** Cor da neblina/ceu no horizonte: quase preta no escuro. */
    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void stalker_horror$fogColor(Camera camera, float partialTicks, ClientLevel level,
                                                int renderDistance, float darken, CallbackInfo ci) {
        float k = ClientEvents.fogColorFactor();
        if (k >= 0.999F) return;
        fogRed *= k;
        fogGreen *= k;
        fogBlue *= k;
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }
}
