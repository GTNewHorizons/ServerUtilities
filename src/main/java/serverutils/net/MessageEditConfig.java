package serverutils.net;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.gui.misc.GuiEditConfig;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageEditConfig extends MessageToClient {

    private static final IConfigCallback RX_CONFIG_TREE = (group,
            sender) -> new MessageEditConfigResponse(group.serializeNBT()).sendToServer();

    private ConfigGroup group;

    public MessageEditConfig() {}

    public MessageEditConfig(ConfigGroup _group) {
        group = _group;
        // TODO: Logger
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.EDIT_CONFIG;
    }

    @Override
    public void writeData(DataOut data) {
        ConfigGroup.SERIALIZER.write(data, group);
    }

    @Override
    public void readData(DataIn data) {
        group = ConfigGroup.DESERIALIZER.read(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiEditConfig(group, RX_CONFIG_TREE).openGui();
    }
}
