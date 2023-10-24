package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilitiesCommon;
import serverutils.lib.data.Action;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.gui.misc.GuiActionList;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageMyTeamGuiResponse extends MessageToClient {

    private IChatComponent title;
    private Collection<Action.Inst> actions;

    public MessageMyTeamGuiResponse() {}

    public MessageMyTeamGuiResponse(ForgePlayer player) {
        title = player.team.getTitle();
        actions = new ArrayList<>();
        NBTTagCompound emptyData = new NBTTagCompound();

        for (Action action : ServerUtilitiesCommon.TEAM_GUI_ACTIONS.values()) {
            Action.Type type = action.getType(player, emptyData);

            if (type.isVisible()) {
                actions.add(new Action.Inst(action, type));
            }
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.MY_TEAM;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeTextComponent(title);
        data.writeCollection(actions, Action.Inst.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        title = data.readTextComponent();
        actions = data.readCollection(Action.Inst.DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiActionList(
                title.getFormattedText(),
                actions,
                id -> new MessageMyTeamAction(id, new NBTTagCompound()).sendToServer()).openGui();
    }
}
