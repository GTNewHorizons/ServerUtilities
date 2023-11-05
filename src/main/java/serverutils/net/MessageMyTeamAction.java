package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import serverutils.ServerUtilitiesCommon;
import serverutils.lib.data.Action;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageMyTeamAction extends MessageToServer {

    private ResourceLocation action;
    private NBTTagCompound nbt;

    public MessageMyTeamAction() {}

    public MessageMyTeamAction(ResourceLocation id, NBTTagCompound data) {
        action = id;
        nbt = data;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.MY_TEAM;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeResourceLocation(action);
        data.writeNBT(nbt);
    }

    @Override
    public void readData(DataIn data) {
        action = data.readResourceLocation();
        nbt = data.readNBT();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        Action a = ServerUtilitiesCommon.TEAM_GUI_ACTIONS.get(action);

        if (a != null) {
            ForgePlayer p = Universe.get().getPlayer(player);

            if (p.hasTeam() && a.getType(p, nbt).isEnabled()) {
                a.onAction(p, nbt);
            }
        }
    }
}
