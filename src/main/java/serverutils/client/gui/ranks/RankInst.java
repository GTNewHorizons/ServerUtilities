package serverutils.client.gui.ranks;

import java.util.ArrayList;
import java.util.List;

import serverutils.lib.config.ConfigGroup;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.FinalIDObject;

public class RankInst extends FinalIDObject {

    public static final DataOut.Serializer<RankInst> SERIALIZER = (data, object) -> {
        data.writeString(object.toString());
        data.writeCollection(object.parents, DataOut.STRING);
        data.writeString(object.player);
        ConfigGroup.SERIALIZER.write(data, object.group);
    };

    public static final DataIn.Deserializer<RankInst> DESERIALIZER = data -> {
        RankInst inst = new RankInst(data.readString());
        inst.parents = new ArrayList<>(data.readCollection(DataIn.STRING));
        inst.player = data.readString();
        inst.group = ConfigGroup.DESERIALIZER.read(data);
        return inst;
    };

    public List<String> parents;
    public ConfigGroup group;
    public String player;

    public RankInst(String id) {
        super(id);
        parents = new ArrayList<>();
        group = ConfigGroup.newGroup("");
        player = "";
    }
}
