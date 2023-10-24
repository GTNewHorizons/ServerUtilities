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

public class MessageAdminPanelAction extends MessageToServer {

    private ResourceLocation action;

    public MessageAdminPanelAction() {}

    public MessageAdminPanelAction(ResourceLocation id) {
        action = id;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeResourceLocation(action);
    }

    @Override
    public void readData(DataIn data) {
        action = data.readResourceLocation();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        Action a = ServerUtilitiesCommon.ADMIN_PANEL_ACTIONS.get(action);

        if (a != null) {
            ForgePlayer p = Universe.get().getPlayer(player);
            NBTTagCompound data = new NBTTagCompound();

            if (a.getType(p, data).isEnabled()) {
                a.onAction(p, data);
            }
        }
    }
}
