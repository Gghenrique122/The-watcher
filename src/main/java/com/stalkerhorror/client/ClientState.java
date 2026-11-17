package com.stalkerhorror.client;

/** Estado visual do cliente. Sem imports do Minecraft (seguro de carregar em qualquer lado). */
public final class ClientState {
    public static volatile float targetDread = 0.0F;   // vindo do servidor (0..1)
    public static float dread = 0.0F;                  // suavizado
    public static float dark = 0.0F;                   // 0 = iluminado, 1 = escuridao total (suavizado)
    public static float night = 0.0F;                  // 0 = dia, 1 = noite

    private ClientState() {}

    public static void reset() {
        targetDread = 0.0F;
        dread = 0.0F;
        dark = 0.0F;
        night = 0.0F;
    }
}
