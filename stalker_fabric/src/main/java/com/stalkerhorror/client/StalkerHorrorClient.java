package com.stalkerhorror.client;

import com.stalkerhorror.ModEntities;
import com.stalkerhorror.net.ModNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class StalkerHorrorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.STALKER, StalkerRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.DREAD, (client, handler, buf, sender) -> {
            float d = buf.readFloat();
            client.execute(() -> ClientState.targetDread = d);
        });

        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);
    }
}
