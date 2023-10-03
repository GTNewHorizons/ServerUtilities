package serverutils.lib.net;

import java.util.Collection;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.lib.data.Action;
import serverutils.lib.lib.gui.misc.GuiActionList;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;

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
                I18n.format("sidebar_button.serverutilities.admin_panel"),
                actions,
                id -> new MessageAdminPanelAction(id).sendToServer()).openGui();
    }
}
