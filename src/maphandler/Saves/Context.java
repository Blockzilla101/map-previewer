package maphandler.Saves;

import arc.struct.Array;
import mindustry.core.GameState;
import mindustry.game.Stats;
import mindustry.world.Tile;
import mindustry.world.WorldContext;

public class Context implements WorldContext {
    public Tile[][] tiles = new Tile[0][0];
    public int width = 0;
    public int height = 0;
  
    public int build = -1;
  
    public String name;

    public String description;
    public String author;
    public GameState state = new GameState();
    public Stats stats = new Stats();
  
    public Array<String> mods = new Array();
  
    private boolean isGenerating = false;

    public Tile tile(int x, int y) throws IllegalArgumentException {
        return this.tiles[x][y];
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    
        this.tiles = new Tile[width][height];
    }

    public Tile create(int x, int y, int floorID, int overlayID, int wallID) {
        this.tiles[x][y] = new Tile(x, y, floorID, overlayID, wallID); return new Tile(x, y, floorID, overlayID, wallID);
    }

    public boolean isGenerating() {
        return this.isGenerating;
    }

    public void begin() {
        this.isGenerating = true;
    }

    public void end() {
        this.isGenerating = false;
    }
}
