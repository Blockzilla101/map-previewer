/*    */ package maphandler;
/*    */ 
/*    */ import java.io.File;
/*    */ import java.util.concurrent.TimeUnit;
/*    */ import javax.imageio.ImageIO;
/*    */ 
/*    */ public class MapHandler {
/*    */   public static void main(String[] args) {
/*  9 */     if (args.length == 0) {
/* 10 */       System.out.println("A map file needs to be specified");
/* 11 */       System.exit(1);
/*    */     } 
/*    */     
/*    */     try {
/* 15 */       String path = args[0];
/*    */       
/* 17 */       System.out.println("Reading save " + path);
/* 18 */       long start = System.currentTimeMillis();
/*    */       
/* 20 */       Map map = new Map(path);
/* 21 */       ImageIO.write(map.image, "png", new File(path.replaceAll("\\.msav$", ".png")));
/*    */       
/* 23 */       long end = System.currentTimeMillis();
/*    */       
/* 25 */       System.out.println(map.toString(">"));
/* 26 */       for (int i = 0; i < map.stuffs.size; i++) {
/* 27 */         String key = (String)map.stuffs.keys().toArray().get(i);
/* 28 */         String val = (String)map.stuffs.values().toArray().get(i);
/*    */         
/* 30 */         System.out.println(">" + key + "=" + val);
/*    */       } 
/*    */       
/* 33 */       System.out.println();
/* 34 */       long elapsed = end - start;
/* 35 */       System.out.printf("Took %d.%ds%n", new Object[] {
/* 36 */             Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(elapsed)), 
/* 37 */             Long.valueOf(elapsed - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(elapsed)))
/*    */           });
/*    */     }
/* 40 */     catch (Exception exception) {
/* 41 */       exception.printStackTrace();
/* 42 */       System.exit(1);
/*    */     } 
/*    */   }
/*    */ }


/* Location:              E:\projects\handler.jar!\maphandler\MapHandler.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */