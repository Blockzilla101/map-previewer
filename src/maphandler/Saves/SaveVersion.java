package maphandler.Saves;

import arc.struct.Array;
import arc.struct.StringMap;
import arc.util.io.CounterInputStream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

import mindustry.content.Blocks;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.MappableContent;
import mindustry.game.DefaultWaves;
import mindustry.game.Rules;
import mindustry.game.Stats;
import mindustry.io.JsonIO;
import mindustry.io.SaveFileReader;
import mindustry.world.Block;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public abstract class SaveVersion extends mindustry.io.SaveVersion {
    DefaultWaves defaultWaves = new DefaultWaves();
  
    public SaveVersion(int version) {
        super(version);
    
        content = new ContentLoader();
        content.createBaseContent();
        for (ContentType type : ContentType.values()) {
            for (Content content : content.getBy(type)) {
                try {
                    content.init();
                } catch (Throwable throwable) {

                }
            }
        }

        world = new World() {
            public boolean isGenerating() {
                return true;
            }

            public void notifyChanged(Tile tile) {

            }
        };

        state = new GameState();
    }
  
    public final void read(DataInputStream stream, CounterInputStream counter, Context context) throws IOException {
        region("meta", stream, counter, in -> readMeta(in, context));
        region("content", stream, counter, this::readContentHeader);
    
        try {
            region("map", stream, counter, in -> readMap(in, context));
        } finally {
            content.setTemporaryMapper((MappableContent[][])null);
        }
    }
  
    protected void region(String name, DataInput stream, CounterInputStream counter, SaveFileReader.IORunner<DataInput> cons) throws IOException {
        counter.resetCount();
        try {
            readChunk(stream, cons);
        } catch (Throwable e) {
            IOException exp = new IOException("Error reading region \"" + name + "\".", e);
            exp.initCause(e);
            throw exp;
        }
    }
  
    public void readMap(DataInput stream, Context context) throws IOException {
        int width = stream.readUnsignedShort();
        int height = stream.readUnsignedShort();
    
        boolean generating = context.isGenerating();
    
        if (!generating) context.begin();
        try {
            context.resize(width, height);
      
            int i;
            for (i = 0; i < width * height; i++) {
                int x = i % width, y = i / width;
                short floorid = stream.readShort();
                short oreid = stream.readShort();
                int consecutives = stream.readUnsignedByte();
                if (content.block(floorid) == Blocks.air) floorid = Blocks.stone.id;

                context.create(x, y, floorid, oreid, 0);

                for (int j = i + 1; j < i + 1 + consecutives; j++) {
                    int newx = j % width, newy = j / width;
                    context.create(newx, newy, floorid, oreid, 0);
                }

                i += consecutives;
            }

            for (i = 0; i < width * height; i++) {
                int x = i % width, y = i / width;

                Block block = content.block(stream.readShort());
                Tile tile = context.tile(x, y);

                if (block == null) block = Blocks.air;

                tile.setBlock(block);

                if (tile.entity != null) {
                    try {
                        readChunk(stream, true, in -> {
                            byte version = in.readByte();
                            tile.entity.read(in, version);
                        });
                    } catch (Exception e) {
                        throw new IOException("Failed to read tile entity of block: " + block, e);
                    }
                } else {
                    int consecutives = stream.readUnsignedByte();
                    for (int j = i + 1; j < i + 1 + consecutives; j++) {
                        int newx = j % width, newy = j / width;
                        context.tile(newx, newy).setBlock(block);
                    }
                    i += consecutives;
                }
            }
        } finally {
            if (!generating) context.end();
        }
    }
  
    public void readMeta(DataInput stream, Context context) throws IOException {
        StringMap map = readStringMap(stream);
    
        context.state.wave = map.getInt("wave");
        context.state.wavetime = map.getFloat("wavetime", context.state.rules.waveSpacing);
    
        try {
            context.stats = JsonIO.read(Stats.class, map.get("stats", "{}"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            context.state.rules = (Rules)JsonIO.read(Rules.class, (String)map.get("rules", "{}"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    
        try {
            context.mods = (Array<String>)JsonIO.read(Array.class, (String)map.get("mods", "[]"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    
        if (context.state.rules.spawns.isEmpty()) context.state.rules.spawns = this.defaultWaves.get();

        this.lastReadBuild = map.getInt("build", -1);
    
        context.name = map.get("name", "Unknown");
        context.author = map.get("author", "Unknown");
        context.description = map.get("description", "None");

        if (context.description.trim().isEmpty()) context.description = "None";
    
        context.build = this.lastReadBuild;
    }
  
    public void readContentHeader(DataInput stream) throws IOException {
        byte mapped = stream.readByte();
    
        MappableContent[][] map = new MappableContent[(ContentType.values()).length][0];
    
        for (int i = 0; i < mapped; i++) {
            ContentType type = ContentType.values()[stream.readByte()];
            short total = stream.readShort();
            map[type.ordinal()] = new MappableContent[total];
      
            for (int j = 0; j < total; j++) {
                String name = stream.readUTF();
                map[type.ordinal()][j] = content.getByName(type, this.fallback.get(name, name));
            }
        }
        content.setTemporaryMapper(map);
    }
}
