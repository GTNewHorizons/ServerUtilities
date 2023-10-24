package serverutils.events;

import java.util.function.Consumer;

import serverutils.data.NodeEntry;
import serverutils.lib.util.permission.DefaultPermissionLevel;

public class CustomPermissionPrefixesRegistryEvent extends ServerUtilitiesEvent {

    private final Consumer<NodeEntry> callback;

    public CustomPermissionPrefixesRegistryEvent(Consumer<NodeEntry> c) {
        callback = c;
    }

    public void register(NodeEntry entry) {
        callback.accept(entry);
    }

    public void register(String node, DefaultPermissionLevel level, String desc) {
        callback.accept(new NodeEntry(node, level, desc));
    }
}
