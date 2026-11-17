package com.stalkerhorror.system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Memoria do Observador sobre UM jogador. Fica salva no proprio jogador (sobrevive a morte e reinicio).
 * Tudo o que o mod "aprende" mora aqui.
 */
public final class FearProfile {
    private final CompoundTag t;

    private FearProfile(CompoundTag t) {
        this.t = t;
    }

    /** Armazenamento no mundo: sobrevive a morte, relogin e reinicio. */
    private static final class Store extends SavedData {
        final CompoundTag all = new CompoundTag();

        static Store load(CompoundTag tag) {
            Store s = new Store();
            s.all.merge(tag.getCompound("profiles"));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("profiles", all);
            return tag;
        }
    }

    private static Store store(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(Store::load, Store::new, "stalker_horror_profiles");
    }

    public static FearProfile of(Player p) {
        Store s = store(p.getServer());
        String k = p.getUUID().toString();
        if (!s.all.contains(k, Tag.TAG_COMPOUND)) {
            CompoundTag n = new CompoundTag();
            n.putFloat("sanity", 100.0F);
            s.all.put(k, n);
        }
        s.setDirty();
        return new FearProfile(s.all.getCompound(k));
    }

    public static void reset(Player p) {
        Store s = store(p.getServer());
        s.all.remove(p.getUUID().toString());
        s.setDirty();
    }

    public float sanity() {
        return t.contains("sanity") ? t.getFloat("sanity") : 100.0F;
    }

    public void setSanity(float v) {
        t.putFloat("sanity", Mth.clamp(v, 0.0F, 100.0F));
    }

    public void addSanity(float d) {
        setSanity(sanity() + d);
    }

    public int get(String k) {
        return t.getInt(k);
    }

    public void add(String k, int n) {
        t.putInt(k, t.getInt(k) + n);
    }

    public long nextAllowed() {
        return t.getLong("nextAllowed");
    }

    public void setNextAllowed(long v) {
        t.putLong("nextAllowed", v);
    }

    public long nextHaunt() {
        return t.getLong("nextHaunt");
    }

    public void setNextHaunt(long v) {
        t.putLong("nextHaunt", v);
    }

    public String mimic() {
        return t.getString("mimic");
    }

    public void setMimic(String soundId) {
        t.putString("mimic", soundId);
    }

    public static float ratio(int a, int b) {
        return b <= 0 ? 0.0F : Math.min(1.0F, a / (float) b);
    }

    public float stareRate() {
        return ratio(get("stares"), get("sightings"));
    }

    public float fleeRate() {
        return ratio(get("flees"), get("sightings"));
    }

    public float boldRate() {
        return ratio(get("approaches"), get("sightings"));
    }

    /** Onde o jogador passa mais tempo no escuro. */
    public String habit() {
        int ug = get("ug"), ns = get("nightSurface"), lit = get("lit"), water = get("water");
        int max = Math.max(Math.max(ug, ns), Math.max(lit, water));
        if (max < 120) return "ainda desconhecido";
        if (max == ug) return "cavernas/tuneis";
        if (max == lit) return "base iluminada";
        if (max == water) return "agua/barcos";
        return "superficie a noite";
    }
}
