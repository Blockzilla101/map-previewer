package maphandler;

import arc.files.Fi;
import arc.util.*;
import com.google.gson.*;
import mindustry.Vars;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import java.awt.image.*;
import java.io.IOException;
import java.util.*;
import javax.imageio.*;

import static mindustry.Vars.*;

public class MapHandler {
    static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static void main(String[] args) {
        Log.level = Log.LogLevel.err;

        init();


        try {
            var mapOptions = gson.fromJson(Fi.get(args[0]).reader(), JsonArray.class);
            var previewed = new JsonArray();

            mapOptions.forEach(mapOption -> {
                var mapPath = mapOption.getAsJsonObject().get("mapPath").getAsString();
                String previewPath = null;
                if (mapOption.getAsJsonObject().get("previewPath") != null) {
                    previewPath = mapOption.getAsJsonObject().get("previewPath").getAsString();
                }

                try {
                    var preview = new Map(mapPath, previewPath != null);
                    var previewData = preview.toJson();
                    previewData.addProperty("mapPath", mapPath);

                    if (previewPath != null) {
                        ImageIO.write(preview.image, "png", Fi.get(previewPath).write());
                        previewData.addProperty("previewPath", previewPath);
                    }

                    previewed.add(previewData);

                } catch (IOException e) {
                    var error = new JsonObject();
                    error.addProperty("error", e.getMessage());
                    error.addProperty("mapPath", mapPath);

                    if (e.getMessage().equals("Map doesn't exist")) {
                        error.addProperty("code", MapPreviewErrorCodes.InvalidMap.ordinal());
                    } else if (e.getMessage().equals("Map has mods")) {
                        error.addProperty("code", MapPreviewErrorCodes.HasMods.ordinal());
                    } else {
                        error.addProperty("code", MapPreviewErrorCodes.Other.ordinal());
                    }

                    previewed.add(error);
                }
            });

            System.out.println(gson.toJson(previewed));
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static void init() {
        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();

        for (ContentType type : ContentType.values()) {
            for (Content content : Vars.content.getBy(type)) {
                try {
                    content.init();
                } catch (Throwable ignored) {
                }
            }
        }

        Vars.state = new GameState();
        Vars.waves = new Waves();

        for (ContentType type : ContentType.values()) {
            for (Content content : Vars.content.getBy(type)) {
                try {
                    content.load();
                } catch (Throwable ignored) {
                }
            }
        }

        try {
            BufferedImage image = ImageIO.read(Objects.requireNonNull(MapHandler.class.getClassLoader().getResource("sprites/block_colors.png")));

            for (Block block : Vars.content.blocks()) {
                block.mapColor.argb8888(image.getRGB(block.id, 0));
                if (block instanceof OreBlock) {
                    block.mapColor.set(block.itemDrop.color);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        world = new World() {
            public Tile tile(int x, int y) {
                return new Tile(x, y);
            }
        };
    }

    enum MapPreviewErrorCodes {
        InvalidMap,
        HasMods,
        Other,
    }
}
