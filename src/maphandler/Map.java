package maphandler;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;
import javax.imageio.ImageIO;

import arc.util.io.CounterInputStream;
import maphandler.Saves.*;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.game.Stats;
import mindustry.io.MapIO;
import mindustry.io.SaveIO;
import mindustry.maps.Maps;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.OreBlock;

import static mindustry.Vars.*;

public class Map {
    public static final byte[] mapHeader = new byte[] { 77, 83, 65, 86 };
    public static final IntMap<SaveVersion> versions = new IntMap();
    public static final Array<SaveVersion> versionArray = Array.with(
        new Save1(),
        new Save2(),
        new Save3()
    );
  
    public String name;
    public String author;
    public String description;
    public int version;
    public int build;
    public BufferedImage image;
    public Tile[][] tiles;
    public StringMap stuffs = new StringMap();
  
    public int width;
  
    public int height;
  
    public GameState state;
  
    public Stats stats;
    public Array<String> mods;

    Graphics2D currentGraphics;
    BufferedImage currentImage;
  
    static {
        for (SaveVersion version : versionArray) {
            versions.put(version.version, version);
        }
    }

    Map() { }
    Map(String path) throws IOException {
        Map map = parseMap(path);
    
        this.name = map.name;
        this.author = map.author;
        this.description = map.description;
    
        this.stuffs = map.stuffs;
    
        this.image = map.image;
        this.tiles = map.tiles;
        this.width = map.width;
        this.height = map.height;
    
        this.state = map.state;
        this.stats = map.stats;
        this.mods = map.mods;
    
        this.version = map.version;
    }
  
    public static Map parseMap(String path) throws IOException {
        Map out = new Map();
        Fi file = Fi.get(path);
    
        mindustry.Vars.maps = new Maps();
    
        if (!file.exists()) throw new IOException(path + " not found");
        if (!SaveIO.isSaveValid(file)) throw new IOException(path + " invalid save file");
    
        try(InputStream in = file.read(); InflaterInputStream inf = new InflaterInputStream(in); CounterInputStream counter = new CounterInputStream(in); DataInputStream stream = new DataInputStream(inf)) {
            readHeader(stream);
            int version = stream.readInt();
            Context context = new Context();
      
            SaveVersion ver = versions.get(version);
            ver.read(stream, counter, context);
      
            out.tiles = context.tiles;
            out.width = context.width;
            out.height = context.height;
      
            out.name = context.name;
            out.description = context.description;
            out.author = context.author;
      
            out.build = context.build;
            out.state = context.state;
            out.stats = context.stats;
            out.mods = context.mods;
      
            out.version = version;
      
            out.doRest();
        }

        return out;
    }
  
    void doRest() {
        String assets = "mindustry_assets/";
        try {
            BufferedImage image = ImageIO.read(new File(assets + "sprites/block_colors.png"));
      
            for (Block block : content.blocks()) {
                block.color.argb8888(image.getRGB(block.id, 0));
                if (block instanceof OreBlock) {
                    block.color.set(((OreBlock)block).itemDrop.color);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    
        StringMap stuffs = new StringMap();
        BufferedImage img = new BufferedImage(this.tiles.length, (this.tiles[0]).length, 2);
    
        stuffs.put("playableTeams", this.state.rules.defaultTeam.toString());
    
        for (int i = 0; i < this.width * this.height; i++) {
            int x = i % this.width, y = i / this.width;
            Tile tile = this.tiles[x][y];
            img.setRGB(x, img.getHeight() - 1 - y, Tmp.c1.set(MapIO.colorFor(tile.floor(), tile.block(), tile.overlay(), tile.getTeam())).argb8888());

      
            if (tile.block() == Blocks.coreShard || tile.block() == Blocks.coreFoundation || tile.block() == Blocks.coreNucleus) {
                if (!stuffs.get("cores", "").contains(tile.getTeam().toString())) stuffs.put("cores", getPrettyValue(stuffs.get("cores", ""), tile.getTeam().toString()));

            } else if (tile.block() == Blocks.repairPoint) {
                if (!stuffs.get("playableTeams", "").contains(tile.getTeam().toString())) stuffs.put("playableTeams", getPrettyValue(stuffs.get("playableTeams", ""), tile.getTeam().toString()));

            } else if (tile.block() == Blocks.itemSource || tile.block() == Blocks.liquidSource || tile.block() == Blocks.itemVoid || tile.block() == Blocks.liquidVoid) {
                if (!stuffs.get("sandboxBlockTeams", "").contains(tile.getTeam().toString())) stuffs.put("sandboxBlockTeams", getPrettyValue(stuffs.get("sandboxBlockTeams", ""), tile.getTeam().toString()));

            } else if (tile.overlay() == Blocks.spawn) {
                if (!stuffs.get("spawns", "").contains(tile.getTeam().toString())) stuffs.put("spawns", getPrettyValue(stuffs.get("spawns", ""), tile.getTeam().toString()));

            }
        }

        stuffs.put("cores", stuffs.get("cores", "None"));
        stuffs.put("spawns", stuffs.get("spawns", "None"));
        stuffs.put("sandboxBlockTeams", stuffs.get("sandboxBlockTeams", "None"));
    
        stuffs.put("type", "unknown");
    
        String[] cores = stuffs.get("cores").split(", ");
        String[] spawns = stuffs.get("spawns").split(", ");
        String[] playableTeams = stuffs.get("playableTeams").split(", ");
        String[] sandboxBlockTeams = stuffs.get("sandboxBlockTeams").split(", ");
    
        if (overlaps(playableTeams, sandboxBlockTeams) || this.state.rules.infiniteResources) {
            stuffs.put("type", "sandbox");
        } else if (!cores[0].equals("None") && cores.length == playableTeams.length && spawns[0].equals("None")) {
            stuffs.put("type", "pvp");
        } else if (!cores[0].equals("None") && cores.length > 1 && playableTeams.length == 1) {
            stuffs.put("type", "attack");
        } else if (!cores[0].equals("None") && !spawns[0].equals("None")) {
            stuffs.put("type", "survival");
        }
    
        this.image = img;
        this.stuffs = stuffs;
    }
  
    static BufferedImage tint(BufferedImage image, Color color) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Color tmp = new Color();

        for (int x = 0; x < copy.getWidth(); x++) {
            for (int y = 0; y < copy.getHeight(); y++) {
                int argb = image.getRGB(x, y);

                tmp.argb8888(argb);
                tmp.mul(color);

                copy.setRGB(x, y, tmp.argb8888());
            }
        }

        return copy;
    }
  
    static void readHeader(DataInput input) throws IOException {
        byte[] bytes = new byte[mapHeader.length];

        input.readFully(bytes);

        if (!Arrays.equals(bytes, mapHeader)) {
            throw new IOException("Incorrect header! Expecting: " + Arrays.toString(mapHeader) + "; Actual: " + Arrays.toString(bytes));
        }
    }
  
    public String toString(String start) {
        return
           start + "author=" + this.author + "\n" +
           start + "description=" + this.description + "\n" +
           start + "name=" + this.name + "\n" +
           start + "width=" + this.width + "\n" +
           start + "height=" + this.height + "\n" +
           start + "mods=" + arrayToJson(this.mods);
    }

    public String toString() {
        return toString("");
    }
  
    static String getPrettyValue(String currentValue, String toAdd) {
        if (currentValue.equals("")) return toAdd;
        return currentValue + ", " + toAdd;
    }
  
    static boolean overlaps(String[] arr1, String[] arr2) {
        for (String s : arr1) {
            if (Arrays.asList(arr2).contains(s)) return true;
        }
        return false;
    }
  
    static String arrayToJson(Array<String> arr) {
        StringBuilder json = new StringBuilder();

        for (int i = 0; i < arr.size; i++) {
            json.append("\"").append(arr.get(i).replace("\"", "\\\"")).append("\"");
            if (i != arr.size - 1) json.append(", ");

        }
        return "[" + json.toString() + "]";
    }
  

    static class ImageData implements TextureData {
        final BufferedImage image;

        public ImageData(BufferedImage image) {
            this.image = image;
        }

        public TextureData.TextureDataType getType() {
            return TextureData.TextureDataType.Custom;
        }

    
        public boolean isPrepared() {
            return false;
        }

        public void prepare() {

        }

    
        public Pixmap consumePixmap() {
            return null;
        }

    
        public boolean disposePixmap() {
            return false;
        }

        public void consumeCustomData(int target) {

        }

        public int getWidth() {
            return this.image.getWidth();
        }

    
        public int getHeight() {
            return this.image.getHeight();
        }

    
        public Pixmap.Format getFormat() {
            return Pixmap.Format.RGBA8888;
        }

    
        public boolean useMipMaps() {
            return false;
        }

    
        public boolean isManaged() {
            return false;
        }
    }
  
    static class ImageRegion extends TextureAtlas.AtlasRegion {
        final BufferedImage image;
        final int x;
        final int y;
    
        public ImageRegion(String name, Texture texture, int x, int y, BufferedImage image) {
            super(texture, x, y, image.getWidth(), image.getHeight());
            this.name = name;
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }
}
