package serverutils.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.ServerUtilitiesCommon;
import serverutils.lib.data.Action;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageAdminPanelGui extends MessageToServer {

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        List<Action.Inst> actions = new ArrayList<>();
        ForgePlayer p = Universe.get().getPlayer(player);
        NBTTagCompound data = new NBTTagCompound();

        for (Action a : ServerUtilitiesCommon.ADMIN_PANEL_ACTIONS.values()) {
            Action.Type type = a.getType(p, data);

            if (type.isVisible()) {
                actions.add(new Action.Inst(a, type));
            }
        }

        new MessageAdminPanelGuiResponse(actions).sendTo(player);
    }
}
