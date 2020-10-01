/*    */ package maphandler.Saves;
/*    */ 
/*    */ import arc.struct.Array;
/*    */ import mindustry.core.GameState;
/*    */ import mindustry.game.Stats;
/*    */ import mindustry.world.Tile;
/*    */ import mindustry.world.WorldContext;
/*    */ 
/*    */ public class Context implements WorldContext {
/* 10 */   public Tile[][] tiles = new Tile[0][0];
/* 11 */   public int width = 0;
/* 12 */   public int height = 0;
/*    */   
/* 14 */   public int build = -1;
/*    */   
/*    */   public String name;
/*    */   
/*    */   public String description;
/*    */   public String author;
/* 20 */   public GameState state = new GameState();
/* 21 */   public Stats stats = new Stats();
/*    */   
/* 23 */   public Array<String> mods = new Array();
/*    */   
/*    */   private boolean isGenerating = false;
/*    */ 
/*    */   
/*    */   public Tile tile(int x, int y) throws IllegalArgumentException {
/* 29 */     return this.tiles[x][y];
/*    */   }
/*    */ 
/*    */   
/*    */   public void resize(int width, int height) {
/* 34 */     this.width = width;
/* 35 */     this.height = height;
/*    */     
/* 37 */     this.tiles = new Tile[width][height];
/*    */   }
/*    */ 
/*    */   
/*    */   public Tile create(int x, int y, int floorID, int overlayID, int wallID) {
/* 42 */     this.tiles[x][y] = new Tile(x, y, floorID, overlayID, wallID); return new Tile(x, y, floorID, overlayID, wallID);
/*    */   }
/*    */ 
/*    */   
/*    */   public boolean isGenerating() {
/* 47 */     return this.isGenerating;
/*    */   }
/*    */ 
/*    */   
/*    */   public void begin() {
/* 52 */     this.isGenerating = true;
/*    */   }
/*    */ 
/*    */   
/*    */   public void end() {
/* 57 */     this.isGenerating = false;
/*    */   }
/*    */ }


/* Location:              E:\projects\handler.jar!\maphandler\Saves\Context.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */