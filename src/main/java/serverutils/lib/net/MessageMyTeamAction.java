package serverutils.lib.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.ServerUtilitiesLibCommon;
import serverutils.lib.lib.data.Action;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;

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
        return ServerLibNetHandler.MY_TEAM;
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
        Action a = ServerUtilitiesLibCommon.TEAM_GUI_ACTIONS.get(action);

        if (a != null) {
            ForgePlayer p = Universe.get().getPlayer(player);

            if (p.hasTeam() && a.getType(p, nbt).isEnabled()) {
                a.onAction(p, nbt);
            }
        }
    }
}
