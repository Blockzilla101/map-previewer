package maphandler;

import arc.files.*;
import arc.graphics.Color;
import arc.math.geom.Point2;
import arc.struct.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.world.*;
import com.google.gson.*;
import mindustry.world.blocks.storage.CoreBlock;

import java.awt.image.*;
import java.io.*;
import java.util.HashMap;
import java.util.zip.*;

import static mindustry.Vars.*;

public class Map {
    public String author, name, description;
    public int width, height;
    public int build;
    public Team playerTeam;
    public Seq<String> mods;
    public Rules rules;
    public JsonArray processorLocations = new JsonArray();
    public JsonObject cores = new JsonObject();
    public JsonArray spawns = new JsonArray();

    /**
     * rendered preview of map, null when make preview is false
     */
    public BufferedImage image;
    /**
     * used converting int rbga to byte rgba
     */
    private final Color color = new Color();

    @SuppressWarnings("unchecked")
    public Map(String path, boolean makePreview) throws IOException {
        if (!Fi.get(path).exists()) throw new IOException("Map doesn't exist");
        try (InputStream ifs = new InflaterInputStream(Fi.get(path).read()); CounterInputStream counter = new CounterInputStream(ifs); DataInputStream stream = new DataInputStream(counter)) {

            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            StringMap[] metaOut = {null};

            ver.region("meta", stream, counter, in -> metaOut[0] = ver.readStringMap(in));

            StringMap meta = metaOut[0];

            name = meta.get("name", "Unknown");
            author = meta.get("author");
            description = meta.get("description");

            width = meta.getInt("width");
            height = meta.getInt("height");

            mods = JsonIO.read(Seq.class, meta.get("mods", "[]"));
            if (mods.size > 0) throw new IOException("Map has mods");

            build = meta.getInt("build", -1);
            rules = JsonIO.read(Rules.class, meta.get("rules"));
            playerTeam = Team.get(meta.getInt("playerTeam", 0));

            var floors = makePreview ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : null;
            var walls = makePreview ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : null;
            var fgraphics = makePreview ? floors.createGraphics() : null;
            var jcolor = makePreview ? new java.awt.Color(0, 0, 0, 64) : null;
            int black = 255;
            CachedTile tile = new CachedTile() {
                @Override
                public void setBlock(Block type) {
                    super.setBlock(type);

                    if (!makePreview) return;

                    int c = MapIO.colorFor(block(), Blocks.air, Blocks.air, team());
                    if (c != black && c != 0) {
                        walls.setRGB(x, floors.getHeight() - 1 - y, conv(c));
                        fgraphics.setColor(jcolor);
                        fgraphics.drawRect(x, floors.getHeight() - 1 - y + 1, 1, 1);
                    }
                }
            };

            ver.region("content", stream, counter, ver::readContentHeader);
            ver.region("preview_map", stream, counter, in -> ver.readMap(in, new WorldContext() {
                @Override
                public void resize(int width, int height) {
                }

                @Override
                public boolean isGenerating() {
                    return false;
                }

                @Override
                public void begin() {
                    world.setGenerating(true);
                }

                @Override
                public void end() {
                    world.setGenerating(false);
                }

                @Override
                public void onReadBuilding() {
                    if (tile.build != null) {
                        if (tile.block() instanceof CoreBlock) {
                            if (!cores.has(tile.build.team.name)) {
                                cores.add(tile.build.team.name, new JsonArray());
                            }

                            var core = new JsonObject();
                            core.addProperty("team", tile.build.team.name);

                            var pos = new JsonObject();
                            pos.addProperty("x", tile.x);
                            pos.addProperty("y", tile.y);
                            core.add("position", pos);

                            cores.get(tile.build.team.name).getAsJsonArray().add(pos);
                        }

                        if (tile.block().equals(Blocks.worldProcessor)) {
                            var pos = new JsonObject();
                            pos.addProperty("x", tile.x);
                            pos.addProperty("y", tile.  y);
                            processorLocations.add(pos);
                        }
                    }

                    if (!makePreview) return;
                    //read team colors
                    if (tile.build != null) {
                        int c = tile.build.team.color.argb8888();
                        int size = tile.block().size;
                        int offsetx = -(size - 1) / 2;
                        int offsety = -(size - 1) / 2;
                        for (int dx = 0; dx < size; dx++) {
                            for (int dy = 0; dy < size; dy++) {
                                int drawx = tile.x + dx + offsetx, drawy = tile.y + dy + offsety;
                                walls.setRGB(drawx, floors.getHeight() - 1 - drawy, c);
                            }
                        }
                    }
                }

                @Override
                public Tile tile(int index) {
                    tile.x = (short) (index % width);
                    tile.y = (short) (index / width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID) {
                    if (overlayID == Blocks.spawn.id) {
                        var pos = new JsonObject();
                        pos.addProperty("x", x);
                        pos.addProperty("y", y);
                        spawns.add(pos);
                    }

                    if (!makePreview) return tile;

                    if (overlayID != 0) {
                        floors.setRGB(x, floors.getHeight() - 1 - y, conv(MapIO.colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict)));
                    } else {
                        floors.setRGB(x, floors.getHeight() - 1 - y, conv(MapIO.colorFor(Blocks.air, content.block(floorID), Blocks.air, Team.derelict)));
                    }
                    return tile;
                }
            }));

            if (makePreview) {
                fgraphics.drawImage(walls, 0, 0, null);
                fgraphics.dispose();
            }

            image = floors;

        } finally {
            content.setTemporaryMapper(null);
        }
    }

    public JsonObject toJson() {
        var obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("description", description == null || description.equals("") ? null : description);
        obj.addProperty("author", author == null || author.equals("") ? null : author);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        obj.addProperty("version", build);
        obj.add("worldProcessorLocations", processorLocations);
        obj.add("cores", cores);

        var rules = new JsonObject();

        rules.addProperty("sandbox", this.rules.infiniteResources);
        rules.addProperty("attack", this.rules.attackMode);
        rules.addProperty("pvp", this.rules.pvp);
        rules.addProperty("hasWeather", this.rules.weather.size > 0);
        rules.addProperty("hasLighting", this.rules.lighting);
        rules.addProperty("hasWaves", this.rules.waves);
        rules.addProperty("hasUnitAmmo", this.rules.unitAmmo);

        var bannedBlocks = new JsonArray();
        var bannedUnits = new JsonArray();
        var loadout = new JsonObject();

        this.rules.bannedBlocks.each(block -> bannedBlocks.add(block.name));
        this.rules.bannedUnits.each(unit -> bannedUnits.add(unit.name));
        this.rules.loadout.each(item -> loadout.addProperty(item.item.name, item.amount));

        rules.add("bannedBlocks", bannedBlocks);
        rules.add("bannedUnits", bannedUnits);
        rules.add("spawns", spawns);
        rules.add("loadout", loadout);
        obj.add("rules", rules);

        return obj;
    }

    int conv(int rgba) {
        return color.set(rgba).argb8888();
    }

    private static class JsonCoreBlock {
        public String team;
        public Point2 position;

        public JsonCoreBlock(Team team, int x, int y) {
            this.team = team.name;
            this.position = new Point2(x, y);
        }
    }
}
