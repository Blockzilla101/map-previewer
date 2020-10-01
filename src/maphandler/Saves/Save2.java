/*    */ package maphandler.Saves;
/*    */ 
/*    */ import java.io.DataInput;
/*    */ import java.io.IOException;
/*    */ import mindustry.Vars;
/*    */ import mindustry.ctype.ContentType;
/*    */ import mindustry.entities.traits.SaveTrait;
/*    */ import mindustry.type.TypeID;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class Save2
/*    */   extends SaveVersion
/*    */ {
/*    */   public Save2() {
/* 16 */     super(2);
/*    */   }
/*    */ 
/*    */   
/*    */   public void readEntities(DataInput stream) throws IOException {
/* 21 */     byte groups = stream.readByte();
/*    */     
/* 23 */     for (int i = 0; i < groups; i++) {
/* 24 */       int amount = stream.readInt();
/* 25 */       for (int j = 0; j < amount; j++) {
/*    */         
/* 27 */         readChunk(stream, true, in -> {
/*    */               byte typeid = in.readByte();
/*    */               byte version = in.readByte();
/*    */               SaveTrait trait = (SaveTrait)((TypeID)Vars.content.getByID(ContentType.typeid, typeid)).constructor.get();
/*    */               trait.readSave(in, version);
/*    */             });
/*    */       } 
/*    */     } 
/*    */   }
/*    */ }


/* Location:              E:\projects\handler.jar!\maphandler\Saves\Save2.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */