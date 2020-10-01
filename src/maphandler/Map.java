/*     */ package maphandler;
/*     */ import arc.files.Fi;
/*     */ import arc.graphics.Color;
/*     */ import arc.graphics.Pixmap;
/*     */ import arc.graphics.Texture;
/*     */ import arc.graphics.TextureData;
/*     */ import arc.graphics.g2d.TextureAtlas;
/*     */ import arc.struct.Array;
/*     */ import arc.struct.IntMap;
/*     */ import arc.struct.StringMap;
/*     */ import arc.util.Tmp;
/*     */ import arc.util.io.CounterInputStream;
/*     */ import java.awt.Graphics2D;
/*     */ import java.awt.image.BufferedImage;
/*     */ import java.io.DataInput;
/*     */ import java.io.DataInputStream;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.util.Arrays;
/*     */ import java.util.zip.InflaterInputStream;
/*     */ import javax.imageio.ImageIO;
/*     */ import maphandler.Saves.Context;
/*     */ import maphandler.Saves.Save1;
/*     */ import maphandler.Saves.Save2;
/*     */ import maphandler.Saves.Save3;
/*     */ import maphandler.Saves.SaveVersion;
/*     */ import mindustry.Vars;
/*     */ import mindustry.content.Blocks;
/*     */ import mindustry.core.GameState;
/*     */ import mindustry.game.Stats;
/*     */ import mindustry.io.MapIO;
/*     */ import mindustry.io.SaveIO;
/*     */ import mindustry.maps.Maps;
/*     */ import mindustry.world.Block;
/*     */ import mindustry.world.Tile;
/*     */ import mindustry.world.blocks.OreBlock;
/*     */ 
/*     */ public class Map {
/*  40 */   public static final byte[] mapHeader = new byte[] { 77, 83, 65, 86 };
/*  41 */   public static final IntMap<SaveVersion> versions = new IntMap();
/*  42 */   public static final Array<SaveVersion> versionArray = Array.with((Object[])new SaveVersion[] { (SaveVersion)new Save1(), (SaveVersion)new Save2(), (SaveVersion)new Save3() });
/*     */   
/*     */   public String name;
/*     */   public String author;
/*     */   public String description;
/*     */   public int version;
/*     */   public int build;
/*     */   public BufferedImage image;
/*     */   public Tile[][] tiles;
/*  51 */   public StringMap stuffs = new StringMap();
/*     */   
/*     */   public int width;
/*     */   
/*     */   public int height;
/*     */   
/*     */   public GameState state;
/*     */   
/*     */   public Stats stats;
/*     */   public Array<String> mods;
/*     */   Graphics2D currentGraphics;
/*     */   BufferedImage currentImage;
/*     */   
/*     */   static {
/*  65 */     for (SaveVersion version : versionArray) {
/*  66 */       versions.put(version.version, version);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   Map(String path) throws IOException {
/*  73 */     Map map = parseMap(path);
/*     */     
/*  75 */     this.name = map.name;
/*  76 */     this.author = map.author;
/*  77 */     this.description = map.description;
/*     */     
/*  79 */     this.stuffs = map.stuffs;
/*     */     
/*  81 */     this.image = map.image;
/*  82 */     this.tiles = map.tiles;
/*  83 */     this.width = map.width;
/*  84 */     this.height = map.height;
/*     */     
/*  86 */     this.state = map.state;
/*  87 */     this.stats = map.stats;
/*  88 */     this.mods = map.mods;
/*     */     
/*  90 */     this.version = map.version;
/*     */   }
/*     */   
/*     */   public static Map parseMap(String path) throws IOException {
/*  94 */     Map out = new Map();
/*  95 */     Fi file = Fi.get(path);
/*     */     
/*  97 */     Vars.maps = new Maps();
/*     */     
/*  99 */     if (!file.exists()) throw new IOException(path + " not found"); 
/* 100 */     if (!SaveIO.isSaveValid(file)) throw new IOException(path + " invalid save file");
/*     */     
/* 102 */     try(InputStream in = file.read(); InflaterInputStream inf = new InflaterInputStream(in); CounterInputStream counter = new CounterInputStream(in); DataInputStream stream = new DataInputStream(inf)) {
/* 103 */       readHeader(stream);
/* 104 */       int version = stream.readInt();
/* 105 */       Context context = new Context();
/*     */       
/* 107 */       SaveVersion ver = (SaveVersion)versions.get(version);
/* 108 */       ver.read(stream, counter, context);
/*     */       
/* 110 */       out.tiles = context.tiles;
/* 111 */       out.width = context.width;
/* 112 */       out.height = context.height;
/*     */       
/* 114 */       out.name = context.name;
/* 115 */       out.description = context.description;
/* 116 */       out.author = context.author;
/*     */       
/* 118 */       out.build = context.build;
/* 119 */       out.state = context.state;
/* 120 */       out.stats = context.stats;
/* 121 */       out.mods = context.mods;
/*     */       
/* 123 */       out.version = version;
/*     */       
/* 125 */       out.doRest();
/*     */     } 
/* 127 */     return out;
/*     */   }
/*     */   
/*     */   void doRest() {
/* 131 */     String assets = "mindustry_assets/";
/*     */     try {
/* 133 */       BufferedImage image = ImageIO.read(new File(assets + "sprites/block_colors.png"));
/*     */       
/* 135 */       for (Block block : Vars.content.blocks()) {
/* 136 */         block.color.argb8888(image.getRGB(block.id, 0));
/* 137 */         if (block instanceof OreBlock) {
/* 138 */           block.color.set(((OreBlock)block).itemDrop.color);
/*     */         }
/*     */       } 
/* 141 */     } catch (Exception e) {
/* 142 */       throw new RuntimeException(e);
/*     */     } 
/*     */     
/* 145 */     StringMap stuffs = new StringMap();
/* 146 */     BufferedImage img = new BufferedImage(this.tiles.length, (this.tiles[0]).length, 2);
/*     */     
/* 148 */     stuffs.put("playableTeams", this.state.rules.defaultTeam.toString());
/*     */     
/* 150 */     for (int i = 0; i < this.width * this.height; i++) {
/* 151 */       int x = i % this.width, y = i / this.width;
/* 152 */       Tile tile = this.tiles[x][y];
/* 153 */       img.setRGB(x, img.getHeight() - 1 - y, Tmp.c1.set(MapIO.colorFor((Block)tile.floor(), tile.block(), (Block)tile.overlay(), tile.getTeam())).argb8888());
/*     */ 
/*     */       
/* 156 */       if (tile.block() == Blocks.coreShard || tile.block() == Blocks.coreFoundation || tile.block() == Blocks.coreNucleus) {
/* 157 */         stuffs.put("cores", getPrettyValue((String)stuffs.get("cores", ""), tile.getTeam().toString()));
/*     */ 
/*     */       
/*     */       }
/* 161 */       else if (tile.block() == Blocks.repairPoint) {
/* 162 */         stuffs.put("playableTeams", getPrettyValue((String)stuffs.get("playableTeams", ""), tile.getTeam().toString()));
/*     */ 
/*     */       
/*     */       }
/* 166 */       else if (tile.block() == Blocks.itemSource || tile.block() == Blocks.liquidSource || tile.block() == Blocks.itemVoid || tile.block() == Blocks.liquidVoid) {
/* 167 */         stuffs.put("sandboxBlockTeams", getPrettyValue((String)stuffs.get("sandboxBlockTeams", ""), tile.getTeam().toString()));
/*     */ 
/*     */       
/*     */       }
/* 171 */       else if (tile.overlay() == Blocks.spawn) {
/* 172 */         stuffs.put("spawns", getPrettyValue((String)stuffs.get("spawns", ""), tile.getTeam().toString()));
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 177 */     stuffs.put("cores", stuffs.get("cores", "None"));
/* 178 */     stuffs.put("spawns", stuffs.get("spawns", "None"));
/* 179 */     stuffs.put("sandboxBlockTeams", stuffs.get("sandboxBlockTeams", "None"));
/*     */     
/* 181 */     stuffs.put("type", "unknown");
/*     */     
/* 183 */     String[] cores = ((String)stuffs.get("cores")).split(", ");
/* 184 */     String[] spawns = ((String)stuffs.get("spawns")).split(", ");
/* 185 */     String[] playableTeams = ((String)stuffs.get("playableTeams")).split(", ");
/* 186 */     String[] sandboxBlockTeams = ((String)stuffs.get("sandboxBlockTeams")).split(", ");
/*     */     
/* 188 */     if (overlaps(playableTeams, sandboxBlockTeams) || this.state.rules.infiniteResources) {
/* 189 */       stuffs.put("type", "sandbox");
/* 190 */     } else if (!cores[0].equals("None") && cores.length == playableTeams.length && spawns[0].equals("None")) {
/* 191 */       stuffs.put("type", "pvp");
/* 192 */     } else if (!cores[0].equals("None") && cores.length > 1 && playableTeams.length == 1) {
/* 193 */       stuffs.put("type", "attack");
/* 194 */     } else if (!cores[0].equals("None") && !spawns[0].equals("None")) {
/* 195 */       stuffs.put("type", "survival");
/*     */     } 
/*     */     
/* 198 */     this.image = img;
/* 199 */     this.stuffs = stuffs;
/*     */   }
/*     */   
/*     */   static BufferedImage tint(BufferedImage image, Color color) {
/* 203 */     BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
/* 204 */     Color tmp = new Color();
/* 205 */     for (int x = 0; x < copy.getWidth(); x++) {
/* 206 */       for (int y = 0; y < copy.getHeight(); y++) {
/* 207 */         int argb = image.getRGB(x, y);
/* 208 */         tmp.argb8888(argb);
/* 209 */         tmp.mul(color);
/* 210 */         copy.setRGB(x, y, tmp.argb8888());
/*     */       } 
/*     */     } 
/* 213 */     return copy;
/*     */   }
/*     */   
/*     */   static void readHeader(DataInput input) throws IOException {
/* 217 */     byte[] bytes = new byte[mapHeader.length];
/* 218 */     input.readFully(bytes);
/* 219 */     if (!Arrays.equals(bytes, mapHeader)) {
/* 220 */       throw new IOException("Incorrect header! Expecting: " + Arrays.toString(mapHeader) + "; Actual: " + Arrays.toString(bytes));
/*     */     }
/*     */   }
/*     */   
/*     */   public String toString(String start) {
/* 225 */     return start + "author=" + this.author + "\n" + start + "description=" + this.description + "\n" + start + "name=" + this.name + "\n" + start + "width=" + this.width + "\n" + start + "height=" + this.height + "\n" + start + "mods=" + 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 230 */       arrayToJson(this.mods);
/*     */   }
/*     */ 
/*     */   
/*     */   public String toString() {
/* 235 */     return toString("");
/*     */   }
/*     */   
/*     */   static String getPrettyValue(String currentValue, String toAdd) {
/* 239 */     if (currentValue.equals("")) return toAdd; 
/* 240 */     return currentValue + ", " + toAdd;
/*     */   }
/*     */   
/*     */   static boolean overlaps(String[] arr1, String[] arr2) {
/* 244 */     for (String s : arr1) {
/* 245 */       if (Arrays.<String>asList(arr2).contains(s)) return true; 
/*     */     } 
/* 247 */     return false;
/*     */   }
/*     */   
/*     */   static String arrayToJson(Array<String> arr) {
/* 251 */     String json = "";
/* 252 */     for (int i = 0; i < arr.size; i++) {
/* 253 */       json = json + "\"" + ((String)arr.get(i)).replace("\"", "\\\"") + "\"";
/* 254 */       if (i != arr.size - 1) json = json + ", "; 
/*     */     } 
/* 256 */     return "[" + json + "]";
/*     */   }
/*     */   
/*     */   Map() {}
/*     */   
/*     */   static class ImageData implements TextureData {
/*     */     public ImageData(BufferedImage image) {
/* 263 */       this.image = image;
/*     */     }
/*     */     final BufferedImage image;
/*     */     
/*     */     public TextureData.TextureDataType getType() {
/* 268 */       return TextureData.TextureDataType.Custom;
/*     */     }
/*     */ 
/*     */     
/*     */     public boolean isPrepared() {
/* 273 */       return false;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     public void prepare() {}
/*     */ 
/*     */ 
/*     */     
/*     */     public Pixmap consumePixmap() {
/* 283 */       return null;
/*     */     }
/*     */ 
/*     */     
/*     */     public boolean disposePixmap() {
/* 288 */       return false;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/*     */     public void consumeCustomData(int target) {}
/*     */ 
/*     */ 
/*     */     
/*     */     public int getWidth() {
/* 298 */       return this.image.getWidth();
/*     */     }
/*     */ 
/*     */     
/*     */     public int getHeight() {
/* 303 */       return this.image.getHeight();
/*     */     }
/*     */ 
/*     */     
/*     */     public Pixmap.Format getFormat() {
/* 308 */       return Pixmap.Format.RGBA8888;
/*     */     }
/*     */ 
/*     */     
/*     */     public boolean useMipMaps() {
/* 313 */       return false;
/*     */     }
/*     */ 
/*     */     
/*     */     public boolean isManaged() {
/* 318 */       return false;
/*     */     } }
/*     */   
/*     */   static class ImageRegion extends TextureAtlas.AtlasRegion {
/*     */     final BufferedImage image;
/*     */     final int x;
/*     */     final int y;
/*     */     
/*     */     public ImageRegion(String name, Texture texture, int x, int y, BufferedImage image) {
/* 327 */       super(texture, x, y, image.getWidth(), image.getHeight());
/* 328 */       this.name = name;
/* 329 */       this.image = image;
/* 330 */       this.x = x;
/* 331 */       this.y = y;
/*     */     }
/*     */   }
/*     */ }


/* Location:              E:\projects\handler.jar!\maphandler\Map.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */