package net.pl3x.map.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.configuration.WorldConfig;
import net.pl3x.map.markers.Point;
import net.pl3x.map.render.RendererHolder;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.world.World;

public class UpdateSettingsData implements Runnable {
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .setLenient()
            .create();

    @Override
    public void run() {
        List<Object> players = new ArrayList<>();
        Pl3xMap.api().getPlayerRegistry().entries().forEach((key, player) -> {
            if (player.isHidden()) {
                return;
            }

            if (player.isNPC()) {
                return;
            }

            Map<String, Object> entry = new LinkedHashMap<>();

            entry.put("name", player.getDecoratedName());
            entry.put("uuid", player.getUUID().toString());
            entry.put("displayName", player.getDecoratedName());
            entry.put("world", player.getWorld().getName());

            players.add(entry);
        });

        List<Map<String, Object>> worldSettings = new ArrayList<>();
        Pl3xMap.api().getWorldRegistry().entries().forEach((worldKey, world) -> {
            WorldConfig config = world.getConfig();

            if (!config.ENABLED) {
                return;
            }

            Map<String, Object> spawn = new LinkedHashMap<>();
            Point point = world.getSpawn();
            spawn.put("x", point.getX());
            spawn.put("z", point.getZ());

            Map<String, Object> zoom = new LinkedHashMap<>();
            zoom.put("default", config.ZOOM_DEFAULT);
            zoom.put("maxOut", config.ZOOM_MAX_OUT);
            zoom.put("maxIn", config.ZOOM_MAX_IN);

            Map<String, Object> ui = new LinkedHashMap<>();
            ui.put("link", config.UI_LINK);
            ui.put("coords", config.UI_COORDS);
            ui.put("blockinfo", config.UI_BLOCKINFO);
            ui.put("attribution", config.UI_ATTRIBUTION);

            Map<String, Object> settings = new LinkedHashMap<>();
            settings.put("name", world.getName());
            settings.put("tileUpdateInterval", config.RENDER_BACKGROUND_INTERVAL);
            settings.put("spawn", spawn);
            settings.put("zoom", zoom);
            settings.put("ui", ui);

            FileUtil.write(this.gson.toJson(settings), world.getTilesDir().resolve("settings.json"));

            List<Object> renderers = new ArrayList<>();
            world.getConfig().RENDER_RENDERERS.forEach(renderer -> {
                RendererHolder holder = Pl3xMap.api().getRendererRegistry().get(renderer);
                if (holder != null) {
                    renderers.add(Map.of("label", renderer, "value", holder.getName()));
                }
            });

            Map<String, Object> worldsList = new LinkedHashMap<>();
            worldsList.put("name", world.getName());
            worldsList.put("displayName", config.DISPLAY_NAME
                    .replace("<world>", world.getName()));
            worldsList.put("type", world.getType().toString());
            worldsList.put("order", config.ORDER);
            worldsList.put("renderers", renderers);
            worldSettings.add(worldsList);
        });

        // sort worlds by order, then by name
        worldSettings.sort(Comparator.<Map<String, Object>>comparingInt(w -> (int) w.get("order")).thenComparing(w -> (String) w.get("name")));

        Map<String, Object> lang = new LinkedHashMap<>();
        lang.put("title", Lang.UI_TITLE);
        lang.put("blockInfo", Map.of("label", Lang.UI_BLOCKINFO_LABEL, "value", Lang.UI_BLOCKINFO_VALUE));
        lang.put("coords", Map.of("label", Lang.UI_COORDS_LABEL, "value", Lang.UI_COORDS_VALUE));
        lang.put("layers", Map.of("label", Lang.UI_LAYERS_LABEL, "value", Lang.UI_LAYERS_VALUE));
        lang.put("link", Map.of("label", Lang.UI_LINK_LABEL, "value", Lang.UI_LINK_VALUE));
        lang.put("markers", Map.of("label", Lang.UI_MARKERS_LABEL, "value", Lang.UI_MARKERS_VALUE));
        lang.put("players", Map.of("label", Lang.UI_PLAYERS_LABEL, "value", Lang.UI_PLAYERS_VALUE));
        lang.put("worlds", Map.of("label", Lang.UI_WORLDS_LABEL, "value", Lang.UI_WORLDS_VALUE));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("format", Config.WEB_TILE_FORMAT);
        map.put("maxPlayers", MinecraftServer.getServer().getMaxPlayers());
        map.put("lang", lang);
        map.put("players", players);
        map.put("worldSettings", worldSettings);

        FileUtil.write(this.gson.toJson(map), World.TILES_DIR.resolve("settings.json"));
    }
}
