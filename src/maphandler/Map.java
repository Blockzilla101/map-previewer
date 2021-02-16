package maphandler;

import arc.files.*;
import arc.graphics.Color;
import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.storage.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.util.Objects;
import java.util.zip.*;

import static mindustry.Vars.*;

public class Map {
    private boolean inited = false;

    public String name, author, description;
    public int width, height;

    public Seq<String> mods;

    public GameState state;
    public int lastReadBuild;

    public ObjectMap<String, String> tags = new ObjectMap<>();
    public BufferedImage image;

    Color co = new Color();

    public Map(String path) throws IOException {
        if (!Fi.get(path).exists()) throw new IOException("Map doesn't exist");
        try(InputStream ifs = new InflaterInputStream(Fi.get(path).read()); CounterInputStream counter = new CounterInputStream(ifs); DataInputStream stream = new DataInputStream(counter)){
            if (!inited) init();

            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            StringMap[] metaOut = {null};
            ver.region("meta", stream, counter, in -> metaOut[0] = ver.readStringMap(in));

            StringMap meta = metaOut[0];

            name = meta.get("name", "");
            author = meta.get("author", "");
            description = meta.get("description", "");

            state = new GameState();

            state.wave = meta.getInt("wave");
            state.wavetime = meta.getFloat("wavetime", state.rules.waveSpacing);
            state.stats = JsonIO.read(GameStats.class, meta.get("stats", "{}"));
            state.rules = JsonIO.read(Rules.class, meta.get("rules", "{}"));
            if(this.state.rules.spawns.isEmpty()) state.rules.spawns = waves.get();
            lastReadBuild = meta.getInt("build", -1);

            mods = JsonIO.read(Seq.class, meta.get("mods", "[]"));

            width = meta.getInt("width");
            height = meta.getInt("height");

            tags.put("blockDamageMultiplier", Float.toString(state.rules.blockDamageMultiplier));
            tags.put("blockHealthMultiplier", Float.toString(state.rules.blockHealthMultiplier));
            tags.put("unitDamageMultiplier", Float.toString(state.rules.unitDamageMultiplier));
            tags.put("unitBuildSpeedMultiplier", Float.toString(state.rules.unitBuildSpeedMultiplier));
            tags.put("waveSpacing", Float.toString(state.rules.waveSpacing));

            tags.put("waveTeam", state.rules.waveTeam.toString());
            tags.put("defaultTeam", state.rules.defaultTeam.toString());

            Seq<Integer> cores = new Seq<>();
            Seq<Integer> sandboxTeams = new Seq<>();

            var floors = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var walls = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var fgraphics = floors.createGraphics();
            var jcolor = new java.awt.Color(0, 0, 0, 64);
            int black = 255;

            CachedTile tile = new CachedTile(){
                @Override
                public void setBlock(Block type){
                    super.setBlock(type);

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
                    if (tile.block() instanceof CoreBlock) {
                        if (!cores.contains(tile.team().id)) cores.add(tile.team().id);
                    } else if (tile.block() == Blocks.itemSource || tile.block() == Blocks.itemVoid || tile.block() == Blocks.liquidSource || tile.block() == Blocks.liquidVoid || tile.block() == Blocks.powerSource || tile.block() == Blocks.powerVoid) {
                        if (!sandboxTeams.contains(tile.team().id)) sandboxTeams.add(tile.team().id);
                    }

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
                    if(overlayID != 0){
                        floors.setRGB(x, floors.getHeight() - 1 - y, conv(MapIO.colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict)));
                    }else{
                        floors.setRGB(x, floors.getHeight() - 1 - y, conv(MapIO.colorFor(Blocks.air, content.block(floorID), Blocks.air, Team.derelict)));
                    }

                    if (content.block(overlayID) == Blocks.spawn) {
                        tags.put("hasSpawns", "yes");
                    }
                    return tile;
                }
            }));

            cores.forEach(core -> {
                if (state.rules.teams.get(Team.get(core)).cheat || state.rules.teams.get(Team.get(core)).infiniteResources) {
                    if (!sandboxTeams.contains(core)) sandboxTeams.add(core);
                }
            });

            Seq<String> tempCores = new Seq<>();
            Seq<String> tempSandboxTeams = new Seq<>();

            cores.forEach(core -> tempCores.add(Team.get(core).toString()));
            sandboxTeams.forEach(team -> tempSandboxTeams.add(Team.get(team).toString()));

            tags.put("cores", tempCores.toString(", "));
//            tags.put("sandboxTeamBlocks", tempSandboxTeams.toString(", "));

            tags.put("type", state.rules.mode().name());

            fgraphics.drawImage(walls, 0, 0, null);
            fgraphics.dispose();

            image = floors;

        } finally {
            content.setTemporaryMapper(null);
        }
    }

    private void init() {
        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();

        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.init();
                }catch(Throwable ignored){
                }
            }
        }

        Vars.state = new GameState();
        Vars.waves = new Waves();

        for(ContentType type : ContentType.values()){
            for(Content content : Vars.content.getBy(type)){
                try{
                    content.load();
                }catch(Throwable ignored){
                }
            }
        }

        try {
            BufferedImage image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("sprites/block_colors.png")));

            for(Block block : Vars.content.blocks()) {
                block.mapColor.argb8888(image.getRGB(block.id, 0));
                if (block instanceof OreBlock) {
                    block.mapColor.set(((OreBlock)block).itemDrop.color);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        world = new World() {
            public Tile tile(int x, int y){
                return new Tile(x, y);
            }
        };

        inited = true;
    }

    int conv(int rgba){
        return co.set(rgba).argb8888();
    }

//    static String getPrettyValue(String currentValue, String toAdd) {
//        if (currentValue.equals("")) return toAdd;
//        return currentValue + ", " + toAdd;
//    }
//
//    static boolean overlaps(String[] arr1, String[] arr2) {
//        for (String s : arr1) {
//            if (Arrays.asList(arr2).contains(s)) return true;
//        }
//        return false;
//    }
}
