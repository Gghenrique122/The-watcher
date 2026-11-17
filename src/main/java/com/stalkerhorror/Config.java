package com.stalkerhorror;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/** Configuracao simples: config/stalker_horror.json (criado sozinho na primeira execucao). */
public final class Config {
    public static final class Val<T> {
        private T v;
        private final T def;

        Val(T def) {
            this.v = def;
            this.def = def;
        }

        public T get() {
            return v;
        }

        @SuppressWarnings("unchecked")
        void set(JsonPrimitive p) {
            if (def instanceof Boolean) v = (T) Boolean.valueOf(p.getAsBoolean());
            else if (def instanceof Integer) v = (T) Integer.valueOf(p.getAsInt());
            else if (def instanceof Double) v = (T) Double.valueOf(p.getAsDouble());
        }
    }

    private static final Map<String, Val<?>> ALL = new LinkedHashMap<>();

    private static <T> Val<T> def(String key, T d) {
        Val<T> v = new Val<>(d);
        ALL.put(key, v);
        return v;
    }

    // ---- jogabilidade ----
    public static final Val<Integer> GRACE_MINUTES = def("graceMinutes", 12);
    public static final Val<Integer> MIN_COOLDOWN = def("minCooldownMinutes", 4);
    public static final Val<Integer> MAX_COOLDOWN = def("maxCooldownMinutes", 10);
    public static final Val<Double> MIN_DISTANCE = def("minDistance", 14.0);
    public static final Val<Double> SPAWN_CHANCE = def("spawnChanceMultiplier", 1.0);
    public static final Val<Boolean> ONLY_OVERWORLD = def("onlyOverworld", true);
    public static final Val<Boolean> DAY_APPEARANCES = def("dayAppearancesWhenInsane", true);
    public static final Val<Double> SANITY_DRAIN = def("sanityDrainMultiplier", 1.0);
    public static final Val<Boolean> HAUNTS = def("enableHaunts", true);
    public static final Val<Boolean> OPEN_DOORS = def("hauntOpenDoors", true);
    public static final Val<Boolean> SNUFF_TORCHES = def("hauntSnuffTorches", true);

    // ---- visual (cliente) ----
    public static final Val<Boolean> DARK_NIGHT = def("darkNight", true);
    public static final Val<Double> NIGHT_DARKNESS = def("nightDarkness", 0.88);
    public static final Val<Boolean> FOG = def("fog", true);
    public static final Val<Double> FOG_DISTANCE = def("fogDistance", 48.0);
    public static final Val<Double> DREAD_FOG_DISTANCE = def("dreadFogDistance", 28.0);
    public static final Val<Boolean> VIGNETTE = def("dreadVignette", true);

    private Config() {}

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("stalker_horror.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(file)) {
                JsonObject o = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                for (Map.Entry<String, Val<?>> e : ALL.entrySet()) {
                    JsonElement je = o.get(e.getKey());
                    if (je != null && je.isJsonPrimitive()) e.getValue().set(je.getAsJsonPrimitive());
                }
            }
            JsonObject out = new JsonObject();
            for (Map.Entry<String, Val<?>> e : ALL.entrySet()) {
                Object v = e.getValue().get();
                if (v instanceof Boolean b) out.addProperty(e.getKey(), b);
                else if (v instanceof Number n) out.addProperty(e.getKey(), n);
            }
            Files.writeString(file, gson.toJson(out));
        } catch (Exception ex) {
            System.err.println("[stalker_horror] Erro ao ler config, usando padroes: " + ex);
        }
    }
}
