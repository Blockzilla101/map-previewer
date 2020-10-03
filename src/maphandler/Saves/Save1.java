package maphandler.Saves;

import arc.func.Prov;
import java.io.DataInput;
import java.io.IOException;
import mindustry.entities.traits.SaveTrait;
import mindustry.io.versions.LegacyTypeTable;

public class Save1 extends Save2 {
    public void readEntities(DataInput stream) throws IOException {
        Prov[] table = LegacyTypeTable.getTable(this.lastReadBuild);
    
        byte groups = stream.readByte();
    
        for (int i = 0; i < groups; i++) {
            int amount = stream.readInt();
            for (int j = 0; j < amount; j++) {
                readChunk(stream, true, in -> {
                    byte typeid = in.readByte();
                    byte version = in.readByte();
                    SaveTrait trait = (SaveTrait)table[typeid].get();
                    trait.readSave(in, version);
                });
            }
        }
    }
}
