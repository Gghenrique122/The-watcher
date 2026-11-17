package com.stalkerhorror.mixin;

import com.stalkerhorror.client.ClientEvents;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    /** No inicio do HUD: escurece o mundo antes de desenhar hotbar, vida etc. */
    @Inject(method = "render", at = @At("HEAD"))
    private void stalker_horror$darkness(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        ClientEvents.renderDarkness(graphics);
    }
}
