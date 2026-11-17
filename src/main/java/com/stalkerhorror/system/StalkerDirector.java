package com.stalkerhorror.system;

import com.stalkerhorror.Config;
import com.stalkerhorror.ModEntities;
import com.stalkerhorror.ModItems;
import com.stalkerhorror.entity.StalkerEntity;
import com.stalkerhorror.entity.StalkerStyle;
import com.stalkerhorror.net.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Diretor: decide QUANDO, ONDE e COMO o Observador aparece, e gerencia o sistema de Sanidade.
 */
public final class StalkerDirector {
    private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_DREAD = new ConcurrentHashMap<>();
    private static final List<Delayed> QUEUE = new ArrayList<>();
    private static long serverTick = 0;

    private record Delayed(long when, Runnable task) {}

    private StalkerDirector() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(StalkerDirector::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            QUEUE.clear();
            ACTIVE.clear();
            LAST_DREAD.clear();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            ACTIVE.remove(id);
            LAST_DREAD.remove(id);
        });
        EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
            if (entity instanceof ServerPlayer p) onWake(p);
        });
    }

    // ------------------------------------------------------------------ infra
    public static void markActive(UUID id, long gameTime) {
        ACTIVE.put(id, gameTime);
    }

    public static boolean isActive(ServerPlayer p) {
        Long t = ACTIVE.get(p.getUUID());
        return t != null && p.serverLevel().getGameTime() - t <= 10L;
    }

    public static void schedule(int delayTicks, Runnable r) {
        QUEUE.add(new Delayed(serverTick + Math.max(0, delayTicks), r));
    }

    private static void onServerTick(MinecraftServer server) {
        serverTick++;
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (p.isSpectator() || !p.isAlive()) continue;
            if (p.tickCount % 20 == 0) secondTick(p);
            if (p.tickCount % 10 == 0) sync(p);
        }
        if (QUEUE.isEmpty()) return;
        List<Delayed> due = new ArrayList<>();
        QUEUE.removeIf(d -> {
            if (d.when() <= serverTick) {
                due.add(d);
                return true;
            }
            return false;
        });
        for (Delayed d : due) {
            try {
                d.task().run();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static boolean hasCharm(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.WARDING_CHARM)) return true;
        }
        return false;
    }

    private static void secondTick(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        boolean overworld = lvl.dimension() == Level.OVERWORLD;
        if (Config.ONLY_OVERWORLD.get() && !overworld) return;

        FearProfile f = FearProfile.of(p);
        long now = lvl.getGameTime();
        float dark = overworld ? TimeUtil.darkness(lvl.getDayTime()) : 0.0F;
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        int block = lvl.getBrightness(LightLayer.BLOCK, eye);
        int sky = lvl.getBrightness(LightLayer.SKY, eye);
        float light = Math.max(block, sky * (1.0F - dark));
        boolean enclosed = sky <= 2;
        boolean deep = overworld && enclosed && eye.getY() < 55;
        boolean charm = hasCharm(p);

        // ---------------- SANIDADE ----------------
        double drain = 0.0D, regen = 0.0D;
        if (light <= 4.0F) drain = deep ? 0.05D : (dark > 0.5F ? 0.07D : 0.02D);
        else if (light <= 8.0F && dark > 0.5F) drain = 0.02D;
        if (lvl.isThundering() && dark > 0.5F) drain += 0.03D;
        if (light >= 12.0F && dark < 0.3F) regen = 0.14D;
        else if (block >= 11) regen = 0.08D;
        else if (dark < 0.3F) regen = 0.04D;
        if (charm) {
            drain *= 0.4D;
            regen = regen * 1.5D + 0.02D;
        }
        drain *= Config.SANITY_DRAIN.get();
        f.addSanity((float) (regen - drain));

        // ---------------- HABITOS (aprendizado) ----------------
        f.add("play", 1);
        if (deep && light <= 5.0F) f.add("ug", 1);
        else if (dark > 0.6F && !enclosed && light <= 6.0F) f.add("nightSurface", 1);
        if (dark > 0.6F && block >= 10) f.add("lit", 1);
        if (p.isInWaterOrBubble() || p.getVehicle() instanceof Boat) f.add("water", 1);

        float sanity = f.sanity();
        if (sanity < 28.0F && lvl.random.nextInt(3) == 0) Haunts.heartbeat(p, 0.5F);

        boolean darkCtx = dark >= 0.55F || deep || (lvl.isRaining() && dark > 0.2F)
                || (Config.DAY_APPEARANCES.get() && sanity < 22.0F && light <= 7.0F);

        // ---------------- SUSTOS ----------------
        if (Config.HAUNTS.get() && darkCtx && sanity < 90.0F && now >= f.nextHaunt() && !p.isSleeping()) {
            Haunts.trigger(p, f);
            double scale = 0.35D + 0.65D * (sanity / 100.0D);
            f.setNextHaunt(now + (long) ((900 + lvl.random.nextInt(1800)) * scale));
        }

        // ---------------- APARICAO ----------------
        if (!darkCtx || isActive(p) || p.isSleeping()) return;
        if (now < f.nextAllowed()) return;
        if (f.get("play") < Config.GRACE_MINUTES.get() * 60) return;
        double chance = (0.045D + (1.0D - sanity / 100.0D) * 0.08D) * Config.SPAWN_CHANCE.get();
        if (charm) chance *= 0.6D;
        if (lvl.random.nextDouble() > chance) return;
        if (hostileNear(p)) return;
        spawn(p, null, false);
    }

    private static boolean hostileNear(ServerPlayer p) {
        return !p.serverLevel().getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(14.0D),
                m -> m.getTarget() == p).isEmpty();
    }

    private static void sync(ServerPlayer p) {
        FearProfile f = FearProfile.of(p);
        float dread = 1.0F - f.sanity() / 100.0F;
        if (isActive(p)) dread = Math.min(1.0F, dread + 0.35F);
        Float last = LAST_DREAD.get(p.getUUID());
        if (last == null || Math.abs(last - dread) > 0.02F || p.tickCount % 100 == 0) {
            LAST_DREAD.put(p.getUUID(), dread);
            ModNetwork.sendDread(p, dread);
        }
    }

    // ------------------------------------------------------------------ pos-aparicao
    public static void afterSighting(ServerPlayer p, boolean seen) {
        FearProfile f = FearProfile.of(p);
        long now = p.serverLevel().getGameTime();
        if (seen) f.add("sightings", 1);
        RandomSource r = p.getRandom();
        int minCd = Config.MIN_COOLDOWN.get() * 1200;
        int maxCd = Math.max(minCd + 1, Config.MAX_COOLDOWN.get() * 1200);
        long cd = minCd + r.nextInt(maxCd - minCd);
        float sanityFactor = 0.45F + 0.55F * f.sanity() / 100.0F;
        float fleeFactor = 1.0F - 0.25F * f.fleeRate();   // quem foge, e perseguido mais cedo
        cd = (long) (cd * sanityFactor * fleeFactor);
        if (!seen) cd = Math.min(cd, 2400 + r.nextInt(2400));
        f.setNextAllowed(now + cd);
    }

    // ------------------------------------------------------------------ despertar
    public static void onWake(ServerPlayer p) {
        FearProfile f = FearProfile.of(p);
        float before = f.sanity();
        f.add("sleeps", 1);
        f.addSanity(20.0F);
        if (before < 80.0F && f.get("sleeps") >= 3 && f.get("play") >= Config.GRACE_MINUTES.get() * 60
                && p.getRandom().nextFloat() < 0.30F && !isActive(p)) {
            UUID id = p.getUUID();
            schedule(60 + p.getRandom().nextInt(80), () -> {
                if (p.getServer() == null) return;
                ServerPlayer pl = p.getServer().getPlayerList().getPlayer(id);
                if (pl != null && pl.isAlive() && !isActive(pl)) spawn(pl, StalkerStyle.WINDOW, false);
            });
        }
    }

    // ------------------------------------------------------------------ spawn
    private static StalkerStyle chooseStyle(ServerPlayer p, FearProfile f, boolean deep) {
        RandomSource r = p.getRandom();
        float stare = f.stareRate(), flee = f.fleeRate(), bold = f.boldRate();
        boolean experienced = f.get("sightings") >= 2;

        if (deep) {
            return (stare > 0.5F && r.nextFloat() < 0.3F) ? StalkerStyle.PERIPHERAL : StalkerStyle.TUNNEL;
        }
        float wWatcher = 1.5F;
        float wPeri = 1.0F + 4.0F * stare;
        float wPursuit = experienced ? 0.5F + 4.0F * flee : 0.2F;
        float wFar = 0.8F + 4.0F * bold;
        float total = wWatcher + wPeri + wPursuit + wFar;
        float x = r.nextFloat() * total;
        if ((x -= wWatcher) < 0) return StalkerStyle.WATCHER;
        if ((x -= wPeri) < 0) return StalkerStyle.PERIPHERAL;
        if ((x -= wPursuit) < 0) return StalkerStyle.PURSUIT;
        return StalkerStyle.FAR;
    }

    public static boolean spawn(ServerPlayer p, StalkerStyle forced, boolean force) {
        ServerLevel lvl = p.serverLevel();
        FearProfile f = FearProfile.of(p);
        long now = lvl.getGameTime();

        BlockPos eye = BlockPos.containing(p.getEyePosition());
        boolean deep = lvl.dimension() == Level.OVERWORLD
                && lvl.getBrightness(LightLayer.SKY, eye) <= 2 && eye.getY() < 55;

        StalkerStyle style = forced != null ? forced : chooseStyle(p, f, deep);
        BlockPos spot = findSpot(p, style, force);
        if (spot == null && forced == null && style != StalkerStyle.TUNNEL) {
            style = StalkerStyle.WATCHER;
            spot = findSpot(p, style, force);
        }
        if (spot == null) {
            if (!force) f.setNextAllowed(now + 400);
            return false;
        }

        StalkerEntity e = ModEntities.STALKER.create(lvl);
        if (e == null) return false;
        e.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.0F, 0.0F);
        // quem costuma encarar -> ele some mais depressa (nunca da tempo de estudar)
        int maxStare = (int) (style.maxStare * (1.0F - 0.4F * f.stareRate()));
        e.setup(p.getUUID(), style, maxStare, style.life);
        lvl.addFreshEntity(e);
        markActive(p.getUUID(), now);
        return true;
    }

    private static BlockPos findGround(ServerLevel lvl, int x, int y0, int z) {
        for (int i = 0; i < 25; i++) {
            int off = (i % 2 == 0) ? i / 2 : -((i + 1) / 2);
            BlockPos feet = new BlockPos(x, y0 + off, z);
            if (!lvl.hasChunkAt(feet)) return null;
            if (standable(lvl, feet)) return feet;
        }
        return null;
    }

    private static boolean standable(ServerLevel lvl, BlockPos feet) {
        BlockPos below = feet.below();
        if (!lvl.getBlockState(below).isFaceSturdy(lvl, below, Direction.UP)) return false;
        if (!lvl.getFluidState(below).isEmpty()) return false;
        for (int i = 0; i < 3; i++) {
            BlockPos b = feet.above(i);
            if (!lvl.getBlockState(b).getCollisionShape(lvl, b).isEmpty()) return false;
            if (!lvl.getFluidState(b).isEmpty()) return false;
        }
        return true;
    }

    private static BlockPos findSpot(ServerPlayer p, StalkerStyle st, boolean force) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = p.getRandom();
        Vec3 eye = p.getEyePosition();
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < 56; i++) {
            double dist = st.minR + r.nextDouble() * (st.maxR - st.minR);
            double rel = st.minA + r.nextDouble() * (st.maxA - st.minA);
            if (r.nextBoolean()) rel = -rel;
            double ang = Math.toRadians(p.getYRot() + rel);
            int x = Mth.floor(p.getX() - Math.sin(ang) * dist);
            int z = Mth.floor(p.getZ() + Math.cos(ang) * dist);
            BlockPos g = findGround(lvl, x, Mth.floor(p.getY()), z);
            if (g == null) continue;
            int maxLight = force ? 15 : (i < 32 ? 3 : 7);
            if (lvl.getBrightness(LightLayer.BLOCK, g) > maxLight) continue;
            Vec3 head = new Vec3(g.getX() + 0.5D, g.getY() + 2.0D, g.getZ() + 0.5D);
            if (lvl.clip(new ClipContext(eye, head, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, p)).getType() != HitResult.Type.MISS) continue;
            if (st != StalkerStyle.FAR) return g;
            if (g.getY() > bestScore) {      // FAR prefere terreno alto
                best = g;
                bestScore = g.getY();
            }
        }
        return best;
    }
}
