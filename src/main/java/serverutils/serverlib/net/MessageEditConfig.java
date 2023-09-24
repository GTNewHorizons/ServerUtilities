package serverutils.serverlib.net;

import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.config.IConfigCallback;
import serverutils.serverlib.lib.gui.misc.GuiEditConfig;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.net.NetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public class MessageEditConfig extends MessageToClient
{
	private static final IConfigCallback RX_CONFIG_TREE = (group, sender) -> new MessageEditConfigResponse(group.serializeNBT()).sendToServer();

	private ConfigGroup group;

	public MessageEditConfig()
	{
	}

	public MessageEditConfig(ConfigGroup _group)
	{
		group = _group;
		//TODO: Logger
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.EDIT_CONFIG;
	}

	@Override
	public void writeData(DataOut data)
	{
		ConfigGroup.SERIALIZER.write(data, group);
	}

	@Override
	public void readData(DataIn data)
	{
		group = ConfigGroup.DESERIALIZER.read(data);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
		new GuiEditConfig(group, RX_CONFIG_TREE).openGui();
	}
}