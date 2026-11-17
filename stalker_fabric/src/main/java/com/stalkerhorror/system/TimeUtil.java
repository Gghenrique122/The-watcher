package com.stalkerhorror.system;

public final class TimeUtil {
    private TimeUtil() {}

    /** 0 = dia claro, 1 = noite fechada. A manha cedo ainda tem 'neblina' residual (ate 0.6). */
    public static float darkness(long dayTime) {
        long t = ((dayTime % 24000L) + 24000L) % 24000L;
        if (t < 1500) return 0.6F * (1.0F - t / 1500.0F);
        if (t < 12000) return 0.0F;
        if (t < 13000) return (t - 12000) / 1000.0F;
        if (t < 23000) return 1.0F;
        return 1.0F - 0.4F * ((t - 23000) / 1000.0F);
    }
}
