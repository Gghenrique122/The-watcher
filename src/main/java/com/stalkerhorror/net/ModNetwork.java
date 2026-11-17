package com.stalkerhorror.net;

import com.stalkerhorror.StalkerHorror;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetwork {
    public static final ResourceLocation DREAD = new ResourceLocation(StalkerHorror.MODID, "dread");

    private ModNetwork() {}

    public static void sendDread(ServerPlayer p, float dread) {
        if (!ServerPlayNetworking.canSend(p, DREAD)) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(dread);
        ServerPlayNetworking.send(p, DREAD, buf);
    }
}
