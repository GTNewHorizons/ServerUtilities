package serverutils.lib.net;

import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.config.IConfigCallback;
import serverutils.lib.lib.gui.misc.GuiEditConfig;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageEditConfig extends MessageToClient {

	private static final IConfigCallback RX_CONFIG_TREE = (group, sender) -> new MessageEditConfigResponse(group.serializeNBT()).sendToServer();

	private ConfigGroup group;

	public MessageEditConfig() {}

	public MessageEditConfig(ConfigGroup _group) {
		group = _group;
		// TODO: Logger
	}

	@Override
	public NetworkWrapper getWrapper() {
		return ServerLibNetHandler.EDIT_CONFIG;
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