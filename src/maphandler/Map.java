package maphandler;

import arc.files.*;
import arc.graphics.Color;
import arc.struct.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.world.*;
import com.google.gson.*;

import java.awt.image.*;
import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class Map {
    public String author, name, description;
    public int width, height;
    public int build;
    public Team playerTeam;
    public Seq<String> mods;
    public Rules rules;


    /** rendered preview of map, null when make preview is false */
    public BufferedImage image;
    /** used converting int rbga to byte rgba */
    private final Color color = new Color();

    @SuppressWarnings("unchecked")
    public Map(String path, boolean makePreview) throws IOException {
        if (!Fi.get(path).exists()) throw new IOException("Map doesn't exist");
        try(InputStream ifs = new InflaterInputStream(Fi.get(path).read()); CounterInputStream counter = new CounterInputStream(ifs); DataInputStream stream = new DataInputStream(counter)) {

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
            build = meta.getInt("build", -1);
            rules = JsonIO.read(Rules.class, meta.get("rules"));
            playerTeam = Team.get(meta.getInt("playerTeam", 0));

            var floors = makePreview ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : null;
            var walls = makePreview ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : null;
            var fgraphics = makePreview ? floors.createGraphics() : null;
            var jcolor = makePreview ? new java.awt.Color(0, 0, 0, 64) : null;
            int black = 255;
            CachedTile tile = new CachedTile(){
                @Override
                public void setBlock(Block type){
                    super.setBlock(type);

                    if (!makePreview) return;

                    int c = MapIO.colorFor(block(), Blocks.air, Blocks.air, team());
                    if(c != black && c != 0){
                        walls.setRGB(x, floors.getHeight() - 1 - y, conv(c));
                        fgraphics.setColor(jcolor);
                        fgraphics.drawRect(x, floors.getHeight() - 1 - y + 1, 1, 1);
                    }
                }
            };

            ver.region("content", stream, counter, ver::readContentHeader);
            ver.region("preview_map", stream, counter, in -> ver.readMap(in, new WorldContext(){
                @Override public void resize(int width, int height){}
                @Override public boolean isGenerating(){return false;}
                @Override public void begin(){
                    world.setGenerating(true);
                }
                @Override public void end(){
                    world.setGenerating(false);
                }

                @Override
                public void onReadBuilding(){
                    if (!makePreview) return;
                    //read team colors
                    if(tile.build != null){
                        int c = tile.build.team.color.argb8888();
                        int size = tile.block().size;
                        int offsetx = -(size - 1) / 2;
                        int offsety = -(size - 1) / 2;
                        for(int dx = 0; dx < size; dx++){
                            for(int dy = 0; dy < size; dy++){
                                int drawx = tile.x + dx + offsetx, drawy = tile.y + dy + offsety;
                                walls.setRGB(drawx, floors.getHeight() - 1 - drawy, c);
                            }
                        }
                    }
                }

                @Override
                public Tile tile(int index){
                    tile.x = (short)(index % width);
                    tile.y = (short)(index / width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID){
                    if (!makePreview) return tile;

                    if(overlayID != 0){
                        floors.setRGB(x, floors.getHeight() - 1 - y, conv(MapIO.colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict)));
                    }else{
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

        }finally{
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
        return obj;
    }

    int conv(int rgba){
        return color.set(rgba).argb8888();
    }
}
