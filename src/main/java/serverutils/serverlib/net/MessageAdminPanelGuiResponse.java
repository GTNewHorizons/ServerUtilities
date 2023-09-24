package serverutils.serverlib.net;

import java.util.Collection;

import net.minecraft.client.resources.I18n;

import serverutils.serverlib.lib.data.Action;
import serverutils.serverlib.lib.gui.misc.GuiActionList;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.net.NetworkWrapper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageAdminPanelGuiResponse extends MessageToClient {

	private Collection<Action.Inst> actions;

	public MessageAdminPanelGuiResponse() {}

	public MessageAdminPanelGuiResponse(Collection<Action.Inst> a) {
		actions = a;
	}

	@Override
	public NetworkWrapper getWrapper() {
		return ServerLibNetHandler.GENERAL;
	}

	@Override
	public void writeData(DataOut data) {
		data.writeCollection(actions, Action.Inst.SERIALIZER);
	}

	@Override
	public void readData(DataIn data) {
		actions = data.readCollection(Action.Inst.DESERIALIZER);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage() {
		new GuiActionList(
				I18n.format("sidebar_button.serverlib.admin_panel"),
				actions,
				id -> new MessageAdminPanelAction(id).sendToServer()).openGui();
	}
}
