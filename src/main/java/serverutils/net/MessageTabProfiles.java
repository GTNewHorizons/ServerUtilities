package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.tab.TabSkinCache;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageTabProfiles extends MessageToClient {

    private Collection<Entry> entries = new ArrayList<>();

    public MessageTabProfiles() {}

    public MessageTabProfiles(Collection<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(entries, Entry.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        entries = data.readCollection(Entry.DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        for (Entry entry : entries) {
            TabSkinCache.INSTANCE.registerProfile(entry);
        }
    }

    public static class Entry {

        /** Name as sent in the vanilla tab list packet, may be a nickname on hybrid servers. */
        public final String listName;
        public final UUID id;
        public final String name;
        public final String textures;
        public final String signature;

        public Entry(String listName, UUID id, String name, @Nullable String textures, @Nullable String signature) {
            this.listName = listName;
            this.id = id;
            this.name = name;
            this.textures = textures == null ? "" : textures;
            this.signature = signature == null ? "" : signature;
        }

        public static final DataOut.Serializer<Entry> SERIALIZER = (data, e) -> {
            data.writeString(e.listName);
            data.writeUUID(e.id);
            data.writeString(e.name);
            data.writeString(e.textures);
            data.writeString(e.signature);
        };

        public static final DataIn.Deserializer<Entry> DESERIALIZER = data -> new Entry(
                data.readString(),
                data.readUUID(),
                data.readString(),
                data.readString(),
                data.readString());
    }
}
