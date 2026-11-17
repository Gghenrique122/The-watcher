package com.stalkerhorror.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.stalkerhorror.Config;
import com.stalkerhorror.system.TimeUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;

/**
 * Atmosfera: noite quase breu (tochas importam!), neblina densa, vinheta de medo.
 * Os metodos abaixo sao chamados pelo tick do cliente e pelos mixins (GuiMixin / FogRendererMixin).
 */
public final class ClientEvents {
    private ClientEvents() {}

    private static int heldLight(Player p) {
        int m = 0;
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof BlockItem bi) {
                m = Math.max(m, bi.getBlock().defaultBlockState().getLightEmission());
            }
        }
        return m;
    }

    public static void onClientTick(Minecraft mc) {
        ClientLevel lvl = mc.level;
        Player pl = mc.player;
        if (lvl == null || pl == null) {
            ClientState.reset();
            return;
        }
        boolean overworld = lvl.dimension() == Level.OVERWORLD;
        float night = overworld ? TimeUtil.darkness(lvl.getDayTime()) : 0.0F;
        BlockPos eye = BlockPos.containing(pl.getEyePosition());
        int block = lvl.getBrightness(LightLayer.BLOCK, eye);
        int sky = lvl.getBrightness(LightLayer.SKY, eye);
        float skyEff = sky * (1.0F - night);
        float light = Math.max(Math.max(block, skyEff), heldLight(pl) * 0.75F) / 15.0F;
        float targetDark = overworld ? 1.0F - Mth.clamp(light, 0.0F, 1.0F) : 0.0F;
        if (pl.hasEffect(MobEffects.NIGHT_VISION)) targetDark *= 0.35F;

        ClientState.night = night;
        ClientState.dark += (targetDark - ClientState.dark) * 0.06F;
        ClientState.dread += (ClientState.targetDread - ClientState.dread) * 0.05F;
    }

    /** Desenhado no INICIO do HUD: escurece o mundo, mas a interface continua nitida. */
    public static void renderDarkness(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        if (Config.DARK_NIGHT.get()) {
            float a = Config.NIGHT_DARKNESS.get().floatValue() * ClientState.dark * (0.4F + 0.6F * ClientState.night);
            a += ClientState.dread * 0.10F * ClientState.dark;
            a = Mth.clamp(a, 0.0F, 0.97F);
            if (a > 0.004F) g.fill(0, 0, w, h, ((int) (a * 255.0F)) << 24);
        }

        if (Config.VIGNETTE.get() && ClientState.dread > 0.12F) {
            float pulse = 0.75F + 0.25F * Mth.sin(mc.level.getGameTime() * (0.10F + ClientState.dread * 0.25F));
            float a = Mth.clamp((ClientState.dread - 0.1F) * 0.9F * pulse, 0.0F, 0.85F);
            int edge = h / 3;
            int strong = ((int) (a * 255.0F)) << 24;
            g.fillGradient(0, 0, w, edge, strong, 0x00000000);
            g.fillGradient(0, h - edge, w, h, 0x00000000, strong);
        }
    }

    /** Chamado no fim de FogRenderer.setupFog. */
    public static void adjustFog(Camera camera, FogRenderer.FogMode mode, float far0) {
        if (!Config.FOG.get()) return;
        if (mode != FogRenderer.FogMode.FOG_TERRAIN) return;
        if (camera.getFluidInCamera() != FogType.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;

        float strength = ClientState.dark * (0.5F + 0.5F * ClientState.night);
        float dread = ClientState.dread;
        if (strength < 0.01F && dread < 0.05F) return;

        float far = Mth.lerp(strength, far0, Math.min(far0, Config.FOG_DISTANCE.get().floatValue()));
        float dreadFog = Math.min(far, Config.DREAD_FOG_DISTANCE.get().floatValue());
        far = Mth.lerp(Mth.clamp(dread * 0.8F, 0.0F, 1.0F), far, dreadFog);

        RenderSystem.setShaderFogShape(FogShape.SPHERE);
        RenderSystem.setShaderFogStart(far * 0.18F);
        RenderSystem.setShaderFogEnd(far);
    }

    /** Fator (0..1) que escurece a cor da neblina. 1 = sem alteracao. */
    public static float fogColorFactor() {
        if (!Config.FOG.get()) return 1.0F;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD) return 1.0F;
        return 1.0F - Mth.clamp(ClientState.dark * (0.5F + 0.5F * ClientState.night) * 0.93F
                + ClientState.dread * 0.2F, 0.0F, 0.97F);
    }
}
