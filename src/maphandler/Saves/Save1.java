/*    */ package maphandler.Saves;
/*    */ 
/*    */ import arc.func.Prov;
/*    */ import java.io.DataInput;
/*    */ import java.io.IOException;
/*    */ import mindustry.entities.traits.SaveTrait;
/*    */ import mindustry.io.versions.LegacyTypeTable;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class Save1
/*    */   extends Save2
/*    */ {
/*    */   public void readEntities(DataInput stream) throws IOException {
/* 17 */     Prov[] table = LegacyTypeTable.getTable(this.lastReadBuild);
/*    */     
/* 19 */     byte groups = stream.readByte();
/*    */     
/* 21 */     for (int i = 0; i < groups; i++) {
/* 22 */       int amount = stream.readInt();
/* 23 */       for (int j = 0; j < amount; j++) {
/* 24 */         readChunk(stream, true, in -> {
/*    */               byte typeid = in.readByte();
/*    */               byte version = in.readByte();
/*    */               SaveTrait trait = (SaveTrait)table[typeid].get();
/*    */               trait.readSave(in, version);
/*    */             });
/*    */       } 
/*    */     } 
/*    */   }
/*    */ }


/* Location:              E:\projects\handler.jar!\maphandler\Saves\Save1.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */