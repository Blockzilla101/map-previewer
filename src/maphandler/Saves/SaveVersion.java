/*     */ package maphandler.Saves;
/*     */ import arc.struct.Array;
/*     */ import arc.struct.StringMap;
/*     */ import arc.util.io.CounterInputStream;
/*     */ import java.io.DataInput;
/*     */ import java.io.DataInputStream;
/*     */ import java.io.IOException;
/*     */ import mindustry.Vars;
/*     */ import mindustry.content.Blocks;
/*     */ import mindustry.core.World;
/*     */ import mindustry.ctype.Content;
/*     */ import mindustry.ctype.ContentType;
/*     */ import mindustry.ctype.MappableContent;
/*     */ import mindustry.game.DefaultWaves;
/*     */ import mindustry.game.Rules;
/*     */ import mindustry.game.Stats;
/*     */ import mindustry.io.JsonIO;
/*     */ import mindustry.io.SaveFileReader;
/*     */ import mindustry.world.Block;
/*     */ import mindustry.world.Tile;
/*     */ 
/*     */ public abstract class SaveVersion extends SaveVersion {
/*  23 */   DefaultWaves defaultWaves = new DefaultWaves();
/*     */   
/*     */   public SaveVersion(int version) {
/*  26 */     super(version);
/*     */     
/*  28 */     Vars.content = new ContentLoader();
/*  29 */     Vars.content.createBaseContent();
/*  30 */     for (ContentType type : ContentType.values()) {
/*  31 */       for (Content content : Vars.content.getBy(type)) {
/*     */         try {
/*  33 */           content.init();
/*  34 */         } catch (Throwable throwable) {}
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/*  39 */     Vars.world = new World()
/*     */       {
/*     */         public boolean isGenerating() {
/*  42 */           return true;
/*     */         }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */         
/*     */         public void notifyChanged(Tile tile) {}
/*     */       };
/*  51 */     Vars.state = new GameState();
/*     */   }
/*     */   
/*     */   public final void read(DataInputStream stream, CounterInputStream counter, Context context) throws IOException {
/*  55 */     region("meta", stream, counter, in -> readMeta(in, context));
/*  56 */     region("content", stream, counter, this::readContentHeader);
/*     */     
/*     */     try {
/*  59 */       region("map", stream, counter, in -> readMap(in, context));
/*     */     } finally {
/*     */       
/*  62 */       Vars.content.setTemporaryMapper((MappableContent[][])null);
/*     */     } 
/*     */   }
/*     */   
/*     */   protected void region(String name, DataInput stream, CounterInputStream counter, SaveFileReader.IORunner<DataInput> cons) throws IOException {
/*  67 */     counter.resetCount();
/*     */     try {
/*  69 */       readChunk(stream, cons);
/*     */     }
/*  71 */     catch (Throwable e) {
/*  72 */       IOException exp = new IOException("Error reading region \"" + name + "\".", e);
/*  73 */       exp.initCause(e);
/*  74 */       throw exp;
/*     */     } 
/*     */   }
/*     */   
/*     */   public void readMap(DataInput stream, Context context) throws IOException {
/*  79 */     int width = stream.readUnsignedShort();
/*  80 */     int height = stream.readUnsignedShort();
/*     */     
/*  82 */     boolean generating = context.isGenerating();
/*     */     
/*  84 */     if (!generating) context.begin(); 
/*     */     try {
/*  86 */       context.resize(width, height);
/*     */       
/*     */       int i;
/*  89 */       for (i = 0; i < width * height; i++) {
/*  90 */         int x = i % width, y = i / width;
/*  91 */         short floorid = stream.readShort();
/*  92 */         short oreid = stream.readShort();
/*  93 */         int consecutives = stream.readUnsignedByte();
/*  94 */         if (Vars.content.block(floorid) == Blocks.air) floorid = Blocks.stone.id;
/*     */         
/*  96 */         context.create(x, y, floorid, oreid, 0);
/*     */         
/*  98 */         for (int j = i + 1; j < i + 1 + consecutives; j++) {
/*  99 */           int newx = j % width, newy = j / width;
/* 100 */           context.create(newx, newy, floorid, oreid, 0);
/*     */         } 
/*     */         
/* 103 */         i += consecutives;
/*     */       } 
/*     */ 
/*     */       
/* 107 */       for (i = 0; i < width * height; i++) {
/* 108 */         int x = i % width, y = i / width;
/* 109 */         Block block = Vars.content.block(stream.readShort());
/* 110 */         Tile tile = context.tile(x, y);
/* 111 */         if (block == null) block = Blocks.air; 
/* 112 */         tile.setBlock(block);
/*     */         
/* 114 */         if (tile.entity != null) {
/*     */           try {
/* 116 */             readChunk(stream, true, in -> {
/*     */                   byte version = in.readByte();
/*     */                   tile.entity.read(in, version);
/*     */                 });
/* 120 */           } catch (Exception e) {
/* 121 */             throw new IOException("Failed to read tile entity of block: " + block, e);
/*     */           } 
/*     */         } else {
/* 124 */           int consecutives = stream.readUnsignedByte();
/*     */           
/* 126 */           for (int j = i + 1; j < i + 1 + consecutives; j++) {
/* 127 */             int newx = j % width, newy = j / width;
/* 128 */             context.tile(newx, newy).setBlock(block);
/*     */           } 
/* 130 */           i += consecutives;
/*     */         } 
/*     */       } 
/*     */     } finally {
/* 134 */       if (!generating) context.end(); 
/*     */     } 
/*     */   }
/*     */   
/*     */   public void readMeta(DataInput stream, Context context) throws IOException {
/* 139 */     StringMap map = readStringMap(stream);
/*     */     
/* 141 */     context.state.wave = map.getInt("wave");
/* 142 */     context.state.wavetime = map.getFloat("wavetime", context.state.rules.waveSpacing);
/*     */     
/*     */     try {
/* 145 */       context.stats = (Stats)JsonIO.read(Stats.class, (String)map.get("stats", "{}"));
/* 146 */     } catch (Exception exception) {}
/*     */     
/*     */     try {
/* 149 */       context.state.rules = (Rules)JsonIO.read(Rules.class, (String)map.get("rules", "{}"));
/* 150 */     } catch (Exception e) {
/* 151 */       e.printStackTrace();
/* 152 */       System.exit(1);
/*     */     } 
/*     */     
/*     */     try {
/* 156 */       context.mods = (Array<String>)JsonIO.read(Array.class, (String)map.get("mods", "[]"));
/* 157 */     } catch (Exception e) {
/* 158 */       e.printStackTrace();
/* 159 */       System.exit(1);
/*     */     } 
/*     */     
/* 162 */     if (context.state.rules.spawns.isEmpty()) context.state.rules.spawns = this.defaultWaves.get(); 
/* 163 */     this.lastReadBuild = map.getInt("build", -1);
/*     */     
/* 165 */     context.name = (String)map.get("name", "Unknown");
/* 166 */     context.author = (String)map.get("author", "Unknown");
/* 167 */     context.description = (String)map.get("description", "None");
/*     */     
/* 169 */     context.build = this.lastReadBuild;
/*     */   }
/*     */   
/*     */   public void readContentHeader(DataInput stream) throws IOException {
/* 173 */     byte mapped = stream.readByte();
/*     */     
/* 175 */     MappableContent[][] map = new MappableContent[(ContentType.values()).length][0];
/*     */     
/* 177 */     for (int i = 0; i < mapped; i++) {
/* 178 */       ContentType type = ContentType.values()[stream.readByte()];
/* 179 */       short total = stream.readShort();
/* 180 */       map[type.ordinal()] = new MappableContent[total];
/*     */       
/* 182 */       for (int j = 0; j < total; j++) {
/* 183 */         String name = stream.readUTF();
/* 184 */         map[type.ordinal()][j] = Vars.content.getByName(type, (String)this.fallback.get(name, name));
/*     */       } 
/*     */     } 
/*     */     
/* 188 */     Vars.content.setTemporaryMapper(map);
/*     */   }
/*     */ }


/* Location:              E:\projects\handler.jar!\maphandler\Saves\SaveVersion.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */